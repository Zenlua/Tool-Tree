from __future__ import annotations

from io import BytesIO
from typing import BinaryIO


def compress(src: bytes | BinaryIO) -> bytes:
    """Sevenbit compress from a file-like object or bytes.

    Args:
        src: File-like object or bytes to compress.

    Returns:
        The compressed data.
    """
    if not hasattr(src, "read"):
        src = BytesIO(src)

    dst = bytearray()

    val = 0
    shift = 0
    while True:
        _byte = src.read(1)
        if not len(_byte):
            break

        val |= (_byte[0] & 0x7F) << shift
        shift += 7

        if shift >= 8:
            dst.append(val & 0xFF)
            val >>= 8
            shift -= 8

    if val:
        dst.append(val & 0xFF)

    return bytes(dst)


def decompress(src: bytes | BinaryIO, wide: bool = False) -> bytes:
    """Sevenbit decompress from a file-like object or bytes.

    Args:
        src: File-like object or bytes to decompress.

    Returns:
        The decompressed data.
    """
    if not hasattr(src, "read"):
        src = BytesIO(src)

    dst = bytearray()

    val = 0
    shift = 0
    while True:
        _byte = src.read(1)
        if not len(_byte):
            break

        val |= _byte[0] << shift
        dst.append(val & 0x7F)
        if wide:
            dst.append(0)

        val >>= 7
        shift += 1
        if shift == 7:
            dst.append(val & 0x7F)
            if wide:
                dst.append(0)
            val >>= 7
            shift = 0

    return bytes(dst)
