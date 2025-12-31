from __future__ import annotations

import io
import struct
from typing import BinaryIO


def read_sid(fh: BinaryIO | bytes, endian: str = "<", swap_last: bool = False) -> str:
    """Read a Windows SID from bytes.

    Normally we'd do this with cstruct, but do it with just struct to keep dissect.util dependency-free.
    On the upside, this also improves performance!

    This is equivalent to the following structure::

        typedef struct _SID {
            BYTE        Revision;
            BYTE        SubAuthorityCount;
            CHAR        IdentifierAuthority[6];
            DWORD       SubAuthority[SubAuthorityCount];
        } SID;

    Args:
        fh: A file-like object or bytes object to read the SID from.
        endian: Optional endianness for reading the sub authorities.
        swap_list: Optional flag for swapping the endianess of the _last_ sub authority entry.
    """
    if isinstance(fh, bytes):
        fh = io.BytesIO(fh)

    if len(buf := fh.read(8)) != 8:
        return ""

    revision = buf[0]
    sub_authority_count = buf[1]
    authority = int.from_bytes(buf[2:], "big")

    sub_authority_buf = bytearray(fh.read(sub_authority_count * 4))
    if sub_authority_count and swap_last:
        sub_authority_buf[-4:] = sub_authority_buf[-4:][::-1]

    sub_authorities = struct.unpack(f"{endian}{sub_authority_count}I", sub_authority_buf)

    sid_elements = [
        "S",
        f"{revision}",
        f"{authority}",
    ]
    sid_elements.extend(map(str, sub_authorities))
    return "-".join(sid_elements)
