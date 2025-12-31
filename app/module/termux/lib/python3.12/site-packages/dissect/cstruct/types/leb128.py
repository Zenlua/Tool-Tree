from __future__ import annotations

from typing import TYPE_CHECKING, Any, BinaryIO

from dissect.cstruct.types.base import BaseType

if TYPE_CHECKING:
    from typing_extensions import Self


class LEB128(int, BaseType):
    """Variable-length code compression to store an arbitrarily large integer in a small number of bytes.

    See https://en.wikipedia.org/wiki/LEB128 for more information and an explanation of the algorithm.
    """

    signed: bool

    @classmethod
    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:
        result = 0
        shift = 0
        while True:
            b = stream.read(1)
            if b == b"":
                raise EOFError("EOF reached, while final LEB128 byte was not yet read")

            b = ord(b)
            result |= (b & 0x7F) << shift
            shift += 7
            if (b & 0x80) == 0:
                break

        if cls.signed and b & 0x40 != 0:
            result |= ~0 << shift

        return cls.__new__(cls, result)

    @classmethod
    def _read_0(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> list[Self]:
        result = []

        while True:
            if (value := cls._read(stream, context)) == 0:
                break

            result.append(value)

        return result

    @classmethod
    def _write(cls, stream: BinaryIO, data: int) -> int:
        # only write negative numbers when in signed mode
        if data < 0 and not cls.signed:
            raise ValueError("Attempt to encode a negative integer using unsigned LEB128 encoding")

        result = bytearray()
        while True:
            # low-order 7 bits of value
            byte = data & 0x7F
            data = data >> 7

            # function works similar for signed- and unsigned integers, except for the check when to stop
            # the encoding process.
            if ((cls.signed and (data == 0 and byte & 0x40 == 0)) or (data == -1 and byte & 0x40 != 0)) or (
                not cls.signed and data == 0
            ):
                result.append(byte)
                break

            # Set high-order bit of byte
            result.append(0x80 | byte)

        stream.write(result)
        return len(result)
