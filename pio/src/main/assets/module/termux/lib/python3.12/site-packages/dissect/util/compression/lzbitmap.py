# References:
# - https://github.com/eafer/libzbitmap
# - https://github.com/sgan81/apfs-fuse
from __future__ import annotations

import io
import itertools
import struct
from typing import BinaryIO

_H = struct.Struct("<H")


def decompress(src: bytes | BinaryIO) -> bytes:
    """LZBITMAP decompress from a file-like object or bytes.

    Decompresses until EOF or EOS of the input data.

    Args:
        src: File-like object or bytes to decompress.

    Returns:
        The decompressed data.
    """
    if not hasattr(src, "read"):
        src = io.BytesIO(src)

    if src.read(4) != b"ZBM\x09":
        raise ValueError("Not a valid LZBITMAP stream")

    dst = bytearray()

    while True:
        compressed_size = int.from_bytes(src.read(3), "little")
        uncompressed_size = int.from_bytes(src.read(3), "little")

        if compressed_size == uncompressed_size + 6:  # chunk header size
            # Not compressed
            dst += src.read(uncompressed_size)

        elif uncompressed_size == 0:
            # End of stream
            break

        else:
            # Compressed
            distance_offset = int.from_bytes(src.read(3), "little")
            bitmap_offset = int.from_bytes(src.read(3), "little")
            token_offset = int.from_bytes(src.read(3), "little")
            literal_offset = 15

            # Buffer the whole chunk
            src.seek(-15, io.SEEK_CUR)
            buf = memoryview(src.read(compressed_size))

            # Build the bitmap/token map
            token_map = []
            bits = int.from_bytes(buf[-17:], "little")
            for i in range(0xF):
                if i < 3:
                    token_map.append((None, i))
                else:
                    token_map.append((bits & 0xFF, (bits >> 8) & 3))
                    bits >>= 10

            # Tokens are stored as nibbles, so we need to split each byte into two
            tokens = itertools.chain.from_iterable((b & 0xF, (b >> 4) & 0xF) for b in buf[token_offset:-17])

            # Initial match distance is 8, and is not reset between tokens
            distance = 8

            prev_token = None
            while uncompressed_size > 0:
                if (idx := next(tokens, None) if prev_token is None else prev_token) is None:
                    break

                if idx == 0xF:
                    # 0xF indicates a repeat count
                    raise ValueError("Invalid token index in LZBITMAP stream")

                if (repeat := next(tokens)) != 0xF:
                    # No repeat count, store the token for next iteration
                    prev_token = repeat
                    repeat = 1
                else:
                    # Repeat count, read and sum
                    prev_token = None
                    total = 4
                    while repeat == 0xF:
                        repeat = next(tokens)
                        total += repeat

                        if total < repeat:
                            raise ValueError("Invalid repeat count in LZBITMAP stream")

                    repeat = total

                for _ in range(repeat):
                    bitmap, token = token_map[idx]
                    if idx < 3:
                        # Index 0, 1, 2 are special and indicate we need to read a bitmap from the bitmap region
                        bitmap = buf[bitmap_offset]
                        bitmap_offset += 1

                    if token == 1:
                        # 1-byte distance
                        distance = buf[distance_offset]
                        distance_offset += 1
                    elif token == 2:
                        # 2-byte distance
                        (distance,) = _H.unpack_from(buf, distance_offset)
                        distance_offset += 2

                    for _ in range(8):
                        if bitmap & 1:
                            # Literal
                            dst.append(buf[literal_offset])
                            literal_offset += 1
                        else:
                            # Match
                            if distance > len(dst):
                                raise ValueError("Invalid match distance in LZBITMAP stream")
                            dst.append(dst[-distance])

                        bitmap >>= 1
                        uncompressed_size -= 1
                        if uncompressed_size == 0:
                            break

    return bytes(dst)
