# References:
# - https://github.com/FFmpeg/FFmpeg/blob/master/libavutil/lzo.c
# - https://docs.kernel.org/staging/lzo.html
# - https://github.com/torvalds/linux/blob/master/lib/lzo/lzo1x_decompress_safe.c
from __future__ import annotations

import io
import struct
from typing import BinaryIO

MAX_READ_LENGTH = (1 << 32) - 1000


def _read_length(src: BinaryIO, val: int, mask: int) -> int:
    if (length := val & mask) != 0:
        return length

    while (val := src.read(1)[0]) == 0:
        if length >= MAX_READ_LENGTH:
            raise ValueError("Invalid encoded length")
        length += 255

    return length + mask + val


def decompress(src: bytes | BinaryIO, header: bool = True, buflen: int = -1) -> bytes:
    """LZO decompress from a file-like object or bytes. Assumes no header.

    Arguments are largely compatible with python-lzo API.

    Args:
        src: File-like object or bytes to decompress.
        header: Whether the metadata header is included in the input.
        buflen: If ``header`` is ``False``, a buffer length in bytes must be given that will fit the output.

    Returns:
        The decompressed data.
    """
    if not hasattr(src, "read"):
        src = io.BytesIO(src)

    dst = bytearray()

    if header:
        byte = src.read(1)[0]
        if byte not in [0xF0, 0xF1]:
            raise ValueError("Invalid header value")
        out_len = struct.unpack("<I", src.read(4))[0]
    else:
        out_len = buflen

    val = src.read(1)[0]
    offset = src.tell()
    if src.seek(5) == 5 and val == 17:
        src.seek(offset)
        _ = src.read(1)  # This would be the bitstream version but we don't currently use it
        val = src.read(1)[0]
    else:
        src.seek(offset)

    if val > 17:
        dst += src.read(val - 17)
        val = src.read(1)[0]

        if val < 16:
            raise ValueError("Invalid LZO stream")

    state = 0
    while True:
        if val > 15:
            if val > 63:
                # Copy 3-8 bytes from block within 2kb distance
                length = (val >> 5) - 1
                dist = (src.read(1)[0] << 3) + ((val >> 2) & 7) + 1
            elif val > 31:
                # Copy of small block within 16kb distance
                length = _read_length(src, val, 31)
                val = src.read(1)[0]
                dist = (src.read(1)[0] << 6) + (val >> 2) + 1
            else:
                # Copy of a block within 16...48kB distance
                length = _read_length(src, val, 7)
                dist = (1 << 14) + ((val & 8) << 11)
                val = src.read(1)[0]
                dist += (src.read(1)[0] << 6) + (val >> 2)
                if dist == (1 << 14):
                    if length != 1:
                        raise ValueError("Invalid LZO stream")
                    break
        elif not state:
            # Copy 4 or more literals, depending on the last 4 bits
            length = _read_length(src, val, 15)
            dst += src.read(length + 3)
            val = src.read(1)[0]
            if val > 15:
                continue
            length = 1
            dist = (1 << 11) + (src.read(1)[0] << 2) + (val >> 2) + 1
        else:
            length = 0
            dist = (src.read(1)[0] << 2) + (val >> 2) + 1

        remaining = length + 2
        while remaining > 0:
            match_size = min(remaining, dist)
            dst += dst[-dist : (-dist + match_size) or None]
            remaining -= match_size

        # State is often encoded in the last 2 bits of the value, and used in subsequent iterations
        state = length = val & 3
        dst += src.read(length)

        if len(dst) == out_len:
            break

        val = src.read(1)[0]

    return bytes(dst)
