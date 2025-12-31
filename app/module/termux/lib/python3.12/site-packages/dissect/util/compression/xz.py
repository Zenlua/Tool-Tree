import io
from binascii import crc32
from typing import BinaryIO

from dissect.util.stream import OverlayStream

HEADER_FOOTER_SIZE = 12
CRC_SIZE = 4


def repair_checksum(fh: BinaryIO) -> BinaryIO:
    """Repair CRC32 checksums for all headers in an XZ stream.

    FortiOS XZ files have (on purpose) corrupt streams which they read using a modified ``xz`` binary.
    The only thing changed are the CRC32 checksums, so partially parse the XZ file and fix all of them.

    References:
        - https://tukaani.org/xz/xz-file-format-1.1.0.txt
        - https://github.com/Rogdham/python-xz

    Args:
        fh: A file-like object of an LZMA stream to repair.
    """
    file_size = fh.seek(0, io.SEEK_END)
    repaired = OverlayStream(fh, file_size)
    fh.seek(0)

    header = fh.read(HEADER_FOOTER_SIZE)
    # Check header magic
    magic = b"\xfd7zXZ\x00"
    if header[: len(magic)] != magic:
        raise ValueError("Not an XZ file")

    # Add correct header CRC32
    repaired.add(fh.tell() - CRC_SIZE, _crc32(header[len(magic) : HEADER_FOOTER_SIZE - CRC_SIZE]))

    footer_offset = fh.seek(-HEADER_FOOTER_SIZE, io.SEEK_END)
    footer = fh.read(HEADER_FOOTER_SIZE)

    # Check footer magic
    footer_magic = b"YZ"
    if footer[HEADER_FOOTER_SIZE - len(footer_magic) : HEADER_FOOTER_SIZE] != footer_magic:
        raise ValueError("Not an XZ file")

    # Add correct footer CRC32
    repaired.add(footer_offset, _crc32(footer[CRC_SIZE : HEADER_FOOTER_SIZE - len(footer_magic)]))

    backward_size = (int.from_bytes(footer[4:8], "little") + 1) * 4
    fh.seek(-HEADER_FOOTER_SIZE - backward_size, io.SEEK_END)
    index = fh.read(backward_size)

    # Add correct index CRC32
    repaired.add(fh.tell() - CRC_SIZE, _crc32(index[:-CRC_SIZE]))

    # Parse the index
    isize, num_records = _mbi(index[1:])
    index = index[1 + isize : -4]
    records = []
    for _ in range(num_records):
        if not index:
            raise ValueError("Missing index size")

        isize, unpadded_size = _mbi(index)
        if not unpadded_size:
            raise ValueError("Missing index record unpadded size")

        index = index[isize:]
        if not index:
            raise ValueError("Missing index size")

        isize, uncompressed_size = _mbi(index)
        if not uncompressed_size:
            raise ValueError("Missing index record uncompressed size")

        index = index[isize:]
        records.append((unpadded_size, uncompressed_size))

    block_start = file_size - HEADER_FOOTER_SIZE - backward_size
    blocks_len = sum((unpadded_size + 3) & ~3 for unpadded_size, _ in records)
    block_start -= blocks_len

    # Iterate over all blocks and add the correct block header CRC32
    for unpadded_size, _ in records:
        fh.seek(block_start)

        block_header = fh.read(1)
        block_header_size = (block_header[0] + 1) * 4
        block_header += fh.read(block_header_size - 1)
        repaired.add(fh.tell() - CRC_SIZE, _crc32(block_header[:-CRC_SIZE]))

        block_start += (unpadded_size + 3) & ~3

    return repaired


def _mbi(data: bytes) -> tuple[int, int]:
    """Decode a multibyte integer.

    The encoding is similar to most other "varint" encodings. For each byte, the 7 least significant bits are used for
    the integer value. The most significant bit is used to indicate if the integer continues in the next byte.
    Bytes are ordered in little endian byte order, meaning the least significant byte comes first.
    """
    value = 0
    for size, byte in enumerate(data):
        value |= (byte & 0x7F) << (size * 7)
        if not byte & 0x80:
            return size + 1, value
    raise ValueError("Invalid mbi")


def _crc32(data: bytes) -> bytes:
    return int.to_bytes(crc32(data), CRC_SIZE, "little")
