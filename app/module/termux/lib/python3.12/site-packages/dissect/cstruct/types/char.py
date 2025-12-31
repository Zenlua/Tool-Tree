from __future__ import annotations

from typing import TYPE_CHECKING, Any, BinaryIO

from dissect.cstruct.types.base import EOF, BaseArray, BaseType

if TYPE_CHECKING:
    from typing_extensions import Self


class CharArray(bytes, BaseArray):
    """Character array type for reading and writing byte strings."""

    @classmethod
    def __default__(cls) -> Self:
        return type.__call__(cls, b"\x00" * (0 if cls.dynamic or cls.null_terminated else cls.num_entries))

    @classmethod
    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:
        return type.__call__(cls, super()._read(stream, context))

    @classmethod
    def _write(cls, stream: BinaryIO, data: bytes) -> int:
        if isinstance(data, list) and data and isinstance(data[0], int):
            data = bytes(data)

        elif isinstance(data, str):
            data = data.encode("latin-1")

        if cls.null_terminated:
            return stream.write(data + b"\x00")
        return stream.write(data)


class Char(bytes, BaseType):
    """Character type for reading and writing bytes."""

    ArrayType = CharArray

    @classmethod
    def __default__(cls) -> Self:
        return type.__call__(cls, b"\x00")

    @classmethod
    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:
        return cls._read_array(stream, 1, context)

    @classmethod
    def _read_array(cls, stream: BinaryIO, count: int, context: dict[str, Any] | None = None) -> Self:
        if count == 0:
            return type.__call__(cls, b"")

        data = stream.read(-1 if count == EOF else count)
        if count != EOF and len(data) != count:
            raise EOFError(f"Read {len(data)} bytes, but expected {count}")

        return type.__call__(cls, data)

    @classmethod
    def _read_0(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:
        buf = []
        while True:
            byte = stream.read(1)
            if byte == b"":
                raise EOFError("Read 0 bytes, but expected 1")

            if byte == b"\x00":
                break

            buf.append(byte)

        return type.__call__(cls, b"".join(buf))

    @classmethod
    def _write(cls, stream: BinaryIO, data: bytes | int | str) -> int:
        if isinstance(data, int):
            data = chr(data)

        if isinstance(data, str):
            data = data.encode("latin-1")

        return stream.write(data)
