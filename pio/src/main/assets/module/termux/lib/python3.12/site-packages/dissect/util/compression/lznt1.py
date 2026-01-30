# Reference: https://github.com/google/rekall/blob/master/rekall-core/rekall/plugins/filesystems/lznt1.py
from __future__ import annotations

import array
import io
import struct
from typing import BinaryIO


def _get_displacement(offset: int) -> int:
    """Calculate the displacement."""
    result = 0
    while offset >= 0x10:
        offset >>= 1
        result += 1

    return result


DISPLACEMENT_TABLE = array.array("B", [_get_displacement(x) for x in range(8192)])

COMPRESSED_MASK = 1 << 15
SIGNATURE_MASK = 3 << 12
SIZE_MASK = (1 << 12) - 1
TAG_MASKS = [(1 << i) for i in range(8)]


def decompress(src: bytes | BinaryIO) -> bytes:
    """LZNT1 decompress from a file-like object or bytes.

    Args:
        src: File-like object or bytes to decompress.

    Returns:
        The decompressed data.
    """
    if not hasattr(src, "read"):
        src = io.BytesIO(src)

    offset = src.tell()
    src.seek(0, io.SEEK_END)
    size = src.tell() - offset
    src.seek(offset)

    dst = io.BytesIO()

    while src.tell() - offset < size:
        block_offset = src.tell()
        uncompressed_chunk_offset = dst.tell()

        block_header = struct.unpack("<H", src.read(2))[0]
        if block_header & SIGNATURE_MASK != SIGNATURE_MASK:
            break

        hsize = block_header & SIZE_MASK

        block_end = block_offset + hsize + 3

        if block_header & COMPRESSED_MASK:
            while src.tell() < block_end:
                header = ord(src.read(1))
                for mask in TAG_MASKS:
                    if src.tell() >= block_end:
                        break

                    if header & mask:
                        pointer = struct.unpack("<H", src.read(2))[0]
                        displacement = DISPLACEMENT_TABLE[dst.tell() - uncompressed_chunk_offset - 1]

                        symbol_offset = (pointer >> (12 - displacement)) + 1
                        symbol_length = (pointer & (0xFFF >> displacement)) + 3

                        dst.seek(-symbol_offset, io.SEEK_END)
                        data = dst.read(symbol_length)

                        # Pad the data to make it fit.
                        if 0 < len(data) < symbol_length:
                            data = data * (symbol_length // len(data) + 1)
                            data = data[:symbol_length]

                        dst.seek(0, io.SEEK_END)
                        dst.write(data)
                    else:
                        data = src.read(1)
                        dst.write(data)

        else:
            # Block is not compressed
            data = src.read(hsize + 1)
            dst.write(data)

    return dst.getvalue()
