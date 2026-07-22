#!/data/data/com.tool.tree/files/home/termux/bin/python
# -*- coding: utf-8 -*-

import os
import sys
from typing import List, Optional, Tuple

FsEntry = Tuple[str, List[str]]


def scanfs(fs_path: str) -> List[FsEntry]:
    entries: List[FsEntry] = []
    with open(fs_path, "r", encoding="utf-8", errors="ignore") as fp:
        for line_num, raw_line in enumerate(fp, 1):
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue

            parts = line.split()
            if not parts:
                continue

            filepath, *other = parts
            entries.append((filepath, other))

            if len(other) > 4:
                print(
                    f"[Warn] Line {line_num}: {filepath} has too many fields ({len(other)})."
                )

    return entries


def scan_dir(folder: str):
    folder = os.path.abspath(folder)
    base = os.path.basename(folder)
    yield base
    yield "/"
    yield f"{base}/lost+found"

    for root, dirs, files in os.walk(folder):
        rel_root = os.path.relpath(root, folder)
        rel_root = "" if rel_root == "." else rel_root

        for d in dirs:
            path = os.path.join(base, rel_root, d)
            yield path.replace("\\", "/")

        for f in files:
            path = os.path.join(base, rel_root, f)
            yield path.replace("\\", "/")


def islink(file_path: str) -> Optional[str]:
    if os.path.islink(file_path):
        try:
            return os.readlink(file_path)
        except OSError:
            return None
    return None


def make_config(i: str, filepath: str) -> List[str]:
    path_norm = i.replace("\\", "/")
    
    # 1. Thư mục
    if os.path.isdir(filepath):
        uid = "0"
        gid = (
            "2000"
            if path_norm.startswith(("system/bin/", "system/xbin/", "vendor/bin/"))
            else "0"
        )
        mode = "0755"
        return [uid, gid, mode]

    # 2. File không tồn tại thực tế
    if not os.path.exists(filepath):
        return ["0", "0", "0755"]

    # 3. Liên kết mềm (Symlink)
    link = islink(filepath)
    if link:
        uid = "0"
        gid = (
            "2000"
            if path_norm.startswith(("system/bin/", "system/xbin/", "vendor/bin/"))
            else "0"
        )

        if "/bin/" in path_norm or "/xbin/" in path_norm:
            mode = "0755"
        elif path_norm.endswith(".sh"):
            mode = "0750"
        else:
            mode = "0644"

        return [uid, gid, mode, link]

    # 4. File thực thi trong bin/xbin
    if "/bin/" in path_norm or "/xbin/" in path_norm:
        uid = "0"
        gid = (
            "2000"
            if path_norm.startswith(("system/bin/", "system/xbin/", "vendor/bin/"))
            else "0"
        )
        mode = "0750" if path_norm.endswith(".sh") else "0755"
        return [uid, gid, mode]

    # 5. File thông thường
    return ["0", "0", "0644"]


def find_insert_index(entries: List[FsEntry], new_path: str) -> int:
    norm = new_path.replace("\\", "/").strip("/")
    parts = [p for p in norm.split("/") if p]
    if len(parts) <= 1:
        return len(entries)

    for end in range(len(parts) - 1, 0, -1):
        prefix = "/".join(parts[:end])
        prefix_slash = prefix + "/"

        last_idx = None
        for idx, (path, _) in enumerate(entries):
            p = path.replace("\\", "/").strip("/")
            if p == prefix or p.startswith(prefix_slash):
                last_idx = idx

        if last_idx is not None:
            return last_idx + 1

    return len(entries)


def fs_patch(fs_entries: List[FsEntry], dir_path: str) -> Tuple[List[FsEntry], int]:
    entries = list(fs_entries)
    existing = {k for k, _ in entries}
    seen_new = set()
    new_add = 0
    print("FsPatcher: Load origin %d entries" % len(entries))

    target_dir = os.path.abspath(dir_path)
    base = os.path.basename(target_dir)

    for i in scan_dir(target_dir):
        if not i.isprintable():
            i = "".join(c if c.isprintable() else "*" for c in i)

        if i == base and i not in existing:
            continue

        if i in existing or i in seen_new:
            continue

        # Tính đường dẫn thực tế chính xác trên ổ đĩa
        rel_path = i[len(base) + 1:] if i.startswith(base + "/") else i
        filepath = os.path.abspath(os.path.join(target_dir, rel_path))

        config = make_config(i, filepath)

        insert_at = find_insert_index(entries, i)
        entries.insert(insert_at, (i, config))
        seen_new.add(i)
        new_add += 1

        print(f"Add [{i} {' '.join(config)}] at {insert_at}")

    return entries, new_add


def main(dir_path: str, fs_config: str) -> None:
    fs_entries = scanfs(os.path.abspath(fs_config))
    new_entries, new_add = fs_patch(fs_entries, dir_path)
    with open(fs_config, "w", encoding="utf-8", newline="\n") as f:
        for key, value in new_entries:
            f.write(key + " " + " ".join(value) + "\n")

    print(f"FsPatcher: Add {new_add} entries")


def usage() -> None:
    print(
        """
FsPatcher: FsConfig Patching Tool
Usage: ./FsPatcher [Folders] [FsConfig]
"""
    )


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("FsPatcher: Insufficient parameters")
        usage()
    elif os.path.isfile(os.path.abspath(sys.argv[2])) and os.path.isdir(
        os.path.abspath(sys.argv[1])
    ):
        main(sys.argv[1], sys.argv[2])
    else:
        usage()