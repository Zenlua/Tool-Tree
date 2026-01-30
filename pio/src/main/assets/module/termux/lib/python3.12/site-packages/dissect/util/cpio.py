from __future__ import annotations

import stat
import struct
import tarfile
from tarfile import InvalidHeaderError
from typing import BinaryIO

FORMAT_CPIO_BIN = 10
FORMAT_CPIO_ODC = 11
FORMAT_CPIO_NEWC = 12
FORMAT_CPIO_CRC = 13
FORMAT_CPIO_HPBIN = 16
FORMAT_CPIO_HPODC = 17
FORMAT_CPIO_UNKNOWN = 18

CPIO_MAGIC_OLD = 0o070707
CPIO_MAGIC_NEW = 0o070701
CPIO_MAGIC_CRC = 0o070702

TYPE_MAP = {
    stat.S_IFREG: tarfile.REGTYPE,
    stat.S_IFDIR: tarfile.DIRTYPE,
    stat.S_IFIFO: tarfile.FIFOTYPE,
    stat.S_IFLNK: tarfile.SYMTYPE,
    stat.S_IFCHR: tarfile.CHRTYPE,
    stat.S_IFBLK: tarfile.BLKTYPE,
}


class CpioInfo(tarfile.TarInfo):
    """Custom ``TarInfo`` implementation for reading cpio archives.

    Examples::

        tarfile.open(..., tarinfo=CpioInfo)
        # or
        tarfile.TarFile(..., tarinfo=CpioInfo)

    """

    @classmethod
    def fromtarfile(cls, tarfile: tarfile.TarFile) -> CpioInfo:
        if tarfile.format not in (
            FORMAT_CPIO_BIN,
            FORMAT_CPIO_ODC,
            FORMAT_CPIO_NEWC,
            FORMAT_CPIO_CRC,
            FORMAT_CPIO_HPBIN,
            FORMAT_CPIO_HPODC,
        ):
            tarfile.format = detect_header(tarfile.fileobj)

        if tarfile.format in (FORMAT_CPIO_BIN, FORMAT_CPIO_HPBIN):
            buf = tarfile.fileobj.read(26)
        elif tarfile.format in (FORMAT_CPIO_ODC, FORMAT_CPIO_HPODC):
            buf = tarfile.fileobj.read(76)
        elif tarfile.format in (FORMAT_CPIO_NEWC, FORMAT_CPIO_CRC):
            buf = tarfile.fileobj.read(110)
        else:
            raise InvalidHeaderError("Unknown cpio type")

        obj = cls.frombuf(buf, tarfile.format, tarfile.encoding, tarfile.errors)
        obj.format = tarfile.format
        obj.offset = tarfile.fileobj.tell() - len(buf)
        return obj._proc_member(tarfile)

    @classmethod
    def frombuf(cls, buf: bytes, format: int, encoding: str, errors: str) -> CpioInfo:
        if format in (FORMAT_CPIO_BIN, FORMAT_CPIO_ODC, FORMAT_CPIO_HPBIN, FORMAT_CPIO_HPODC):
            obj = cls._old_frombuf(buf, format)
        elif format in (FORMAT_CPIO_NEWC, FORMAT_CPIO_CRC):
            obj = cls._new_frombuf(buf, format)

        # Common postprocessing
        ftype = stat.S_IFMT(obj._mode)
        obj.type = TYPE_MAP.get(ftype, ftype)
        obj.mode = stat.S_IMODE(obj._mode)

        return obj

    @classmethod
    def _old_frombuf(cls, buf: bytes, format: int) -> CpioInfo:
        if format in (FORMAT_CPIO_BIN, FORMAT_CPIO_HPBIN):
            values = list(struct.unpack("<13H", buf))
            if values[0] == _swap16(CPIO_MAGIC_OLD):
                values = [_swap16(v) for v in values]

            mtime = (values.pop(8) << 16) | values.pop(8)
            size = (values.pop(9) << 16) | values.pop(9)
            values.insert(8, mtime)
            values.append(size)
        else:
            values = [int(v, 8) for v in struct.unpack("<6s6s6s6s6s6s6s6s11s6s11s", buf)]

        if values[0] != CPIO_MAGIC_OLD:
            raise InvalidHeaderError(f"Invalid (old) ASCII/binary cpio header magic: {oct(values[0])}")

        obj = cls()
        obj.devmajor = values[1] >> 8
        obj.devminor = values[1] & 0xFF
        obj._mode = values[3]
        obj.uid = values[4]
        obj.gid = values[5]
        obj.mtime = values[8]
        obj.size = values[10]

        # Extra fields
        obj.magic = values[0]
        obj.ino = values[2]
        obj.nlink = values[6]
        obj.rdevmajor = values[7] >> 8
        obj.rdevminor = values[7] & 0xFF
        obj.namesize = values[9]

        # This is a specific case for HP/UX cpio archives, which I'll let this comment from the original source explain:
        # HP/UX cpio creates archives that look just like ordinary archives,
        # but for devices it sets major = 0, minor = 1, and puts the
        # actual major/minor number in the filesize field.  See if this
        # is an HP/UX cpio archive, and if so fix it.  We have to do this
        # here because process_copy_in() assumes filesize is always 0
        # for devices.
        if (
            stat.S_IFMT(obj.mode) in (stat.S_IFCHR, stat.S_IFBLK, stat.S_IFSOCK, stat.S_IFIFO)
            and obj.size != 0
            and obj.rdevmajor == 0
            and obj.rdevminor == 1
        ):
            obj.rdevmajor = (obj.size >> 8) & 0xFF
            obj.rdevminor = obj.size & 0xFF
            obj.size = 0

        return obj

    @classmethod
    def _new_frombuf(cls, buf: bytes, format: int) -> CpioInfo:
        values = struct.unpack("<6s8s8s8s8s8s8s8s8s8s8s8s8s8s", buf)
        values = [int(values[0], 8)] + [int(v, 16) for v in values[1:]]
        if values[0] not in (CPIO_MAGIC_NEW, CPIO_MAGIC_CRC):
            raise InvalidHeaderError(f"Invalid (new) ASCII cpio header magic: {oct(values[0])}")

        obj = cls()
        obj._mode = values[2]
        obj.uid = values[3]
        obj.gid = values[4]
        obj.mtime = values[6]
        obj.size = values[7]
        obj.devmajor = values[8]
        obj.devminor = values[9]
        obj.chksum = values[13]

        # Extra fields
        obj.magic = values[0]
        obj.ino = values[1]
        obj.nlink = values[5]
        obj.rdevmajor = values[10]
        obj.rdevminor = values[11]
        obj.namesize = values[12]

        return obj

    def _proc_member(self, tarfile: tarfile.TarFile) -> CpioInfo | None:
        self.name = tarfile.fileobj.read(self.namesize - 1).decode(tarfile.encoding, tarfile.errors)
        if self.name == "TRAILER!!!":
            # The last entry in a cpio file has the special name ``TRAILER!!!``, indicating the end of the archive
            return None

        offset = tarfile.fileobj.tell() + 1
        self.offset_data = self._round_word(offset)
        tarfile.offset = self._round_word(self.offset_data + self.size)

        if self.issym():
            tarfile.fileobj.seek(self.offset_data)
            self.linkname = tarfile.fileobj.read(self.size).decode(tarfile.encoding, tarfile.errors)
            self.size = 0

        return self

    def _round_word(self, offset: int) -> int:
        if self.format in (FORMAT_CPIO_BIN, FORMAT_CPIO_HPBIN):
            return (offset + 1) & ~0x01

        if self.format in (FORMAT_CPIO_NEWC, FORMAT_CPIO_CRC):
            return (offset + 3) & ~0x03

        return offset

    def issocket(self) -> bool:
        """Return True if it is a socket."""
        return self.type == stat.S_IFSOCK


