#!/data/data/com.tool.tree/files/home/termux/bin/python
# -*- coding: utf-8 -*-

import os
import sys

# Key không có dấu "/" ở đầu để khớp hoàn chỉnh với cấu trúc sinh ra từ scan_dir
fix_permission = {
    "vendor/bin/hw/android.hardware.wifi@1.0": ["u:object_r:hal_wifi_default_exec:s0"],
    "system/bin/init": ["u:object_r:init_exec:s0"],
    "lost+found": ["u:object_r:rootfs:s0"],
    "system/bin/idmap": ["u:object_r:idmap_exec:s0"],
    "system/bin/fsck": ["u:object_r:fsck_exec:s0"],
    "system/bin/e2fsck": ["u:object_r:fsck_exec:s0"],
    "system/bin/logcat": ["u:object_r:logcat_exec:s0"]
}

def scan_context(file) -> tuple:
    """Đọc file context, giữ nguyên cấu trúc Regex gốc và bảo toàn thứ tự dòng."""
    context = {}
    original_order = []
    
    with open(file, "r", encoding='utf-8') as file_:
        for i in file_:
            line = i.strip()
            if not line or line.startswith('#'):
                continue
                
            parts = line.split()
            if not parts:
                continue
                
            filepath, *other = parts
            filepath = filepath.replace(r'\@', '@')
            context[filepath] = other
            original_order.append(filepath)
            
    return context, original_order

def scan_dir(folder) -> list:
    """
    Quét thư mục thực tế và chuyển đổi sang định dạng path chuẩn của Android.
    HỖ TRỢ DYNAMIC PARTITIONS.
    """
    folder_abs = os.path.abspath(folder).replace('\\', '/')
    part_name = os.path.basename(os.path.normpath(folder))
            
    # Xây dựng các root path chuẩn
    allfiles = ['/', f'/{part_name}/lost+found', f'/{part_name}', f'/{part_name}/']
    
    for root, dirs, files in os.walk(folder, topdown=True):
        # Chuẩn hóa root path hiện tại về dạng gạch chéo xuôi
        root_clean = os.path.abspath(root).replace('\\', '/')
        
        for dir_ in dirs:
            full_path = f"{root_clean}/{dir_}"
            target_path = full_path.replace(folder_abs, '/' + part_name)
            allfiles.append(target_path)
            
        for file in files:
            full_path = f"{root_clean}/{file}"
            target_path = full_path.replace(folder_abs, '/' + part_name)
            allfiles.append(target_path)
                
    return sorted(set(allfiles), key=allfiles.index)

def context_patch(fs_file, filename) -> tuple:
    """Vá context thông minh dựa trên luật cứng hoặc thừa kế từ thư mục cha."""
    new_fs = {}
    added_keys = []
    
    permission_d = ['u:object_r:system_file:s0']
    if fs_file:
        permission_d = fs_file.get(next(iter(fs_file)), permission_d)

    for i in filename:
        actual_i = i if i.isprintable() else ''.join(c if c.isprintable() else '*' for c in i)
        
        if fs_file.get(actual_i):
            new_fs[actual_i] = fs_file[actual_i]
            continue
            
        if actual_i in new_fs:
            continue

        permission = permission_d
        clean_i = actual_i.lstrip('/')
        
        if clean_i in fix_permission:
            permission = fix_permission[clean_i]
        elif actual_i in fix_permission:
            permission = fix_permission[actual_i]
        else:
            # Thuật toán tìm ngược lên thư mục cha có thêm điều kiện chặn lặp vô hạn
            tmp_path = os.path.dirname(actual_i)
            while tmp_path and tmp_path != "/":
                if tmp_path in fs_file:
                    permission = fs_file[tmp_path]
                    break
                prev_path = tmp_path
                tmp_path = os.path.dirname(tmp_path)
                if tmp_path == prev_path: # Chặn lỗi lặp cấu trúc lạ
                    break

        print(f"ADD [{actual_i}:{permission}]")
        new_fs[actual_i] = permission
        added_keys.append(actual_i)
        
    return new_fs, added_keys

def main(dir_path, fs_config) -> None:
    origin, orig_order = scan_context(os.path.abspath(fs_config))
    allfiles = scan_dir(os.path.abspath(dir_path))
    
    new_fs, added_keys = context_patch(origin, allfiles)
    
    with open(fs_config, "w", encoding='utf-8', newline='\n') as f:
        # Ghi lại các key cũ theo đúng thứ tự ban đầu
        for key in orig_order:
            if key in new_fs:
                f.write(f"{key} {' '.join(new_fs[key])}\n")
                del new_fs[key]
                
        # Ghi các key mới được thêm vào cuối file
        for key in sorted(added_keys):
            if key in new_fs:
                f.write(f"{key} {' '.join(new_fs[key])}\n")

    print("Load origin %d entries" % (len(origin.keys())))
    print("Detect total %d entries" % (len(allfiles)))
    print("Add %d entries thành công" % (len(added_keys)))

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Insufficient parameters")
        sys.exit(1)
    if not os.path.exists(sys.argv[1]) or not os.path.exists(sys.argv[2]):
        print("File does not exist")
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])