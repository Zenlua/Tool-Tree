from typing import BinaryIO

def decompress(
    src: bytes | BinaryIO,
    uncompressed_size: int = -1,
    return_bytearray: bool = False,
) -> bytes | tuple[bytes, int]: ...
