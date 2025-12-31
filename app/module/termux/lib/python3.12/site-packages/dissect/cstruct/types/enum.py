from __future__ import annotations

import sys
from enum import Enum as _Enum
from enum import EnumMeta, IntEnum, IntFlag
from typing import TYPE_CHECKING, Any, BinaryIO, TypeVar, overload

from dissect.cstruct.types.base import Array, BaseType, MetaType

if TYPE_CHECKING:
    from typing_extensions import Self

    from dissect.cstruct.cstruct import cstruct


PY_311 = sys.version_info >= (3, 11, 0)
PY_312 = sys.version_info >= (3, 12, 0)

_S = TypeVar("_S")


class EnumMetaType(EnumMeta, MetaType):
    type: type[BaseType]

    @overload
    def __call__(cls, value: cstruct, name: str, type_: type[BaseType], *args, **kwargs) -> type[Enum]: ...

    @overload
    def __call__(cls: type[_S], value: int | BinaryIO | bytes) -> _S: ...

    def __call__(
        cls,
        value: cstruct | int | BinaryIO | bytes | None = None,
        name: str | None = None,
        type_: type[BaseType] | None = None,
        *args,
        **kwargs,
    ) -> Enum | type[Enum]:
        if name is None:
            if value is None:
                value = cls.type.__default__()

            if not isinstance(value, int):
                # value is a parsable value
                value = cls.type(value)

            return super().__call__(value)

        # We are constructing a new Enum class
        # cs is the cstruct instance, but we can't isinstance check it due to circular imports
        cs = value
        if not issubclass(type_, int):
            raise TypeError("Enum can only be created from int type")

        enum_cls = super().__call__(name, *args, **kwargs)
        enum_cls.cs = cs
        enum_cls.type = type_
        enum_cls.size = type_.size
        enum_cls.dynamic = type_.dynamic
        enum_cls.alignment = type_.alignment

        _fix_alias_members(enum_cls)

        return enum_cls

    @overload
    def __getitem__(cls: type[_S], name: str) -> _S: ...

    @overload
    def __getitem__(cls: type[_S], name: int) -> Array: ...

    def __getitem__(cls: type[_S], name: str | int) -> _S | Array:
        if isinstance(name, str):
            return super().__getitem__(name)
        return MetaType.__getitem__(cls, name)

    __len__ = MetaType.__len__

    def __contains__(cls, value: Any) -> bool:
        # We used to let stdlib enum handle `__contains__``` but this commit is incompatible with our API:
        # https://github.com/python/cpython/commit/8a9aee71268c77867d3cc96d43cbbdcbe8c0e1e8
        if isinstance(value, cls):
            return True
        return value in cls._value2member_map_

    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:
        return cls(cls.type._read(stream, context))

    def _read_array(cls, stream: BinaryIO, count: int, context: dict[str, Any] | None = None) -> list[Self]:
        return list(map(cls, cls.type._read_array(stream, count, context)))

    def _read_0(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> list[Self]:
        return list(map(cls, cls.type._read_0(stream, context)))

    def _write(cls, stream: BinaryIO, data: Enum) -> int:
        return cls.type._write(stream, data.value)

    def _write_array(cls, stream: BinaryIO, array: list[BaseType | int]) -> int:
        data = [entry.value if isinstance(entry, _Enum) else entry for entry in array]
        return cls.type._write_array(stream, data)

    def _write_0(cls, stream: BinaryIO, array: list[BaseType | int]) -> int:
        data = [entry.value if isinstance(entry, _Enum) else entry for entry in array]
        return cls._write_array(stream, [*data, cls.type.__default__()])


def _fix_alias_members(cls: type[Enum]) -> None:
    # Emulate aenum NoAlias behaviour
    # https://github.com/ethanfurman/aenum/blob/master/aenum/doc/aenum.rst
    if len(cls._member_names_) == len(cls._member_map_):
        return

    for name, member in cls._member_map_.items():
        if name != member.name:
            new_member = int.__new__(cls, member.value)
            new_member._name_ = name
            new_member._value_ = member.value

            type.__setattr__(cls, name, new_member)
            cls._member_names_.append(name)
            cls._member_map_[name] = new_member
            cls._value2member_map_[member.value] = new_member


class Enum(BaseType, IntEnum, metaclass=EnumMetaType):
    """Enum type supercharged with cstruct functionality.

    Enums are (mostly) compatible with the Python 3 standard library ``IntEnum`` with some notable differences:
        - Duplicate members are their own unique member instead of being an alias
        - Non-existing values are allowed and handled similarly to ``IntFlag``: ``<Enum: 0>``
        - Enum members are only considered equal if the enum class is the same

    Enums can be made using any integer type.

    Example:
        When using the default C-style parser, the following syntax is supported::

            enum <name> [: <type>] {
                <values>
            };

        For example, an enum that has A=1, B=5 and C=6 could be written like so::

            enum Test : uint16 {
                A, B=5, C
            };
    """

    if PY_311:

        def __repr__(self) -> str:
            # Use the IntFlag repr as a base since it handles unknown values the way we want it
            # I.e. <Color: 255> instead of <Color.None: 255>
            result = IntFlag.__repr__(self)
            if not self.__class__.__name__:
                # Deal with anonymous enums by stripping off the first bit
                # I.e. <.RED: 1> -> <RED: 1>
                result = f"<{result[2:]}"
            return result

        def __str__(self) -> str:
            # We differentiate with standard Python enums in that we use a more descriptive str representation
            # Standard Python enums just use the integer value as str, we use EnumName.ValueName
            # In case of anonymous enums, we just use the ValueName
            # In case of unknown members, we use the integer value (in combination with the EnumName if there is one)
            base = f"{self.__class__.__name__}." if self.__class__.__name__ else ""
            value = self.name if self.name is not None else str(self.value)
            return f"{base}{value}"

    else:

        def __repr__(self) -> str:
            name = self.__class__.__name__
            if self._name_ is not None:
                if name:
                    name += "."
                name += self._name_
            return f"<{name}: {self._value_!r}>"

        def __str__(self) -> str:
            base = f"{self.__class__.__name__}." if self.__class__.__name__ else ""
            value = self._name_ if self._name_ is not None else str(self._value_)
            return f"{base}{value}"

    def __eq__(self, other: int | Enum) -> bool:
        if isinstance(other, Enum) and other.__class__ is not self.__class__:
            return False

        # Python <= 3.10 compatibility
        if isinstance(other, Enum):
            other = other.value

        return self.value == other

    def __ne__(self, value: int | Enum) -> bool:
        return not self.__eq__(value)

    def __hash__(self) -> int:
        return hash((self.__class__, self.name, self.value))

    @classmethod
    def _missing_(cls, value: int) -> Self:
        # Emulate FlagBoundary.KEEP for enum (allow values other than the defined members)
        new_member = int.__new__(cls, value)
        new_member._name_ = None
        new_member._value_ = value
        return new_member
