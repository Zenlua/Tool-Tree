#!/data/data/com.tool.tree/files/home/termux/bin/python
# -*- coding: utf-8 -*-

import os
import re
import sys
from typing import List, Tuple

ContextEntry = Tuple[str, List[str]]

fix_permission = {
    "/vendor/bin/hw/android.hardware.wifi@1.0": ["u:object_r:hal_wifi_default_exec:s0"]
}


def str_to_selinux(s: str) -> str:
    # Escape dấu . cho đúng định dạng regex file_contexts, giữ nguyên / và -
    escaped = re.escape(s).replace(r"\-", "-").replace(r"\/", "/")
    return escaped.replace(r"\@", "@")


def scan_context(file_path: str) -> List[ContextEntry]:
    """Đọc file gốc và GIỮ NGUYÊN thứ tự xuất hiện của từng dòng."""
    entries: List[ContextEntry] = []
    with open(file_path, "r", encoding="utf-8", errors="ignore") as fp:
        for line in fp:
            line_str = line.strip()
            if not line_str or line_str.startswith("#"):
                continue

            parts = line_str.split()
            if not parts:
                continue

            filepath, *other = parts
            entries.append((filepath, other))

    return entries


def scan_dir(folder: str) -> list:
    folder = os.path.abspath(folder)
    part_name = os.path.basename(folder.rstrip(os.sep)) or os.path.basename(folder)
    allfiles = ["/", f"/{part_name}/lost+found", f"/{part_name}", f"/{part_name}/"]

    for root, dirs, files in os.walk(folder, topdown=True):
        rel_root = os.path.relpath(root, folder)
        rel_root = "" if rel_root == "." else rel_root

        for dir_ in dirs:
            rel_path = os.path.join(rel_root, dir_).replace("\\", "/")
            allfiles.append(f"/{part_name}/{rel_path}".replace("//", "/"))

        for file_ in files:
            rel_path = os.path.join(rel_root, file_).replace("\\", "/")
            allfiles.append(f"/{part_name}/{rel_path}".replace("//", "/"))

    return sorted(set(allfiles), key=allfiles.index)


def _default_permission(entries: List[ContextEntry]) -> list:
    if entries:
        return entries[0][1]
    return ["u:object_r:system_file:s0"]


def find_insert_index(entries: List[ContextEntry], raw_path: str) -> Tuple[int, list]:
    """
    Tìm vị trí xuất hiện cuối cùng của thư mục cha trong file gốc
    để chèn item mới vào ngay bên cạnh/dưới nó. Đồng thời trả về permission kế thừa.
    """
    tmp_path = os.path.dirname(raw_path)

    while tmp_path and tmp_path != "/":
        tmp_selinux = str_to_selinux(tmp_path)
        last_idx = None
        found_perm = None

        for idx, (path, perm) in enumerate(entries):
            # Khớp với đường dẫn gốc hoặc dạng đã escape
            if path == tmp_path or path == tmp_selinux or path.startswith(tmp_selinux + "/"):
                last_idx = idx
                found_perm = perm

        if last_idx is not None:
            return last_idx + 1, found_perm

        tmp_path = os.path.dirname(tmp_path)

    # Nếu không tìm thấy cha, chèn vào cuối file
    return len(entries), None


def context_patch(fs_entries: List[ContextEntry], filename: list) -> Tuple[List[ContextEntry], int]:
    entries = list(fs_entries)
    
    # Tập hợp các đường dẫn đã tồn tại để tránh chèn trùng
    existing_paths = {path for path, _ in entries}
    permission_d = _default_permission(entries)

    seen_new = set()
    new_add = 0

    for i in filename:
        if not i:
            continue

        raw_path = i
        if not raw_path.isprintable():
            raw_path = "".join(c if c.isprintable() else "*" for c in raw_path)

        selinux_path = str_to_selinux(raw_path)

        # Bỏ qua nếu dòng đã tồn tại trong file gốc hoặc đã được thêm trước đó
        if raw_path in existing_paths or selinux_path in existing_paths or selinux_path in seen_new:
            continue

        # Xử lý quy tắc đè thủ công trong fix_permission
        if raw_path in fix_permission:
            permission = fix_permission[raw_path]
            insert_at, _ = find_insert_index(entries, raw_path)
        elif selinux_path in fix_permission:
            permission = fix_permission[selinux_path]
            insert_at, _ = find_insert_index(entries, raw_path)
        else:
            # Tìm vị trí thích hợp (sau thư mục cha) và lấy permission kế thừa
            insert_at, parent_perm = find_insert_index(entries, raw_path)
            permission = parent_perm if parent_perm else permission_d

        # Chèn trực tiếp vào vị trí tìm được mà KHÔNG làm xáo trộn các dòng khác
        entries.insert(insert_at, (selinux_path, permission))
        seen_new.add(selinux_path)
        existing_paths.add(selinux_path)
        new_add += 1

        print(f"ADD [{selinux_path} {' '.join(permission)}] at index {insert_at}")

    return entries, new_add


def main(dir_path: str, fs_config: str) -> None:
    origin_entries = scan_context(os.path.abspath(fs_config))
    allfiles = scan_dir(os.path.abspath(dir_path))
    
    new_entries, new_add = context_patch(origin_entries, allfiles)

    with open(fs_config, "w", encoding="utf-8", newline="\n") as f:
        for path, perm in new_entries:
            f.write(f"{path} {' '.join(perm)}\n")

    print(f"Load origin {len(origin_entries)} entries")
    print(f"Detect total {len(allfiles)} entries")
    print(f"Add {new_add} entries")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Insufficient parameters")
        sys.exit(1)

    if not os.path.exists(sys.argv[1]) or not os.path.exists(sys.argv[2]):
        print("File or directory does not exist")
        sys.exit(1)

    main(sys.argv[1], sys.argv[2])