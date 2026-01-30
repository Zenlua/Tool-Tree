from __future__ import annotations

import io
from collections import ChainMap
from collections.abc import Callable, MutableMapping
from contextlib import contextmanager
from enum import Enum
from functools import lru_cache
from itertools import chain
from operator import attrgetter
from textwrap import dedent
from types import FunctionType
from typing import TYPE_CHECKING, Any, BinaryIO

from dissect.cstruct.bitbuffer import BitBuffer
from dissect.cstruct.types.base import (
    BaseType,
    MetaType,
    _is_buffer_type,
    _is_readable_type,
)
from dissect.cstruct.types.enum import EnumMetaType
from dissect.cstruct.types.pointer import Pointer

if TYPE_CHECKING:
    from collections.abc import Iterator, Mapping
    from types import FunctionType

    from typing_extensions import Self


class Field:
    """Structure field."""

    def __init__(self, name: str | None, type_: type[BaseType], bits: int | None = None, offset: int | None = None):
        self.name = name  # The name of the field, or None if anonymous
        self._name = name or type_.__name__  # The name of the field, or the type name if anonymous
        self.type = type_
        self.bits = bits
        self.offset = offset
        self.alignment = type_.alignment or 1

    def __repr__(self) -> str:
        bits_str = f" : {self.bits}" if self.bits else ""
        return f"<Field {self.name} {self.type.__name__}{bits_str}>"


