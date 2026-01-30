from __future__ import annotations

import pprint
import string
import sys
from enum import Enum
from typing import TYPE_CHECKING

from dissect.cstruct.types.pointer import Pointer
from dissect.cstruct.types.structure import Structure

if TYPE_CHECKING:
    from collections.abc import Iterator
    from typing import Literal

COLOR_RED = "\033[1;31m"
COLOR_GREEN = "\033[1;32m"
COLOR_YELLOW = "\033[1;33m"
COLOR_BLUE = "\033[1;34m"
COLOR_PURPLE = "\033[1;35m"
COLOR_CYAN = "\033[1;36m"
COLOR_WHITE = "\033[1;37m"
COLOR_NORMAL = "\033[1;0m"

COLOR_BG_RED = "\033[1;41m\033[1;37m"
COLOR_BG_GREEN = "\033[1;42m\033[1;37m"
COLOR_BG_YELLOW = "\033[1;43m\033[1;37m"
COLOR_BG_BLUE = "\033[1;44m\033[1;37m"
COLOR_BG_PURPLE = "\033[1;45m\033[1;37m"
COLOR_BG_CYAN = "\033[1;46m\033[1;37m"
COLOR_BG_WHITE = "\033[1;47m\033[1;30m"

PRINTABLE = string.digits + string.ascii_letters + string.punctuation + " "

ENDIANNESS_MAP: dict[str, Literal["big", "little"]] = {
    "@": sys.byteorder,
    "=": sys.byteorder,
    "<": "little",
    ">": "big",
    "!": "big",
    "network": "big",
}

Palette = list[tuple[int, str]]


def _hexdump(data: bytes, palette: Palette | None = None, offset: int = 0, prefix: str = "") -> Iterator[str]:
    """Hexdump some data.

    Args:
        data: Bytes to hexdump.
        offset: Byte offset of the hexdump.
        prefix: Optional prefix.
        palette: Colorize the hexdump using this color pattern.
    """
    if palette:
        palette = palette[::-1]

    remaining = 0
    active = None

    for i in range(0, len(data), 16):
        values = ""
        chars = []

        for j in range(16):
            if not active and palette:
                remaining, active = palette.pop()
                while remaining == 0:
                    if len(palette) == 0:
                        # Last palette tuple is empty: print remaining whitespaces
                        active = ""
                        break
                    else:
                        remaining, active = palette.pop()
                values += active
            elif active and j == 0:
                values += active

            if i + j >= len(data):
                values += "  "
            else:
                char = data[i + j]
                char = chr(char)

                print_char = char if char in PRINTABLE else "."

                if active:
                    values += f"{ord(char):02x}"
                    chars.append(active + print_char + COLOR_NORMAL)
                else:
                    values += f"{ord(char):02x}"
                    chars.append(print_char)

                remaining -= 1
                if remaining == 0:
                    active = None

                    if palette is not None:
                        values += COLOR_NORMAL

                if j == 15 and palette is not None:
                    values += COLOR_NORMAL

            values += " "
            if j == 7:
                values += " "

        chars = "".join(chars)
        yield f"{prefix}{offset + i:08x}  {values:48s}  {chars}"


def hexdump(
    data: bytes, palette: Palette | None = None, offset: int = 0, prefix: str = "", output: str = "print"
) -> Iterator[str] | str | None:
    """Hexdump some data.

    Args:
        data: Bytes to hexdump.
        palette: Colorize the hexdump using this color pattern.
        offset: Byte offset of the hexdump.
        prefix: Optional prefix.
        output: Output format, can be 'print', 'generator' or 'string'.
    """
    generator = _hexdump(data, palette, offset, prefix)
    if output == "print":
        print("\n".join(generator))
        return None
    if output == "generator":
        return generator
    if output == "string":
        return "\n".join(list(generator))
    raise ValueError(f"Invalid output argument: {output!r} (should be 'print', 'generator' or 'string').")


def _dumpstruct(
    structure: Structure,
    data: bytes,
    offset: int,
    color: bool,
    output: str,
) -> str | None:
    palette = []
    colors = [
        (COLOR_RED, COLOR_BG_RED),
        (COLOR_GREEN, COLOR_BG_GREEN),
        (COLOR_YELLOW, COLOR_BG_YELLOW),
        (COLOR_BLUE, COLOR_BG_BLUE),
        (COLOR_PURPLE, COLOR_BG_PURPLE),
        (COLOR_CYAN, COLOR_BG_CYAN),
        (COLOR_WHITE, COLOR_BG_WHITE),
    ]
    ci = 0
    out = [f"struct {structure.__class__.__name__}:"]
    foreground, background = None, None
    for field in structure.__class__.__fields__:
        if getattr(field.type, "anonymous", False):
            continue

        value = getattr(structure, field._name)
        if isinstance(value, (str, Pointer, Enum)):
            value = repr(value)
        elif isinstance(value, int):
            value = hex(value)
        elif isinstance(value, list):
            value = pprint.pformat(value)
            if "\n" in value:
                value = value.replace("\n", f"\n{' ' * (len(field._name) + 4)}")

        if color:
            foreground, background = colors[ci % len(colors)]
            size = structure.__sizes__[field._name]
            palette.append((size, background))
            ci += 1
            out.append(f"- {foreground}{field._name}{COLOR_NORMAL}: {value}")
        else:
            out.append(f"- {field._name}: {value}")

    out = "\n".join(out)

    if output == "print":
        print()
        hexdump(data, palette, offset=offset)
        print()
        print(out)
    elif output == "string":
        return f"\n{hexdump(data, palette, offset=offset, output='string')}\n\n{out}"
    return None


