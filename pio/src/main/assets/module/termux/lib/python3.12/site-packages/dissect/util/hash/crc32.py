import zlib
from functools import lru_cache


@lru_cache(maxsize=32)
def _table(polynomial: int) -> tuple[int, ...]:
    """Generate a CRC32 table for a given (reversed) polynomial.

    Args:
        polynomial: The (reversed) polynomial to use for the CRC32 calculation.
    """
    table = []
    for i in range(256):
        crc = i
        for _ in range(8):
            if (crc & 1) != 0:
                crc = (crc >> 1) ^ polynomial
            else:
                crc >>= 1
            crc &= 0xFFFFFFFF
        table.append(crc)
    return tuple(table)


def update(crc: int, data: bytes, polynomial: int = 0xEDB88320, table: tuple[int, ...] | None = None) -> int:
    """Update CRC32 checksum with data.

    Args:
        crc: The initial value of the checksum.
        data: The data to update the checksum with.
        polynomial: The (reversed) polynomial to use for the CRC32 calculation. Default is 0xEDB88320 (crc32b).
        table: Optional precomputed CRC32 table. If None, a table will be generated using the given polynomial.
    """
    if polynomial == 0xEDB88320 and table is None:
        return zlib.crc32(data, crc)

    if table is None:
        table = _table(polynomial)

    crc = crc ^ 0xFFFFFFFF
    for b in data:
        crc = table[(crc ^ b) & 0xFF] ^ ((crc >> 8) & 0xFFFFFFFF)
    return crc ^ 0xFFFFFFFF


def crc32(data: bytes, value: int = 0, polynomial: int = 0xEDB88320, table: tuple[int, ...] | None = None) -> int:
    """Calculate CRC32 checksum of some data, with an optional initial value and polynomial.

    Args:
        data: The data to calculate the checksum of.
        value: The initial value of the checksum. Default is 0.
        polynomial: The (reversed) polynomial to use for the CRC32 calculation. Default is 0xEDB88320 (crc32b).
        table: Optional precomputed CRC32 table. If None, a table will be generated using the given polynomial.
    """
    return update(value, data, polynomial, table) & 0xFFFFFFFF
