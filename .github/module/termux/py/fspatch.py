#!/data/data/com.tool.tree/files/home/termux/bin/python
# -*- coding: utf-8 -*-

import os
import sys

def scanfs(file) -> tuple:
    """Đọc file cấu hình gốc, bỏ qua comment/dòng trống và giữ nguyên thứ tự dòng."""
    filesystem_config = {}
    original_order = []
    
    with open(file, "r", encoding="utf-8") as file_:
        for line_num, i in enumerate(file_, 1):
            line = i.strip()
            if not line or line.startswith('#'):
                continue
            parts = line.split()
            if not parts:
                continue
            filepath, *other = parts
            filesystem_config[filepath] = other
            original_order.append(filepath)
            if len(other) > 4:
                print(f"[Warn] Current {line_num}: {filepath} there are too many fields ({len(other)}).")
                
    return filesystem_config, original_order

def scan_dir(folder):
    """
    Quét thư mục thực tế cấu trúc POSIX Android.
    Hỗ trợ Dynamic Partitions bằng cách ép đường dẫn gốc về chuẩn Android.
    """
    base = os.path.basename(os.path.normpath(folder))
            
    yield base
    yield '/'
    yield f'{base}/lost+found'
    
    for root, dirs, files in os.walk(folder):
        rel_root = os.path.relpath(root, folder)
        rel_root = '' if rel_root == '.' else rel_root
        for d in dirs:
            path = os.path.join(base, rel_root, d)
            yield path.replace('\\', '/')
        for f in files:
            path = os.path.join(base, rel_root, f)
            yield path.replace('\\', '/')

def islink(file) -> str or None:
    """Kiểm tra và trả về đường dẫn gốc của Symlink chuẩn xác trên cả Win và Linux."""
    if os.name == 'nt':
        if os.path.exists(file) and not os.path.isdir(file):
            try:
                with open(file, 'rb') as f:
                    content = f.read()
                    # Kiểm tra header của file symlink kiểu cũ (Cygwin/Msys2/Giả lập)
                    if content.startswith(b'!<symlink>\xff\xfe'):
                        link_bytes = content[12:]
                        return link_bytes.decode("utf-16", errors="ignore").replace('\x00', '').strip()
            except IOError:
                pass
    elif os.name == 'posix':
        if os.path.islink(file):
            return os.readlink(file)
    return None

def fs_patch(fs_file, dir_path) -> tuple:
    """Đối chiếu cấu hình file và vá các mục còn thiếu."""
    new_fs = {}
    added_keys = []
    new_add = 0
    r_fs = set()
    
    print("FsPatcher: Load origin %d entries" % len(fs_file.keys()))
    
    # Đã sửa: Bỏ dấu gạch chéo ở đầu để khớp chính xác với kết quả sinh ra từ scan_dir
    special_binaries = {
        "bin/su", "xbin/su", "disable_selinux.sh", "daemon", "ext/.su", 
        "install-recovery", 'installed_su', 'bin/rw-system.sh', 'bin/getSPL'
    }

    # Lấy thư mục cha trực tiếp của dir_path một cách an toàn
    parent_dir = os.path.dirname(os.path.abspath(dir_path))

    for i in scan_dir(os.path.abspath(dir_path)):
        actual_i = i
        if not i.isprintable():
            actual_i = ''.join(c if c.isprintable() else '*' for c in i)

        if fs_file.get(actual_i):
            new_fs[actual_i] = fs_file[actual_i]
            continue

        if actual_i in r_fs:
            continue

        # Sửa lỗi tính toán sai filepath tương đối khi gộp hệ thống
        filepath = os.path.join(parent_dir, actual_i.replace('/', os.sep))

        is_dir = os.path.isdir(filepath)
        exists = os.path.exists(filepath) or os.path.islink(filepath)
        link_target = islink(filepath)

        # Thiết lập GID phân vùng nhị phân
        is_bin_path = any(x in actual_i for x in ["system/bin", "system/xbin", "vendor/bin"])
        gid = '2000' if is_bin_path else '0'
        uid = '0'

        if is_dir:
            config = [uid, gid, '0755']
        elif not exists:
            config = ['0', '0', '0755']
        elif link_target is not None:
            if ("bin/" in actual_i) or ("xbin/" in actual_i):
                mode = '0755'
            elif ".sh" in actual_i:
                mode = "0750"
            else:
                mode = "0644"
            config = [uid, gid, mode, link_target]
        elif ("bin/" in actual_i) or ("xbin/" in actual_i):
            if ".sh" in actual_i:
                mode = "0750"
            elif any(s in actual_i for s in special_binaries):
                mode = "0755"
            else:
                mode = '0755'
            config = [uid, gid, mode]
        else:
            config = [uid, '0', '0644']

        print(f'Add [{actual_i} {config}]')
        r_fs.add(actual_i)
        new_add += 1
        new_fs[actual_i] = config
        added_keys.append(actual_i)

    return new_fs, added_keys, new_add

def main(dir_path, fs_config) -> None:
    origin_fs, orig_order = scanfs(os.path.abspath(fs_config))
    new_fs, added_keys, new_add = fs_patch(origin_fs, dir_path)
    
    with open(fs_config, "w", encoding='utf-8', newline='\n') as f:
        # Bước 1: Ghi lại các cấu hình cũ theo đúng thứ tự ưu tiên ban đầu
        for key in orig_order:
            if key in new_fs:
                f.write(f"{key} {' '.join(new_fs[key])}\n")
                del new_fs[key]
                
        # Bước 2: Sắp xếp và ghi các cấu hình mới thêm vào cuối file
        for key in sorted(added_keys):
            if key in new_fs:
                f.write(f"{key} {' '.join(new_fs[key])}\n")
                
    print('FsPatcher: Add %d entries thành công!' % new_add)

def usage():
    print("""
    FsPatcher: FsConfig Patching Tool
    Usage： ./FsPatcher [Folders] [FsConfig]
          """)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("FsPatcher: Insufficient parameters")
        usage()
    elif os.path.isfile(os.path.abspath(sys.argv[2])) and os.path.isdir(os.path.abspath(sys.argv[1])):
        main(sys.argv[1], sys.argv[2])
    else:
        usage()