# Made in Japan

from __future__ import annotations

import io
import logging
from enum import Enum
from textwrap import dedent, indent
from typing import TYPE_CHECKING

from dissect.cstruct.bitbuffer import BitBuffer
from dissect.cstruct.types import (
    Array,
    BaseType,
    Char,
    CharArray,
    Flag,
    Int,
    Packed,
    Pointer,
    Structure,
    Union,
    Void,
    VoidArray,
    Wchar,
    WcharArray,
)
from dissect.cstruct.types.base import BaseArray
from dissect.cstruct.types.enum import EnumMetaType
from dissect.cstruct.types.packed import _struct

if TYPE_CHECKING:
    from collections.abc import Iterator
    from types import MethodType

    from dissect.cstruct.cstruct import cstruct
    from dissect.cstruct.types.structure import Field

SUPPORTED_TYPES = (
    Array,
    Char,
    CharArray,
    Enum,
    Flag,
    Int,
    Packed,
    Pointer,
    Structure,
    Void,
    Wchar,
    WcharArray,
    VoidArray,
)

log = logging.getLogger(__name__)

python_compile = compile


def compile(structure: type[Structure]) -> type[Structure]:
    return Compiler(structure.cs).compile(structure)


class Compiler:
    def __init__(self, cs: cstruct):
        self.cs = cs

    def compile(self, structure: type[Structure]) -> type[Structure]:
        if issubclass(structure, Union):
            return structure

        try:
            structure._read = self.compile_read(structure.__fields__, structure.__name__, structure.__align__)
            structure.__compiled__ = True
        except Exception as e:
            # Silently ignore, we didn't compile unfortunately
            log.debug("Failed to compile %s", structure, exc_info=e)

        return structure

    def compile_read(self, fields: list[Field], name: str | None = None, align: bool = False) -> MethodType:
        return _ReadSourceGenerator(self.cs, fields, name, align).generate()