def detect_header(fh: BinaryIO) -> int:
    """Detect a cpio format on a file-like object."""
    offset = fh.tell()
    magic = fh.read(6)
    fh.seek(offset)

    result = FORMAT_CPIO_UNKNOWN
    if magic == b"070701":
        result = FORMAT_CPIO_NEWC
    elif magic == b"070707":
        result = FORMAT_CPIO_ODC
    elif magic == b"070702":
        result = FORMAT_CPIO_CRC
    elif magic[:2] in (b"\x71\xc7", b"\xc7\x71"):
        # 0o070707 in little and big endian
        result = FORMAT_CPIO_BIN

    return result


def _swap16(value: int) -> int:
    return ((value & 0xFF) << 8) | (value >> 8)


def CpioFile(*args, **kwargs) -> tarfile.TarFile:  # noqa: N802
    """Utility wrapper around ``tarfile.TarFile`` to easily open cpio archives."""
    kwargs.setdefault("format", FORMAT_CPIO_UNKNOWN)
    return tarfile.TarFile(*args, **kwargs, tarinfo=CpioInfo)


def open(*args, **kwargs) -> tarfile.TarFile:
    """Utility wrapper around ``tarfile.open`` to easily open cpio archives."""
    kwargs.setdefault("format", FORMAT_CPIO_UNKNOWN)
    return tarfile.open(*args, **kwargs, tarinfo=CpioInfo)
