# Reference: [MS-XCA]
from __future__ import annotations

import io
import struct
from typing import BinaryIO


def decompress(src: bytes | BinaryIO) -> bytes:
    """LZXPRESS decompress from a file-like object or bytes.

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

    dst = bytearray()

    buffered_flags = 0
    buffered_flags_count = 0
    last_length_half_byte = 0

    while src.tell() - offset < size:
        if buffered_flags_count == 0:
            buffered_flags = struct.unpack("<I", src.read(4))[0]
            buffered_flags_count = 32

        buffered_flags_count -= 1
        if buffered_flags & (1 << buffered_flags_count) == 0:
            dst.append(ord(src.read(1)))
        else:
            if src.tell() - offset == size:
                break

            match = struct.unpack("<H", src.read(2))[0]
            match_offset, match_length = divmod(match, 8)
            match_offset += 1

            if match_length == 7:
                if last_length_half_byte == 0:
                    last_length_half_byte = src.tell()
                    match_length = ord(src.read(1)) % 16
                else:
                    rewind = src.tell()
                    src.seek(last_length_half_byte)
                    match_length = ord(src.read(1)) // 16
                    src.seek(rewind)
                    last_length_half_byte = 0

                if match_length == 15:
                    match_length = ord(src.read(1))
                    if match_length == 255:
                        match_length = struct.unpack("<H", src.read(2))[0]
                        if match_length == 0:
                            match_length = struct.unpack("<I", src.read(4))[0]

                        if match_length < 15 + 7:
                            raise ValueError("wrong match length")

                        match_length -= 15 + 7
                    match_length += 15
                match_length += 7
            match_length += 3

            remaining = match_length
            while remaining > 0:
                match_size = min(remaining, match_offset)
                dst += dst[-match_offset : (-match_offset + match_size) or None]
                remaining -= match_size

    return bytes(dst)
