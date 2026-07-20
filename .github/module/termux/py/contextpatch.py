#!/data/data/com.tool.tree/files/home/termux/bin/python
# -*- coding: utf-8 -*-

import os
import sys

# Đồng bộ kiểu dữ liệu mảng [list] để không bị lỗi rã chữ cái khi dùng join()
# Đã điều chỉnh key không có dấu "/" ở đầu để khớp hoàn chỉnh với cấu trúc sinh ra từ scan_dir
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
    HỖ TRỢ DYNAMIC PARTITIONS: Tự động đưa tên thư mục tạm về tên phân vùng chuẩn.
    """
# Lấy tên thư mục gốc (giữ nguyên hoa/thường để tránh lỗi phân vùng tùy biến)
    folder_name_raw = os.path.basename(os.path.normpath(folder))
    folder_name = folder_name_raw.lower()
    
    # Mặc định là tên thư mục gốc ban đầu nếu không nhận diện được phân vùng chuẩn
    part_name = folder_name_raw
    for partition in ['vendor', 'product', 'system_ext', 'odm', 'system']:
        if partition in folder_name:
            part_name = partition
            break
            
    # Xây dựng các root path chuẩn (SELinux của Android thường viết dạng /vendor hay /vendor/.*)
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
        
        # Đồng bộ hóa việc check luật cứng (loại bỏ dấu "/" ở đầu actual_i nếu có để khớp fix_permission)
        clean_i = actual_i.lstrip('/')
        
        if clean_i in fix_permission:
            permission = fix_permission[clean_i]
        elif actual_i in fix_permission:
            permission = fix_permission[actual_i]
        else:
            # Thuật toán tìm ngược lên thư mục cha để lấy quyền kế thừa
            tmp_path = os.path.dirname(actual_i)
            while tmp_path and tmp_path != "/":
                if tmp_path in fs_file:
                    permission = fs_file[tmp_path]
                    break
                tmp_path = os.path.dirname(tmp_path)

        print(f"ADD [{actual_i}:{permission}]")
        new_fs[actual_i] = permission
        added_keys.append(actual_i)
        
    return new_fs, added_keys

def main(dir_path, fs_config) -> None:
    origin, orig_order = scan_context(os.path.abspath(fs_config))
    allfiles = scan_dir(os.path.abspath(dir_path))
    
    new_fs, added_keys = context_patch(origin, allfiles)
    
    with open(fs_config, "w", encoding='utf-8', newline='\n') as f:
        for key in orig_order:
            if key in new_fs:
                f.write(f"{key} {' '.join(new_fs[key])}\n")
                del new_fs[key]
                
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