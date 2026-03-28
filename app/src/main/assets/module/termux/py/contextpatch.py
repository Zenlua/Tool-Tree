#!/data/data/com.tool.tree/files/home/termux/bin/python

# -*- coding: utf-8 -*-
import os
import sys
from re import sub
from re import escape

fix_permission = {
    "system/app/*/.apk": "u:object_r:system_file:s0",
    "data-app/.apk": "u:object_r:system_file:s0",
    "android.hardware.wifi": "u:object_r:hal_wifi_default_exec:s0",
    "bin/idmap": "u:object_r:idmap_exec:s0",
    "bin/fsck": "u:object_r:fsck_exec:s0",
    "bin/e2fsck": "u:object_r:fsck_exec:s0",
    "bin/logcat": "u:object_r:logcat_exec:s0",
    "system/bin": "u:object_r:system_file:s0",
    "/system/bin/init": "u:object_r:init_exec:s0",
    "/vendor/bin/hw/android.hardware.wifi@1.0": "u:object_r:hal_wifi_default_exec:s0"
}

def str_to_selinux(s: str) -> str:
    return escape(s).replace('\\-', '-')

def scan_context(file) -> dict:
    context = {}
    with open(file, "r", encoding='utf-8') as file_:
        for i in file_.readlines():
            line = i.strip().replace('\\', '')
            if not line:
                continue
            if line.startswith('#'):
                continue
            parts = line.split()
            if not parts:
                continue
            filepath, *other = parts
            filepath = filepath.replace(r'\@', '@')
            context[filepath] = other
    return context

def scan_dir(folder) -> list:
    part_name = os.path.basename(folder)
    allfiles = ['/', f'/{part_name}/lost+found', f'/{part_name}', f'/{part_name}/']
    for root, dirs, files in os.walk(folder, topdown=True):
        for dir_ in dirs:
            if os.name == 'nt':
                allfiles.append(os.path.join(root, dir_).replace(folder, '/' + part_name).replace('\\', '/'))
            elif os.name == 'posix':
                allfiles.append(os.path.join(root, dir_).replace(folder, '/' + part_name))
        for file in files:
            if os.name == 'nt':
                allfiles.append(os.path.join(root, file).replace(folder, '/' + part_name).replace('\\', '/'))
            elif os.name == 'posix':
                allfiles.append(os.path.join(root, file).replace(folder, '/' + part_name))
    return sorted(set(allfiles), key=allfiles.index)


def context_patch(fs_file, filename) -> dict:
    new_fs = {}
    # Giữ logic cũ: lấy permission mặc định từ entry đầu
    permission_d = fs_file.get(next(iter(fs_file)))
    if not permission_d:
        permission_d = ['u:object_r:system_file:s0']
    for i in filename:
        selinux_path = str_to_selinux(i)
        if fs_file.get(i):
            new_fs[selinux_path] = fs_file[i]
            continue
        permission = permission_d
        if i:
            # giữ logic cũ: thay ký tự không printable bằng '*'
            if not i.isprintable():
                i = ''.join(c if c.isprintable() else '*' for c in i)
            # giữ logic cũ: fix_permission
            if i in fix_permission:
                permission = fix_permission[i]
            else:
                # giữ nguyên logic tìm thư mục cha
                tmp_path = os.path.dirname(i)
                while tmp_path and tmp_path != "/":
                    if tmp_path in fs_file:
                        permission = fs_file[tmp_path]
                        break
                    tmp_path = os.path.dirname(tmp_path)
        print(f"ADD [{i}:{permission}]")
        new_fs[selinux_path] = permission
    return new_fs


def main(dir_path, fs_config) -> None:
    origin = scan_context(os.path.abspath(fs_config))
    allfiles = scan_dir(os.path.abspath(dir_path))
    new_fs = context_patch(origin, allfiles)
    with open(fs_config, "w+", encoding='utf-8', newline='\n') as f:
        f.writelines([i + " " + " ".join(new_fs[i]) + "\n" for i in sorted(new_fs.keys())])
    print("Load origin %d" % (len(origin.keys())) + " entries")
    print("Detect total %d" % (len(allfiles)) + " entries")
    print('Add %d' % (len(new_fs.keys()) - len(origin.keys())) + " entries")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Insufficient parameters")
        sys.exit(1)
    if not os.path.exists(sys.argv[1]) or not os.path.exists(sys.argv[2]):
        print("File does not exist")
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])
    