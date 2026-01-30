from __future__ import annotations

from typing import TYPE_CHECKING, BinaryIO

if TYPE_CHECKING:
    from dissect.cstruct.types import BaseType


class BitBuffer:
    """Implements a bit buffer that can read and write bit fields."""

    def __init__(self, stream: BinaryIO, endian: str):
        self.stream = stream
        self.endian = endian

        self._type: type[BaseType] | None = None
        self._buffer = 0
        self._remaining = 0

    def read(self, field_type: type[BaseType], bits: int) -> int:
        if self._remaining == 0 or self._type != field_type:
            if field_type.size is None:
                raise ValueError("Reading variable-length fields is unsupported")

            self._type = field_type
            self._remaining = field_type.size * 8
            self._buffer = field_type._read(self.stream)

        if isinstance(self._buffer, bytes):
            if self.endian == "<":
                self._buffer = int.from_bytes(self._buffer, "little")
            else:
                self._buffer = int.from_bytes(self._buffer, "big")

        if bits > self._remaining:
            raise ValueError("Reading straddled bits is unsupported")

        if self.endian == "<":
            v = self._buffer & ((1 << bits) - 1)
            self._buffer >>= bits
            self._remaining -= bits
        else:
            v = self._buffer & (((1 << (self._remaining - bits)) - 1) ^ ((1 << self._remaining) - 1))
            v >>= self._remaining - bits
            self._remaining -= bits

        return v

    def write(self, field_type: type[BaseType], data: int, bits: int) -> None:
        if self._remaining == 0 or self._type != field_type:
            if self._type:
                self.flush()

            if field_type.size is None:
                raise ValueError("Writing variable-length fields is unsupported")

            self._remaining = field_type.size * 8
            self._type = field_type

        if self._type is None or self._type.size is None:
            raise ValueError("Invalid state")

        if self.endian == "<":
            self._buffer |= data << (self._type.size * 8 - self._remaining)
        else:
            self._buffer |= data << (self._remaining - bits)

        self._remaining -= bits
        if self._remaining == 0:
            self.flush()

    def flush(self) -> None:
        if self._type is not None:
            self._type._write(self.stream, self._buffer)
        self._type = None
        self._remaining = 0
        self._buffer = 0

    def reset(self) -> None:
        self._type = None
        self._buffer = 0
        self._remaining = 0
