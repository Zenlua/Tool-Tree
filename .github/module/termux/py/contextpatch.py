#!/data/data/com.tool.tree/files/home/termux/bin/python
# -*- coding: utf-8 -*-

import os
import re
import sys

# Khai báo quy tắc gán context cứng (hỗ trợ cả đường dẫn tuyệt đối lẫn tên file/thư mục đặc biệt)
FIX_PERMISSIONS = {
    "lost+found": ["u:object_r:rootfs:s0"],
    "system/bin/init": ["u:object_r:init_exec:s0"],
    "system/bin/idmap": ["u:object_r:idmap_exec:s0"],
    "system/bin/fsck": ["u:object_r:fsck_exec:s0"],
    "system/bin/e2fsck": ["u:object_r:fsck_exec:s0"],
    "system/bin/logcat": ["u:object_r:logcat_exec:s0"],
    "vendor/bin/hw/android.hardware.wifi@1.0": ["u:object_r:hal_wifi_default_exec:s0"]
}

def escape_regex_path(path: str) -> str:
    """
    Escape các ký tự đặc biệt Regex (+, .) cho file_contexts của Android,
    giữ nguyên dấu / và @ để make_ext4fs đọc chuẩn.
    """
    # Escape dấu '+' chưa được escape trước đó
    path_escaped = re.sub(r'(?<!\\)\+', r'\+', path)
    return path_escaped

def scan_context(file_path: str) -> tuple:
    """Đọc file context, lưu trữ dưới dạng dict và bảo toàn thứ tự dòng gốc."""
    context = {}
    original_order = []
    
    with open(file_path, "r", encoding='utf-8') as f:
        for line in f:
            line_str = line.strip()
            if not line_str or line_str.startswith('#'):
                continue
                
            parts = line_str.split()
            if not parts:
                continue
                
            filepath = parts[0]
            # Chuẩn hóa đường dẫn bỏ escape @ nếu có
            filepath_clean = filepath.replace(r'\@', '@')
            
            context[filepath_clean] = parts[1:]
            original_order.append(filepath_clean)
            
    return context, original_order

def scan_dir(folder: str) -> list:
    """
    Quét thư mục thực tế và chuyển đổi sang định dạng path chuẩn của Android SELinux.
    Hỗ trợ Dynamic Partitions (system, vendor, product, system_ext, cust...).
    """
    folder_abs = os.path.abspath(folder).replace('\\', '/')
    part_name = os.path.basename(os.path.normpath(folder))
    
    allfiles = [
        '/',
        f'/{part_name}',
        f'/{part_name}/',
        f'/{part_name}/lost+found'
    ]
    
    for root, dirs, files in os.walk(folder, topdown=True):
        root_clean = os.path.abspath(root).replace('\\', '/')
        
        for dir_name in dirs:
            full_path = f"{root_clean}/{dir_name}"
            target_path = full_path.replace(folder_abs, '/' + part_name)
            allfiles.append(target_path)
            
        for file_name in files:
            full_path = f"{root_clean}/{file_name}"
            target_path = full_path.replace(folder_abs, '/' + part_name)
            allfiles.append(target_path)
                
    return sorted(set(allfiles), key=allfiles.index)

def context_patch(fs_file: dict, filename_list: list) -> tuple:
    """Vá context thông minh dựa trên luật cứng, quy tắc tương thích hoặc thừa kế thư mục cha."""
    new_fs = {}
    added_keys = []
    
    # Context mặc định cho file/folder không xác định
    default_permission = ['u:object_r:system_file:s0']
    if fs_file:
        default_permission = fs_file.get(next(iter(fs_file)), default_permission)

    for path in filename_list:
        actual_path = path if path.isprintable() else ''.join(c if c.isprintable() else '*' for c in path)
        
        # 1. Khớp chính xác với file_contexts gốc
        if actual_path in fs_file:
            new_fs[actual_path] = fs_file[actual_path]
            continue
            
        if actual_path in new_fs:
            continue

        permission = None
        clean_path = actual_path.lstrip('/')
        base_name = os.path.basename(actual_path)

        # 2. Khớp theo FIX_PERMISSIONS (Tên file/thư mục hoặc đuôi path)
        if clean_path in FIX_PERMISSIONS:
            permission = FIX_PERMISSIONS[clean_path]
        elif base_name in FIX_PERMISSIONS:
            permission = FIX_PERMISSIONS[base_name]
        else:
            # 3. Thừa kế context từ thư mục cha gần nhất
            tmp_path = os.path.dirname(actual_path)
            while tmp_path:
                if tmp_path in fs_file:
                    permission = fs_file[tmp_path]
                    break
                if tmp_path in new_fs:
                    permission = new_fs[tmp_path]
                    break
                if tmp_path in ('/', ''):
                    break
                
                prev_path = tmp_path
                tmp_path = os.path.dirname(tmp_path)
                if tmp_path == prev_path:
                    break

        # Nếu vẫn chưa tìm thấy context -> Dùng default
        if not permission:
            permission = default_permission

        print(f"ADD [{actual_path} : {' '.join(permission)}]")
        new_fs[actual_path] = permission
        added_keys.append(actual_path)
        
    return new_fs, added_keys

def main(dir_path: str, fs_config: str) -> None:
    origin, orig_order = scan_context(os.path.abspath(fs_config))
    allfiles = scan_dir(os.path.abspath(dir_path))
    
    new_fs, added_keys = context_patch(origin, allfiles)
    
    with open(fs_config, "w", encoding='utf-8', newline='\n') as f:
        written_keys = set()

        # Ghi các key cũ theo đúng thứ tự gốc
        for key in orig_order:
            if key in new_fs:
                formatted_key = escape_regex_path(key)
                f.write(f"{formatted_key} {' '.join(new_fs[key])}\n")
                written_keys.add(key)
                
        # Ghi các key mới được bổ sung
        for key in sorted(added_keys):
            if key in new_fs and key not in written_keys:
                formatted_key = escape_regex_path(key)
                f.write(f"{formatted_key} {' '.join(new_fs[key])}\n")
                written_keys.add(key)

    print("---")
    print(f"Load origin: {len(origin)} entries")
    print(f"Detect total: {len(allfiles)} entries")
    print(f"Added new: {len(added_keys)} entries successfully")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python script.py <dir_path> <file_contexts_path>")
        sys.exit(1)
        
    dir_arg, context_arg = sys.argv[1], sys.argv[2]
    
    if not os.path.exists(dir_arg) or not os.path.exists(context_arg):
        print("Error: Target directory or file_contexts path does not exist.")
        sys.exit(1)
        
    main(dir_arg, context_arg)