class StructureMetaType(MetaType):
    """Base metaclass for cstruct structure type classes."""

    # TODO: resolve field types in _update_fields, remove resolves elsewhere?

    fields: dict[str, Field]
    """Mapping of field names to :class:`Field` objects, including "folded" fields from anonymous structures."""
    lookup: dict[str, Field]
    """Mapping of "raw" field names to :class:`Field` objects. E.g. holds the anonymous struct and not its fields."""
    __fields__: list[Field]
    """List of :class:`Field` objects for this structure. This is the structures' Single Source Of Truth."""

    # Internal
    __align__: bool
    __anonymous__: bool
    __updating__ = False
    __compiled__ = False
    __static_sizes__: dict[str, int]  # Cache of static sizes by field name

    def __new__(metacls, name: str, bases: tuple[type, ...], classdict: dict[str, Any]) -> Self:  # type: ignore
        if (fields := classdict.pop("fields", None)) is not None:
            metacls._update_fields(metacls, fields, align=classdict.get("__align__", False), classdict=classdict)

        return super().__new__(metacls, name, bases, classdict)

    def __call__(cls, *args, **kwargs) -> Self:  # type: ignore
        if (
            cls.__fields__
            and len(args) == len(cls.__fields__) == 1
            and isinstance(args[0], bytes)
            and issubclass(cls.__fields__[0].type, bytes)
            and len(args[0]) == cls.__fields__[0].type.size
        ):
            # Shortcut for single char/bytes type
            return type.__call__(cls, *args, **kwargs)
        if not args and not kwargs:
            obj = type.__call__(cls)
            object.__setattr__(obj, "__dynamic_sizes__", {})
            return obj

        return super().__call__(*args, **kwargs)

    def _update_fields(
        cls, fields: list[Field], align: bool = False, classdict: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        classdict = classdict or {}

        lookup = {}
        raw_lookup = {}
        field_names = []
        static_sizes = {}
        for field in fields:
            if field._name in lookup and field._name != "_":
                raise ValueError(f"Duplicate field name: {field._name}")

            if not field.type.dynamic:
                static_sizes[field._name] = field.type.size

            if isinstance(field.type, StructureMetaType) and field.name is None:
                for anon_field in field.type.fields.values():
                    attr = f"{field._name}.{anon_field.name}"
                    classdict[anon_field.name] = property(attrgetter(attr), attrsetter(attr))

                lookup.update(field.type.fields)
            else:
                lookup[field._name] = field

            raw_lookup[field._name] = field

        field_names = lookup.keys()
        classdict["fields"] = lookup
        classdict["lookup"] = raw_lookup
        classdict["__fields__"] = fields
        classdict["__static_sizes__"] = static_sizes
        classdict["__bool__"] = _generate__bool__(field_names)

        if issubclass(cls, UnionMetaType) or isinstance(cls, UnionMetaType):
            classdict["__init__"] = _generate_union__init__(raw_lookup.values())
            # Not a great way to do this but it works for now
            classdict["__eq__"] = Union.__eq__
        else:
            classdict["__init__"] = _generate_structure__init__(raw_lookup.values())
            classdict["__eq__"] = _generate__eq__(field_names)

        classdict["__hash__"] = _generate__hash__(field_names)

        # If we're calling this as a class method or a function on the metaclass
        if issubclass(cls, type):
            size, alignment = cls._calculate_size_and_offsets(cls, fields, align)
        else:
            size, alignment = cls._calculate_size_and_offsets(fields, align)

        if cls.__compiled__:
            # If the previous class was compiled try to compile this too
            from dissect.cstruct import compiler  # noqa: PLC0415

            try:
                classdict["_read"] = compiler.Compiler(cls.cs).compile_read(fields, cls.__name__, align=cls.__align__)
                classdict["__compiled__"] = True
            except Exception:
                # Revert _read to the slower loop based method
                classdict["_read"] = classmethod(Structure._read.__func__)
                classdict["__compiled__"] = False

        # TODO: compile _write
        # TODO: generate cached_property for lazy reading

        classdict["size"] = size
        classdict["alignment"] = alignment
        classdict["dynamic"] = size is None

        return classdict

    def _calculate_size_and_offsets(cls, fields: list[Field], align: bool = False) -> tuple[int | None, int]:
        """Iterate all fields in this structure to calculate the field offsets and total structure size.

        If a structure has a dynamic field, further field offsets will be set to None and self.dynamic
        will be set to True.
        """
        # The current offset, set to None if we become dynamic
        offset = 0
        # The current alignment for this structure
        alignment = 0

        # The current bit field type
        bits_type = None
        # The offset of the current bit field, set to None if we become dynamic
        bits_field_offset = 0
        # How many bits we have left in the current bit field
        bits_remaining = 0

        for field in fields:
            if field.offset is not None:
                # If a field already has an offset, it's leading
                offset = field.offset

            if align and offset is not None:
                # Round to next alignment
                offset += -offset & (field.alignment - 1)

            # The alignment of this struct is equal to its largest members' alignment
            alignment = max(alignment, field.alignment)

            if field.bits:
                field_type = field.type

                if isinstance(field_type, EnumMetaType):
                    field_type = field_type.type

                # Bit fields have special logic
                if (
                    # Exhausted a bit field
                    bits_remaining == 0
                    # Moved to a bit field of another type, e.g. uint16 f1 : 8, uint32 f2 : 8;
                    or field_type != bits_type
                    # Still processing a bit field, but it's at a different offset due to alignment or a manual offset
                    or (bits_type is not None and offset > bits_field_offset + bits_type.size)
                ):
                    # ... if any of this is true, we have to move to the next field
                    bits_type = field_type
                    bits_count = bits_type.size * 8
                    bits_remaining = bits_count
                    bits_field_offset = offset

                    if offset is not None:
                        # We're not dynamic, update the structure size and current offset
                        offset += bits_type.size

                    field.offset = bits_field_offset

                bits_remaining -= field.bits

                if bits_remaining < 0:
                    raise ValueError("Straddled bit fields are unsupported")
            else:
                # Reset bits stuff
                bits_type = None
                bits_field_offset = bits_remaining = 0

                field.offset = offset

                if offset is not None:
                    # We're not dynamic, update the structure size and current offset
                    try:
                        field_len = len(field.type)
                    except TypeError:
                        # This field is dynamic
                        offset = None
                        continue

                    offset += field_len

        if align and offset is not None:
            # Add "tail padding" if we need to align
            # This bit magic rounds up to the next alignment boundary
            # E.g. offset = 3; alignment = 8; -offset & (alignment - 1) = 5
            offset += -offset & (alignment - 1)

        # The structure size is whatever the currently calculated offset is
        return offset, alignment

    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:  # type: ignore
        bit_buffer = BitBuffer(stream, cls.cs.endian)
        struct_start = stream.tell()

        result = {}
        sizes = {}
        for field in cls.__fields__:
            offset = stream.tell()

            if field.offset is not None and offset != struct_start + field.offset:
                # Field is at a specific offset, either alligned or added that way
                offset = struct_start + field.offset
                stream.seek(offset)

            if cls.__align__ and field.offset is None:
                # Previous field was dynamically sized and we need to align
                offset += -offset & (field.alignment - 1)
                stream.seek(offset)

            if field.bits:
                if isinstance(field.type, EnumMetaType):
                    value = field.type(bit_buffer.read(field.type.type, field.bits))
                else:
                    value = bit_buffer.read(field.type, field.bits)

                result[field._name] = value
                continue

            bit_buffer.reset()

            value = field.type._read(stream, result)

            result[field._name] = value
            if field.type.dynamic:
                sizes[field._name] = stream.tell() - offset

        if cls.__align__:
            # Align the stream
            stream.seek(-stream.tell() & (cls.alignment - 1), io.SEEK_CUR)

        # Using type.__call__ directly calls the __init__ method of the class
        # This is faster than calling cls() and bypasses the metaclass __call__ method
        obj = type.__call__(cls, **result)
        obj.__dynamic_sizes__ = sizes
        return obj

    def _read_0(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> list[Self]:  # type: ignore
        result = []

        while obj := cls._read(stream, context):
            result.append(obj)

        return result

    def _write(cls, stream: BinaryIO, data: Structure) -> int:
        bit_buffer = BitBuffer(stream, cls.cs.endian)
        struct_start = stream.tell()
        num = 0

        for field in cls.__fields__:
            field_type = cls.cs.resolve(field.type)

            bit_field_type = (
                (field_type.type if isinstance(field_type, EnumMetaType) else field_type) if field.bits else None
            )
            # Current field is not a bit field, but previous was
            # Or, moved to a bit field of another type, e.g. uint16 f1 : 8, uint32 f2 : 8;
            if (not field.bits and bit_buffer._type is not None) or (
                bit_buffer._type and bit_buffer._type != bit_field_type
            ):
                # Flush the current bit buffer so we can process alignment properly
                bit_buffer.flush()

            offset = stream.tell()

            if field.offset is not None and offset < struct_start + field.offset:
                # Field is at a specific offset, either alligned or added that way
                stream.write(b"\x00" * (struct_start + field.offset - offset))
                offset = struct_start + field.offset

            if cls.__align__ and field.offset is None:
                is_bitbuffer_boundary = bit_buffer._type and (
                    bit_buffer._remaining == 0 or bit_buffer._type != field_type
                )
                if not bit_buffer._type or is_bitbuffer_boundary:
                    # Previous field was dynamically sized and we need to align
                    align_pad = -offset & (field.alignment - 1)
                    stream.write(b"\x00" * align_pad)
                    offset += align_pad

            value = getattr(data, field._name, None)
            if value is None:
                value = field_type.__default__()

            if field.bits:
                if isinstance(field_type, EnumMetaType):
                    bit_buffer.write(field_type.type, value.value, field.bits)
                else:
                    bit_buffer.write(field_type, value, field.bits)
            else:
                field_type._write(stream, value)
                num += stream.tell() - offset

        if bit_buffer._type is not None:
            bit_buffer.flush()

        if cls.__align__:
            # Align the stream
            stream.write(b"\x00" * (-stream.tell() & (cls.alignment - 1)))

        return num

    def add_field(cls, name: str, type_: type[BaseType], bits: int | None = None, offset: int | None = None) -> None:
        field = Field(name, type_, bits=bits, offset=offset)
        cls.__fields__.append(field)

        if not cls.__updating__:
            cls.commit()

    @contextmanager
    def start_update(cls) -> Iterator[None]:
        try:
            cls.__updating__ = True
            yield
        finally:
            cls.commit()
            cls.__updating__ = False

    def commit(cls) -> None:
        classdict = cls._update_fields(cls.__fields__, cls.__align__)

        for key, value in classdict.items():
            setattr(cls, key, value)


class Structure(BaseType, metaclass=StructureMetaType):
    """Base class for cstruct structure type classes.

    Note that setting attributes which do not correspond to a field in the structure results in undefined behavior.
    For performance reasons, the structure does not check if the field exists when writing to an attribute.
    """

    __dynamic_sizes__: dict[str, int]

    def __len__(self) -> int:
        return len(self.dumps())

    def __bytes__(self) -> bytes:
        return self.dumps()

    def __getitem__(self, item: str) -> Any:
        return getattr(self, item)

    def __repr__(self) -> str:
        values = []
        for name, field in self.__class__.fields.items():
            value = self[name]
            if issubclass(field.type, int) and not issubclass(field.type, (Pointer, Enum)):
                value = hex(value)
            else:
                value = repr(value)
            values.append(f"{name}={value}")

        return f"<{self.__class__.__name__} {' '.join(values)}>"

    @property
    def __values__(self) -> MutableMapping[str, Any]:
        return StructureValuesProxy(self)

    @property
    def __sizes__(self) -> Mapping[str, int | None]:
        return ChainMap(self.__class__.__static_sizes__, self.__dynamic_sizes__)


class StructureValuesProxy(MutableMapping):
    """A proxy for the values of fields of a Structure."""

    def __init__(self, struct: Structure):
        self._struct: Structure = struct

    def __getitem__(self, key: str) -> Any:
        if key in self:
            return getattr(self._struct, key)
        raise KeyError(key)

    def __setitem__(self, key: str, value: Any) -> None:
        if key in self:
            return setattr(self._struct, key, value)
        raise KeyError(key)

    def __contains__(self, key: str) -> bool:
        return key in self._struct.__class__.fields

    def __iter__(self) -> Iterator[str]:
        return iter(self._struct.__class__.fields)

    def __len__(self) -> int:
        return len(self._struct.__class__.fields)

    def __repr__(self) -> str:
        return repr(dict(self))

    def __delitem__(self, _: str):
        # Is abstract in base, but deleting is not supported.
        raise NotImplementedError("Cannot delete fields from a Structure")


class UnionMetaType(StructureMetaType):
    """Base metaclass for cstruct union type classes."""

    def __call__(cls, *args, **kwargs) -> Self:  # type: ignore
        obj: Union = super().__call__(*args, **kwargs)

        # Calling with non-stream args or kwargs means we are initializing with values
        if (args and not (len(args) == 1 and (_is_readable_type(args[0]) or _is_buffer_type(args[0])))) or kwargs:
            # We don't support user initialization of dynamic unions yet
            if cls.dynamic:
                raise NotImplementedError("Initializing a dynamic union is not yet supported")

            # User (partial) initialization, rebuild the union
            # First user-provided field is the one used to rebuild the union
            arg_fields = (field._name for _, field in zip(args, cls.__fields__, strict=False))
            kwarg_fields = (name for name in kwargs if name in cls.lookup)
            if (first_field := next(chain(arg_fields, kwarg_fields), None)) is not None:
                obj._rebuild(first_field)
        elif not args and not kwargs:
            # Initialized with default values
            # Note that we proxify here in case we have a default initialization (cls())
            # We don't proxify in case we read from a stream, as we do that later on in _read at a more appropriate time
            # Same with (partial) user initialization, we do that after rebuilding the union
            obj._proxify()

        return obj

    def _calculate_size_and_offsets(cls, fields: list[Field], align: bool = False) -> tuple[int | None, int]:
        size = 0
        alignment = 0

        for field in fields:
            if size is not None:
                try:
                    size = max(len(field.type), size)
                except TypeError:
                    size = None

            alignment = max(field.alignment, alignment)

        if align and size is not None:
            # Add "tail padding" if we need to align
            # This bit magic rounds up to the next alignment boundary
            # E.g. offset = 3; alignment = 8; -offset & (alignment - 1) = 5
            size += -size & (alignment - 1)

        return size, alignment

    def _read_fields(
        cls, stream: BinaryIO, context: dict[str, Any] | None = None
    ) -> tuple[dict[str, Any], dict[str, int]]:
        result = {}
        sizes = {}

        if cls.size is None:
            offset = stream.tell()
            buf = stream
        else:
            offset = 0
            buf = io.BytesIO(stream.read(cls.size))

        for field in cls.__fields__:
            field_type = cls.cs.resolve(field.type)

            start = 0
            if field.offset is not None:
                start = field.offset

            buf.seek(offset + start)
            value = field_type._read(buf, result)

            result[field._name] = value
            if field.type.dynamic:
                sizes[field._name] = buf.tell() - start

        return result, sizes

    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:  # type: ignore
        if cls.size is None:
            start = stream.tell()
            result, sizes = cls._read_fields(stream, context)
            size = stream.tell() - start
            stream.seek(start)
            buf = stream.read(size)
        else:
            result = {}
            sizes = {}
            buf = stream.read(cls.size)

        # Create the object and set the values
        # Using type.__call__ directly calls the __init__ method of the class
        # This is faster than calling cls() and bypasses the metaclass __call__ method
        # It also makes it easier to differentiate between user-initialization of the class
        # and initialization from a stream read
        obj: Union = type.__call__(cls, **result)
        object.__setattr__(obj, "__dynamic_sizes__", sizes)
        object.__setattr__(obj, "_buf", buf)

        if cls.size is not None:
            obj._update()

        # Proxify any nested structures
        obj._proxify()

        return obj

    def _write(cls, stream: BinaryIO, data: Union) -> int:
        if cls.dynamic:
            raise NotImplementedError("Writing dynamic unions is not yet supported")

        offset = stream.tell()
        expected_offset = offset + len(cls)

        # Sort by largest field
        fields = sorted(cls.__fields__, key=lambda e: e.type.size or 0, reverse=True)
        anonymous_struct = False

        # Try to write by largest field
        for field in fields:
            if isinstance(field.type, StructureMetaType) and field.name is None:
                # Prefer to write regular fields initially
                anonymous_struct = field.type
                continue

            # Write the value
            field.type._write(stream, getattr(data, field._name))
            break

        # If we haven't written anything yet and we initially skipped an anonymous struct, write it now
        if stream.tell() == offset and anonymous_struct:
            anonymous_struct._write(stream, data)

        # If we haven't filled the union size yet, pad it
        if remaining := expected_offset - stream.tell():
            stream.write(b"\x00" * remaining)

        return stream.tell() - offset


class Union(Structure, metaclass=UnionMetaType):
    """Base class for cstruct union type classes."""

    _buf: bytes

    def __eq__(self, other: object) -> bool:
        return self.__class__ is other.__class__ and bytes(self) == bytes(other)

    def __setattr__(self, attr: str, value: Any) -> None:
        if self.__class__.dynamic:
            raise NotImplementedError("Modifying a dynamic union is not yet supported")

        super().__setattr__(attr, value)
        self._rebuild(attr)

    def _rebuild(self, attr: str) -> None:
        if (cur_buf := getattr(self, "_buf", None)) is None:
            cur_buf = b"\x00" * self.__class__.size

        buf = io.BytesIO(cur_buf)
        field = self.__class__.lookup[attr]
        if field.offset:
            buf.seek(field.offset)

        if (value := getattr(self, attr)) is None:
            value = field.type.__default__()

        field.type._write(buf, value)

        object.__setattr__(self, "_buf", buf.getvalue())
        self._update()

        # (Re-)proxify all values
        self._proxify()

    def _update(self) -> None:
        result, sizes = self.__class__._read_fields(io.BytesIO(self._buf))
        self.__dict__.update(result)
        object.__setattr__(self, "__dynamic_sizes__", sizes)

    def _proxify(self) -> None:
        def _proxy_structure(value: Structure) -> None:
            for field in value.__class__.__fields__:
                if issubclass(field.type, Structure):
                    nested_value = getattr(value, field._name)
                    proxy = UnionProxy(self, field._name, nested_value)
                    object.__setattr__(value, field._name, proxy)
                    _proxy_structure(nested_value)

        _proxy_structure(self)


class UnionProxy:
    __union__: Union
    __attr__: str
    __target__: Structure

    def __init__(self, union: Union, attr: str, target: Structure):
        object.__setattr__(self, "__union__", union)
        object.__setattr__(self, "__attr__", attr)
        object.__setattr__(self, "__target__", target)

    def __len__(self) -> int:
        return len(self.__target__.dumps())

    def __bytes__(self) -> bytes:
        return self.__target__.dumps()

    def __getitem__(self, item: str) -> Any:
        return getattr(self.__target__, item)

    def __repr__(self) -> str:
        return repr(self.__target__)

    def __getattr__(self, attr: str) -> Any:
        return getattr(self.__target__, attr)

    def __setattr__(self, attr: str, value: Any) -> None:
        setattr(self.__target__, attr, value)
        self.__union__._rebuild(self.__attr__)


def attrsetter(path: str) -> Callable[[Any], Any]:
    path, _, attr = path.rpartition(".")
    path = path.split(".")

    def _func(obj: Any, value: Any) -> Any:
        for name in path:
            obj = getattr(obj, name)
        setattr(obj, attr, value)

    return _func


def _codegen(func: FunctionType) -> FunctionType:
    """Decorator that generates a template function with a specified number of fields.

    This code is a little complex but allows use to cache generated functions for a specific number of fields.
    For example, if we generate a structure with 10 fields, we can cache the generated code for that structure.
    We can then reuse that code and patch it with the correct field names when we create a new structure with 10 fields.

    The functions that are decorated with this decorator should take a list of field names and return a string of code.
    The decorated function is needs to be called with the number of fields, instead of the field names.
    The confusing part is that that the original function takes field names, but you then call it with
    the number of fields instead.

    Inspired by https://github.com/dabeaz/dataklasses.

    Args:
        func: The decorated function that takes a list of field names and returns a string of code.

    Returns:
        A cached function that generates the desired function code, to be called with the number of fields.
    """

    def make_func_code(num_fields: int) -> FunctionType:
        exec(func([f"_{n}" for n in range(num_fields)]), {}, d := {})
        return d.popitem()[1]

    make_func_code.__wrapped__ = func
    return lru_cache(make_func_code)


@_codegen
def _make_structure__init__(fields: list[str]) -> str:
    """Generates an ``__init__`` method for a structure with the specified fields.

    Args:
        fields: List of field names.
    """
    field_args = ", ".join(f"{field} = None" for field in fields)
    field_init = "\n".join(
        f" self.{name} = {name} if {name} is not None else _{i}_default" for i, name in enumerate(fields)
    )

    code = f"def __init__(self{', ' + field_args or ''}):\n"
    return code + (field_init or " pass")


@_codegen
def _make_union__init__(fields: list[str]) -> str:
    """Generates an ``__init__`` method for a class with the specified fields using setattr.

    Args:
        fields: List of field names.
    """
    field_args = ", ".join(f"{field} = None" for field in fields)
    field_init = "\n".join(
        f" object.__setattr__(self, '{name}', {name} if {name} is not None else _{i}_default)"
        for i, name in enumerate(fields)
    )

    code = f"def __init__(self{', ' + field_args or ''}):\n"
    return code + (field_init or " pass")


@_codegen
def _make__eq__(fields: list[str]) -> str:
    """Generates an ``__eq__`` method for a class with the specified fields.

    Args:
        fields: List of field names.
    """
    self_vals = ",".join(f"self.{name}" for name in fields)
    other_vals = ",".join(f"other.{name}" for name in fields)

    if self_vals:
        self_vals += ","
    if other_vals:
        other_vals += ","

    # In the future this could be a looser check, e.g. an __eq__ on the classes, which compares the fields
    code = f"""
    def __eq__(self, other):
        if self.__class__ is other.__class__:
            return ({self_vals}) == ({other_vals})
        return False
    """

    return dedent(code)


@_codegen
def _make__bool__(fields: list[str]) -> str:
    """Generates a ``__bool__`` method for a class with the specified fields.

    Args:
        fields: List of field names.
    """
    vals = ", ".join(f"self.{name}" for name in fields)

    code = f"""
    def __bool__(self):
        return any([{vals}])
    """

    return dedent(code)


@_codegen
def _make__hash__(fields: list[str]) -> str:
    """Generates a ``__hash__`` method for a class with the specified fields.

    Args:
        fields: List of field names.
    """
    vals = ", ".join(f"self.{name}" for name in fields)

    code = f"""
    def __hash__(self):
        return hash(({vals}))
    """

    return dedent(code)


def _patch_attributes(func: FunctionType, fields: list[str], start: int = 0) -> FunctionType:
    """Patches a function's attributes.

    Args:
        func: The function to patch.
        fields: List of field names to add.
        start: The starting index for patching. Defaults to 0.
    """
    return type(func)(
        func.__code__.replace(co_names=(*func.__code__.co_names[:start], *fields)),
        func.__globals__,
    )


def _generate_structure__init__(fields: list[Field]) -> FunctionType:
    """Generates an ``__init__`` method for a structure with the specified fields.

    Args:
        fields: List of field names.
    """
    mapping = _generate_co_mapping(fields)

    template: FunctionType = _make_structure__init__(len(fields))
    return type(template)(
        template.__code__.replace(
            co_names=_remap_co_values(template.__code__.co_names, mapping),
            co_varnames=_remap_co_values(template.__code__.co_varnames, mapping),
        ),
        template.__globals__ | {f"__{field._name}_default__": field.type.__default__() for field in fields},
        argdefs=template.__defaults__,
    )


def _generate_union__init__(fields: list[Field]) -> FunctionType:
    """Generates an ``__init__`` method for a union with the specified fields.

    Args:
        fields: List of field names.
    """
    mapping = _generate_co_mapping(fields)

    template: FunctionType = _make_union__init__(len(fields))
    return type(template)(
        template.__code__.replace(
            co_consts=_remap_co_values(template.__code__.co_consts, mapping),
            co_names=_remap_co_values(template.__code__.co_names, mapping),
            co_varnames=_remap_co_values(template.__code__.co_varnames, mapping),
        ),
        template.__globals__ | {f"__{field._name}_default__": field.type.__default__() for field in fields},
        argdefs=template.__defaults__,
    )


def _generate_co_mapping(fields: list[Field]) -> dict[str, str]:
    """Generates a mapping of generated code object names to field names.

    The generated code uses names like ``_0``, ``_1``, etc. for fields, and ``_0_default``, ``_1_default``, etc.
    for default initializer values. Return a mapping of these names to the actual field names.

    Args:
        fields: List of field names.
    """
    return {
        key: value
        for i, field in enumerate(fields)
        for key, value in [(f"_{i}", field._name), (f"_{i}_default", f"__{field._name}_default__")]
    }


def _remap_co_values(value: tuple[Any, ...], mapping: dict[str, str]) -> tuple[Any, ...]:
    """Remaps code object values using a mapping.

    This is used to replace generated code object names with actual field names.

    Args:
        value: The original code object values.
        mapping: A mapping of generated code object names to field names.
    """
    # Only attempt to remap if the value is a string, otherwise return it as is
    # This is to avoid issues with trying to remap non-hashable types, and we only need to replace strings anyway
    return tuple(mapping.get(v, v) if isinstance(v, str) else v for v in value)


def _generate__eq__(fields: list[str]) -> FunctionType:
    """Generates an ``__eq__`` method for a class with the specified fields.

    Args:
        fields: List of field names.
    """
    return _patch_attributes(_make__eq__(len(fields)), fields, 1)


def _generate__bool__(fields: list[str]) -> FunctionType:
    """Generates a ``__bool__`` method for a class with the specified fields.

    Args:
        fields: List of field names.
    """
    return _patch_attributes(_make__bool__(len(fields)), fields, 1)


def _generate__hash__(fields: list[str]) -> FunctionType:
    """Generates a ``__hash__`` method for a class with the specified fields.

    Args:
        fields: List of field names.
    """
    return _patch_attributes(_make__hash__(len(fields)), fields, 1)
