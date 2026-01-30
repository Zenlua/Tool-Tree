from __future__ import annotations

from typing import TYPE_CHECKING, Any, BinaryIO

from dissect.cstruct.types.base import BaseType
from dissect.cstruct.utils import ENDIANNESS_MAP

if TYPE_CHECKING:
    from typing_extensions import Self


class Int(int, BaseType):
    """Integer type that can span an arbitrary amount of bytes."""

    signed: bool

    @classmethod
    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:
        data = stream.read(cls.size)

        if len(data) != cls.size:
            raise EOFError(f"Read {len(data)} bytes, but expected {cls.size}")

        return cls.from_bytes(data, ENDIANNESS_MAP[cls.cs.endian], signed=cls.signed)

    @classmethod
    def _read_0(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:
        result = []

        while True:
            if (value := cls._read(stream, context)) == 0:
                break

            result.append(value)

        return result

    @classmethod
    def _write(cls, stream: BinaryIO, data: int) -> int:
        return stream.write(data.to_bytes(cls.size, ENDIANNESS_MAP[cls.cs.endian], signed=cls.signed))