def dumpstruct(
    obj: Structure | type[Structure],
    data: bytes | None = None,
    offset: int = 0,
    color: bool = True,
    output: str = "print",
) -> str | None:
    """Dump a structure or parsed structure instance.

    Prints a colorized hexdump and parsed structure output.

    Args:
        obj: Structure to dump.
        data: Bytes to parse the Structure on, if obj is not a parsed Structure already.
        offset: Byte offset of the hexdump.
        output: Output format, can be 'print' or 'string'.
    """
    if output not in ("print", "string"):
        raise ValueError(f"Invalid output argument: {output!r} (should be 'print' or 'string').")

    if isinstance(obj, Structure):
        return _dumpstruct(obj, obj.dumps(), offset, color, output)
    if issubclass(obj, Structure) and data is not None:
        return _dumpstruct(obj(data), data, offset, color, output)
    raise ValueError("Invalid arguments")


def pack(value: int, size: int | None = None, endian: str = "little") -> bytes:
    """Pack an integer value to a given bit size, endianness.

    Arguments:
        value: Value to pack.
        size: Integer size in bits.
        endian: Endianness to use (little, big, network, <, > or !)
    """
    size = ((size or value.bit_length()) + 7) // 8
    return value.to_bytes(size, ENDIANNESS_MAP.get(endian, endian), signed=value < 0)


def unpack(value: bytes, size: int | None = None, endian: str = "little", sign: bool = False) -> int:
    """Unpack an integer value from a given bit size, endianness and sign.

    Arguments:
        value: Value to unpack.
        size: Integer size in bits.
        endian: Endianness to use (little, big, network, <, > or !)
        sign: Signedness of the integer.
    """
    if size and len(value) != size // 8:
        raise ValueError(f"Invalid byte value, expected {size // 8} bytes, got {len(value)} bytes")
    return int.from_bytes(value, ENDIANNESS_MAP.get(endian, endian), signed=sign)


def p8(value: int, endian: str = "little") -> bytes:
    """Pack an 8 bit integer.

    Arguments:
        value: Value to pack.
        endian: Endianness to use (little, big, network, <, > or !)
    """
    return pack(value, 8, endian)


def p16(value: int, endian: str = "little") -> bytes:
    """Pack a 16 bit integer.

    Arguments:
        value: Value to pack.
        endian: Endianness to use (little, big, network, <, > or !)
    """
    return pack(value, 16, endian)


def p32(value: int, endian: str = "little") -> bytes:
    """Pack a 32 bit integer.

    Arguments:
        value: Value to pack.
        endian: Endianness to use (little, big, network, <, > or !)
    """
    return pack(value, 32, endian)


def p64(value: int, endian: str = "little") -> bytes:
    """Pack a 64 bit integer.

    Arguments:
        value: Value to pack.
        endian: Endianness to use (little, big, network, <, > or !)
    """
    return pack(value, 64, endian)


def u8(value: bytes, endian: str = "little", sign: bool = False) -> int:
    """Unpack an 8 bit integer.

    Arguments:
        value: Value to unpack.
        endian: Endianness to use (little, big, network, <, > or !)
        sign: Signedness of the integer.
    """
    return unpack(value, 8, endian, sign)


def u16(value: bytes, endian: str = "little", sign: bool = False) -> int:
    """Unpack a 16 bit integer.

    Arguments:
        value: Value to unpack.
        endian: Endianness to use (little, big, network, <, > or !)
        sign: Signedness of the integer.
    """
    return unpack(value, 16, endian, sign)


def u32(value: bytes, endian: str = "little", sign: bool = False) -> int:
    """Unpack a 32 bit integer.

    Arguments:
        value: Value to unpack.
        endian: Endianness to use (little, big, network, <, > or !)
        sign: Signedness of the integer.
    """
    return unpack(value, 32, endian, sign)


def u64(value: bytes, endian: str = "little", sign: bool = False) -> int:
    """Unpack a 64 bit integer.

    Arguments:
        value: Value to unpack.
        endian: Endianness to use (little, big, network, <, > or !)
        sign: Signedness of the integer.
    """
    return unpack(value, 64, endian, sign)


def swap(value: int, size: int) -> int:
    """Swap the endianness of an integer with a given bit size.

    Arguments:
        value: Integer to swap.
        size: Integer size in bits.
    """
    return unpack(pack(value, size, ">"), size, "<")


def swap16(value: int) -> int:
    """Swap the endianness of a 16 bit integer.

    Arguments:
        value: Integer to swap.
    """
    return swap(value, 16)


def swap32(value: int) -> int:
    """Swap the endianness of a 32 bit integer.

    Arguments:
        value: Integer to swap.
    """
    return swap(value, 32)


def swap64(value: int) -> int:
    """Swap the endianness of a 64 bit integer.

    Arguments:
        value: Integer to swap.
    """
    return swap(value, 64)
