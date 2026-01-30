#!/data/data/com.tool.tree/files/home/termux/bin/python

import os
import sys

formats = ([b'PK', "zip"], [b'\x10\x20\xF5\xF2', 'f2fs', 1024],
           [b'\x53\xef', 'ext', 1080], [b'\x3a\xff\x26\xed', "sparse"],
           [b'\x67\x44\x6c\x61', 'super', 4096], [b"AVB0", "vbmeta"],
           [b'\xe2\xe1\xf5\xe0', "erofs", 1024], [b"CrAU", "payload"],
           [b'\xd7\xb7\xab\x1e', "dtbo"], [b'\xd0\x0d\xfe\xed', "dtb"],
           [b"MZ", "exe"], [b".ELF", 'elf'], [b"ANDROID!", "boot"],
           [b"VNDRBOOT", "vendor_boot"], [b'\x28\xb5\x2f\xfd', 'zstd'],
           [b"sqsh", "squashfs"], [b'hsqs', 'squashfs'], [b"NTPI", 'NTPI'],
           [b'\xfa\xff\xfa\xff', 'pac', 2116], [b'RKFW','rkfw'],[b'RKAF','rkaf'],
           [b'\x56\x19\xb5\x27', 'amlogic', 8], [b"-rom1fs-", 'romfs'],
           [b'(\x05\x00\x00$8"%', 'kdz'], [b"\x32\x96\x18\x74", 'dz'],
           [b'OPPOENCRYPT!', "ozip"], [b'7z', "7z"], [b'\x1f\x8b', "gzip"],
           [b'AVBf', "avb_foot"], [b'BZh', "bzip2"], [b'\x89PNG', 'png'],
           [b'CHROMEOS', 'chrome'], [b"LOGO!!!!", 'logo', 16384],
           [b'\x1f\x9e', "gzip"], [b'\x02\x21\x4c\x18', "lz4_legacy"],
           [b'\x03\x21\x4c\x18', 'lz4'], [b'\x04\x22\x4d\x18', 'lz4'],
           [b'\x1f\x8b\x08\x00\x00\x00\x00\x00\x02\x03', "zopfli"],
           [b'\xfd7zXZ', 'xz'], [b'\x7fELF', 'elf'], [b'\x5d\x00', 'lzma'],
           [b']\x00\x00\x00\x04\xff\xff\xff\xff\xff\xff\xff\xff', 'lzma'],
           [b'\x02!L\x18', 'lz4_lg'], [b'UBI#', "ubi"], [b"\x85\x19", "jffs2"])

def get_max_offset():
    return max((entry[2] if len(entry) == 3 else 0) + len(entry[0]) for entry in formats)

def gettype(file: str) -> str:
    if not os.path.exists(file):
        return "fne"
    if os.path.getsize(file) == 0:
        return "empty"
    ext = os.path.splitext(file)[1].lower().lstrip(".")
    if ext in ("dat", "br"):
        return ext

    max_offset = get_max_offset()
    try:
        with open(file, 'rb') as f:
            data = f.read(max_offset)
    except Exception:
        return "err"

    for f_ in formats:
        sig = f_[0]
        if len(f_) == 2:
            if data.startswith(sig):
                return f_[1]
        elif len(f_) == 3:
            offset = f_[2]
            if len(data) >= offset + len(sig) and data[offset:offset + len(sig)] == sig:
                return f_[1]
    # ext = os.path.splitext(file)[1].lower().lstrip(".")
    # if ext:
        # return ext
    return "unknown"

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python gettype.py <filepath>")
    else:
        print(gettype(sys.argv[1]))