class _ReadSourceGenerator:
    def __init__(self, cs: cstruct, fields: list[Field], name: str | None = None, align: bool = False):
        self.cs = cs
        self.fields = fields
        self.name = name
        self.align = align

        self.field_map: dict[str, Field] = {}
        self._token_id = 0

    def _map_field(self, field: Field) -> str:
        token = f"_{self._token_id}"
        self.field_map[token] = field
        self._token_id += 1
        return token

    def generate(self) -> MethodType:
        source = self.generate_source()
        symbols = {token: field.type for token, field in self.field_map.items()}

        code = python_compile(source, f"<compiled {self.name or 'anonymous'}._read>", "exec")
        exec(code, {"BitBuffer": BitBuffer, "_struct": _struct, **symbols}, d := {})
        obj = d.popitem()[1]
        obj.__source__ = source

        return classmethod(obj)

    def generate_source(self) -> str:
        preamble = """
        r = {}
        s = {}
        o = stream.tell()
        """

        if any(field.bits for field in self.fields):
            preamble += "bit_reader = BitBuffer(stream, cls.cs.endian)\n"

        read_code = "\n".join(self._generate_fields())

        outro = """
        obj = type.__call__(cls, **r)
        obj.__dynamic_sizes__ = s

        return obj
        """

        code = indent(dedent(preamble).lstrip() + read_code + dedent(outro), "    ")

        return f"def _read(cls, stream, context=None):\n{code}"

    def _generate_fields(self) -> Iterator[str]:
        current_offset = 0
        current_block: list[Field] = []
        prev_was_bits = False
        prev_bits_type = None
        bits_remaining = 0
        bits_rollover = False

        def flush() -> Iterator[str]:
            if current_block:
                if self.align and current_block[0].offset is None:
                    yield f"stream.seek(-stream.tell() & ({current_block[0].alignment} - 1), {io.SEEK_CUR})"

                yield from self._generate_packed(current_block)
                current_block[:] = []

        def align_to_field(field: Field) -> Iterator[str]:
            nonlocal current_offset

            if field.offset is not None and field.offset != current_offset:
                # If a field has a set offset and it's not the same as the current tracked offset, seek to it
                yield f"stream.seek(o + {field.offset})"
                current_offset = field.offset

            if self.align and field.offset is None:
                yield f"stream.seek(-stream.tell() & ({field.alignment} - 1), {io.SEEK_CUR})"

        for field in self.fields:
            field_type = field.type

            if isinstance(field_type, EnumMetaType):
                field_type = field_type.type

            if not issubclass(field_type, SUPPORTED_TYPES):
                raise TypeError(f"Unsupported type for compiler: {field_type}")

            if prev_was_bits and not field.bits:
                yield "bit_reader.reset()"
                prev_was_bits = False
                bits_remaining = 0

            try:
                size = len(field_type)
                is_dynamic = False
            except TypeError:
                size = None
                is_dynamic = True

            # Sub structure
            if issubclass(field_type, Structure):
                yield from flush()
                yield from align_to_field(field)
                yield from self._generate_structure(field)

            # Array of structures and multi-dimensional arrays
            elif issubclass(field_type, (Array, CharArray, WcharArray)) and (
                issubclass(field_type.type, Structure) or issubclass(field_type.type, BaseArray) or is_dynamic
            ):
                yield from flush()
                yield from align_to_field(field)
                yield from self._generate_array(field)

            # Bit fields
            elif field.bits:
                if size is None:
                    raise TypeError(f"Unsupported type for bit field: {field_type}")

                if not prev_was_bits:
                    prev_bits_type = field_type
                    prev_was_bits = True

                if bits_remaining == 0 or prev_bits_type != field_type:
                    bits_remaining = (size * 8) - field.bits
                    bits_rollover = True

                yield from flush()
                yield from align_to_field(field)
                yield from self._generate_bits(field)

            # Everything else - basic and composite types (and arrays of them)
            else:
                current_block.append(field)

            if current_offset is not None and size is not None and (not field.bits or bits_rollover):
                current_offset += size
                bits_rollover = False

        yield from flush()

        if self.align:
            yield f"stream.seek(-stream.tell() & (cls.alignment - 1), {io.SEEK_CUR})"

    def _generate_structure(self, field: Field) -> Iterator[str]:
        template = f"""
        {"_s = stream.tell()" if field.type.dynamic else ""}
        r["{field._name}"] = {self._map_field(field)}._read(stream, context=r)
        {f's["{field._name}"] = stream.tell() - _s' if field.type.dynamic else ""}
        """

        yield dedent(template)

    def _generate_array(self, field: Field) -> Iterator[str]:
        template = f"""
        {"_s = stream.tell()" if field.type.dynamic else ""}
        r["{field._name}"] = {self._map_field(field)}._read(stream, context=r)
        {f's["{field._name}"] = stream.tell() - _s' if field.type.dynamic else ""}
        """

        yield dedent(template)

    def _generate_bits(self, field: Field) -> Iterator[str]:
        lookup = self._map_field(field)
        read_type = "_t"
        field_type = field.type
        if issubclass(field_type, (Enum, Flag)):
            read_type += ".type"
            field_type = field_type.type

        if issubclass(field_type, Char):
            field_type = field_type.cs.uint8
            lookup = "cls.cs.uint8"

        template = f"""
        _t = {lookup}
        r["{field._name}"] = type.__call__(_t, bit_reader.read({read_type}, {field.bits}))
        """

        yield dedent(template)

    def _generate_packed(self, fields: list[Field]) -> Iterator[str]:
        info = list(_generate_struct_info(self.cs, fields, self.align))
        reads = []

        size = 0
        slice_index = 0
        for field, count, _ in info:
            if field is None:
                # Padding
                size += count
                continue

            field_type = field.type
            read_type = _get_read_type(self.cs, field_type)

            if issubclass(field_type, (Array, CharArray, WcharArray)):
                count = field_type.num_entries
                read_type = _get_read_type(self.cs, field_type.type)

                if issubclass(read_type, (Char, Wchar, Int)):
                    count *= read_type.size
                    getter = f"buf[{size}:{size + count}]"
                else:
                    getter = f"data[{slice_index}:{slice_index + count}]"
                    slice_index += count
            elif issubclass(read_type, (Char, Wchar, Int)):
                getter = f"buf[{size}:{size + read_type.size}]"
            else:
                getter = f"data[{slice_index}]"
                slice_index += 1

            if issubclass(read_type, (Wchar, Int)):
                # Types that parse bytes further down to their own type
                parser_template = "{type}({getter})"
            else:
                # All other types can be simply intialized
                parser_template = "type.__call__({type}, {getter})"

            # Create the final reading code
            if issubclass(field_type, Array):
                reads.append(f"_t = {self._map_field(field)}")
                reads.append("_et = _t.type")

                if issubclass(field_type.type, Int):
                    reads.append(f"_b = {getter}")
                    item_parser = parser_template.format(type="_et", getter=f"_b[i:i + {field_type.type.size}]")
                    list_comp = f"[{item_parser} for i in range(0, {count}, {field_type.type.size})]"
                elif issubclass(field_type.type, Pointer):
                    item_parser = "_et.__new__(_et, e, stream, r)"
                    list_comp = f"[{item_parser} for e in {getter}]"
                else:
                    item_parser = parser_template.format(type="_et", getter="e")
                    list_comp = f"[{item_parser} for e in {getter}]"

                parser = f"type.__call__(_t, {list_comp})"
            elif issubclass(field_type, CharArray):
                parser = f"type.__call__({self._map_field(field)}, {getter})"
            elif issubclass(field_type, Pointer):
                reads.append(f"_pt = {self._map_field(field)}")
                parser = f"_pt.__new__(_pt, {getter}, stream, r)"
            else:
                parser = parser_template.format(type=self._map_field(field), getter=getter)

            reads.append(f'r["{field._name}"] = {parser}')
            reads.append("")  # Generates a newline in the resulting code

            size += field_type.size

        fmt = _optimize_struct_fmt(info)
        if fmt == "x" or (len(fmt) == 2 and fmt[1] == "x"):
            unpack = ""
        else:
            unpack = f'data = _struct(cls.cs.endian, "{fmt}").unpack(buf)\n'

        template = f"""
        buf = stream.read({size})
        if len(buf) != {size}: raise EOFError()
        {unpack}
        """

        yield dedent(template) + "\n".join(reads)


