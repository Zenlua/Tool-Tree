#!/data/data/com.tool.tree/files/home/termux/bin/python

# -*- coding: utf-8 -*-
import os
import sys
from re import sub

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
    r"/lost\+found": "u:object_r:rootfs:s0"
}


def scan_context(file) -> dict:
    context = {}
    with open(file, "r", encoding='utf-8') as file_:
        for i in file_.readlines():
            line = i.strip().replace('\\', '')
            if not line:
                continue  # Bỏ qua dòng trống
            if line.startswith('#'):
                continue
            parts = line.split()
            if not parts:
                continue  # An toàn thêm 1 lần
            filepath, *other = parts
            context[filepath] = other
    return context

def scan_dir(folder) -> list:
    part_name = os.path.basename(folder)
    allfiles = ['/', '/lost+found', f'/{part_name}/lost+found', f'/{part_name}', f'/{part_name}/']
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


def context_patch(fs_file, filename) -> dict:  # Receive two dictionaries for comparison
    new_fs = {}
    permission_d = fs_file.get(list(fs_file)[0])
    if not permission_d:
        permission_d = ['u:object_r:system_file:s0']
    for i in filename:
        if fs_file.get(i):
            new_fs[sub(r'([^-_/a-zA-Z0-9])', r'\\\1', i)] = fs_file[i]
        else:
            permission = permission_d
            if i:
                if not i.isprintable():
                    tmp = ''
                    for c in i:
                        tmp += c if c.isprintable() else '*'
                    i = tmp
                if i in fix_permission.keys():
                    permission = fix_permission[i]
                else:
                    # Tìm context từ thư mục gần nhất có trong fs_file
                    tmp_path = os.path.dirname(i)
                    while tmp_path != "/" and tmp_path:
                        if tmp_path in fs_file:
                            permission = fs_file[tmp_path]
                            break
                        tmp_path = os.path.dirname(tmp_path)
            print(f"ADD [{i}:{permission}]")
            new_fs[sub(r'([^-_/a-zA-Z0-9])', r'\\\1', i)] = permission
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
    