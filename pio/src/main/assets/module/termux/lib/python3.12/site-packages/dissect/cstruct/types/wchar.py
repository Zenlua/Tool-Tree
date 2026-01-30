from __future__ import annotations

import sys
from typing import Any, BinaryIO, ClassVar

from dissect.cstruct.types.base import EOF, BaseArray, BaseType


class WcharArray(str, BaseArray):
    """Wide-character array type for reading and writing UTF-16 strings."""

    __slots__ = ()

    @classmethod
    def __default__(cls) -> WcharArray:
        return type.__call__(cls, "\x00" * (0 if cls.dynamic or cls.null_terminated else cls.num_entries))

    @classmethod
    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> WcharArray:
        return type.__call__(cls, super()._read(stream, context))

    @classmethod
    def _write(cls, stream: BinaryIO, data: str) -> int:
        if cls.null_terminated:
            data += "\x00"
        return stream.write(data.encode(Wchar.__encoding_map__[cls.cs.endian]))


class Wchar(str, BaseType):
    """Wide-character type for reading and writing UTF-16 characters."""

    ArrayType = WcharArray

    __slots__ = ()
    __encoding_map__: ClassVar[dict[str, str]] = {
        "@": f"utf-16-{sys.byteorder[0]}e",
        "=": f"utf-16-{sys.byteorder[0]}e",
        "<": "utf-16-le",
        ">": "utf-16-be",
        "!": "utf-16-be",
    }

    @classmethod
    def __default__(cls) -> Wchar:
        return type.__call__(cls, "\x00")

    @classmethod
    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Wchar:
        return cls._read_array(stream, 1, context)

    @classmethod
    def _read_array(cls, stream: BinaryIO, count: int, context: dict[str, Any] | None = None) -> Wchar:
        if count == 0:
            return type.__call__(cls, "")

        if count != EOF:
            count *= 2

        data = stream.read(-1 if count == EOF else count)
        if count != EOF and len(data) != count:
            raise EOFError(f"Read {len(data)} bytes, but expected {count}")

        return type.__call__(cls, data.decode(cls.__encoding_map__[cls.cs.endian]))

    @classmethod
    def _read_0(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Wchar:
        buf = []
        while True:
            point = stream.read(2)
            if (bytes_read := len(point)) != 2:
                raise EOFError(f"Read {bytes_read} bytes, but expected 2")

            if point == b"\x00\x00":
                break

            buf.append(point)

        return type.__call__(cls, b"".join(buf).decode(cls.__encoding_map__[cls.cs.endian]))

    @classmethod
    def _write(cls, stream: BinaryIO, data: str) -> int:
        return stream.write(data.encode(cls.__encoding_map__[cls.cs.endian]))