def _generate_struct_info(cs: cstruct, fields: list[Field], align: bool = False) -> Iterator[tuple[Field, int, str]]:
    if not fields:
        return

    current_offset = fields[0].offset
    imaginary_offset = 0
    for field in fields:
        # We moved -- probably due to alignment
        if field.offset is not None and (drift := field.offset - current_offset) > 0:
            yield None, drift, "x"
            current_offset += drift

        if align and field.offset is None and (drift := -imaginary_offset & (field.alignment - 1)) > 0:
            # Assume we started at a correctly aligned boundary
            yield None, drift, "x"
            imaginary_offset += drift

        count = 1
        read_type = _get_read_type(cs, field.type)

        # Drop voids
        if issubclass(read_type, (Void, VoidArray)):
            continue

        # Array of more complex types are handled elsewhere
        if issubclass(read_type, (Array, CharArray, WcharArray)):
            count = read_type.num_entries
            read_type = _get_read_type(cs, read_type.type)

        # Take the pack char for Packed
        if issubclass(read_type, Packed):
            yield field, count, read_type.packchar

        # Other types are byte based
        # We don't actually unpack anything here but slice directly out of the buffer
        elif issubclass(read_type, (Char, Wchar, Int)):
            yield field, count * read_type.size, "x"

        size = count * read_type.size
        imaginary_offset += size
        if current_offset is not None:
            current_offset += size


def _optimize_struct_fmt(info: Iterator[tuple[Field, int, str]]) -> str:
    chars = []

    current_count = 0
    current_char = None

    for _, count, char in info:
        if current_char is None:
            current_count = count
            current_char = char
            continue

        if char != current_char:
            if current_count:
                chars.append((current_count, current_char))
            current_count = count
            current_char = char
        else:
            current_count += count

    if current_char is not None and current_count:
        chars.append((current_count, current_char))

    return "".join(f"{count if count > 1 else ''}{char}" for count, char in chars)


def _get_read_type(cs: cstruct, type_: type[BaseType]) -> type[BaseType]:
    if issubclass(type_, (Enum, Flag)):
        type_ = type_.type

    if issubclass(type_, Pointer):
        type_ = cs.pointer

    return cs.resolve(type_)
