#!/data/data/com.tool.tree/files/home/termux/bin/python
# -*- coding: utf-8 -*-

import os
import sys

def scanfs(file_path: str) -> tuple:
    """Read original fs_config, skip comments/empty lines, and preserve line order."""
    filesystem_config = {}
    original_order = []
    
    with open(file_path, "r", encoding="utf-8") as file_:
        for line_num, line in enumerate(file_, 1):
            line_str = line.strip()
            if not line_str or line_str.startswith('#'):
                continue
            parts = line_str.split()
            if not parts:
                continue
            filepath, *other = parts
            filesystem_config[filepath] = other
            original_order.append(filepath)
            
            if len(other) > 4:
                print(f"[Warn] Line {line_num}: {filepath} has too many fields ({len(other)}).")
                
    return filesystem_config, original_order

def scan_dir(folder: str) -> list:
    """
    Scan real directory following Android POSIX structure.
    Automatically formats paths to fit standard Android fs_config.
    """
    folder_abs = os.path.abspath(folder)
    base_name = os.path.basename(os.path.normpath(folder))
    
    results = [
        base_name,
        f"{base_name}/lost+found"
    ]
    
    for root, dirs, files in os.walk(folder_abs):
        rel_root = os.path.relpath(root, folder_abs)
        rel_root = '' if rel_root == '.' else rel_root
        
        for d in dirs:
            path = os.path.join(base_name, rel_root, d)
            results.append(path.replace('\\', '/'))
            
        for f in files:
            path = os.path.join(base_name, rel_root, f)
            results.append(path.replace('\\', '/'))

    # Deduplicate while maintaining appearance order
    seen = set()
    deduped = []
    for item in results:
        if item not in seen:
            seen.add(item)
            deduped.append(item)
            
    return deduped

def islink(file_path: str) -> str or None:
    """Check and return symlink target on both Windows and Linux/Termux."""
    if os.name == 'nt':
        if os.path.exists(file_path) and not os.path.isdir(file_path):
            try:
                with open(file_path, 'rb') as f:
                    content = f.read()
                    # Cygwin / MSYS2 / Windows virtual symlink header
                    if content.startswith(b'!<symlink>\xff\xfe'):
                        link_bytes = content[12:]
                        return link_bytes.decode("utf-16", errors="ignore").replace('\x00', '').strip()
            except IOError:
                pass
    else:
        if os.path.islink(file_path):
            return os.readlink(file_path)
            
    return None

def fs_patch(fs_file: dict, dir_path: str) -> tuple:
    """Match files in real folder and patch missing entries into fs_config."""
    new_fs = {}
    added_keys = []
    new_add_count = 0
    
    print(f"FsPatcher: Loaded {len(fs_file)} original entries.")
    
    special_binaries = {
        "bin/su", "xbin/su", "disable_selinux.sh", "daemon", "ext/.su", 
        "install-recovery", "installed_su", "bin/rw-system.sh", "bin/getSPL"
    }

    dir_abs = os.path.abspath(dir_path)
    base_name = os.path.basename(os.path.normpath(dir_path))

    for path_entry in scan_dir(dir_abs):
        actual_entry = path_entry if path_entry.isprintable() else ''.join(c if c.isprintable() else '*' for c in path_entry)

        # 1. Keep existing entry if already present in original fs_config
        if actual_entry in fs_file:
            new_fs[actual_entry] = fs_file[actual_entry]
            continue

        if actual_entry in new_fs:
            continue

        # 2. Resolve accurate absolute path on local file system
        if actual_entry == base_name:
            real_file_path = dir_abs
        elif actual_entry.startswith(f"{base_name}/"):
            rel_path = actual_entry[len(base_name) + 1:]
            real_file_path = os.path.join(dir_abs, rel_path.replace('/', os.sep))
        else:
            real_file_path = os.path.join(dir_abs, actual_entry.replace('/', os.sep))

        is_dir = os.path.isdir(real_file_path)
        exists = os.path.exists(real_file_path) or os.path.islink(real_file_path)
        link_target = islink(real_file_path)

        # 3. Assign Android UID/GID/Permissions
        is_bin_path = any(x in actual_entry for x in ["bin/", "xbin/"])
        gid = '2000' if is_bin_path else '0'
        uid = '0'

        if is_dir:
            config = [uid, gid, '0755']
        elif not exists:
            config = [uid, gid, '0755']
        elif link_target is not None:
            # Handle File Symlinks
            if is_bin_path:
                mode = '0755'
            elif ".sh" in actual_entry:
                mode = "0750"
            else:
                mode = "0644"
            config = [uid, gid, mode, link_target]
        elif is_bin_path:
            # Handle Executable Binaries / Scripts
            if ".sh" in actual_entry:
                mode = "0750"
            elif any(s in actual_entry for s in special_binaries):
                mode = "0755"
            else:
                mode = "0755"
            config = [uid, gid, mode]
        else:
            # Standard Data / Library Files
            config = [uid, '0', '0644']

        print(f"ADD [{actual_entry} {' '.join(config)}]")
        new_add_count += 1
        new_fs[actual_entry] = config
        added_keys.append(actual_entry)

    return new_fs, added_keys, new_add_count

def main(dir_path: str, fs_config: str) -> None:
    dir_abs = os.path.abspath(dir_path)
    fs_config_abs = os.path.abspath(fs_config)
    
    origin_fs, orig_order = scanfs(fs_config_abs)
    new_fs, added_keys, new_add = fs_patch(origin_fs, dir_abs)
    
    with open(fs_config_abs, "w", encoding='utf-8', newline='\n') as f:
        written_keys = set()
        
        # Step 1: Write back original entries preserving order
        for key in orig_order:
            if key in new_fs:
                f.write(f"{key} {' '.join(new_fs[key])}\n")
                written_keys.add(key)
                
        # Step 2: Append newly added entries sorted alphabetically
        for key in sorted(added_keys):
            if key in new_fs and key not in written_keys:
                f.write(f"{key} {' '.join(new_fs[key])}\n")
                written_keys.add(key)
                
    print("---")
    print(f"FsPatcher: Successfully added {new_add} entries to configuration.")

def usage():
    print("""
FsPatcher: Android FsConfig Patching Tool
Usage: python script.py <Folder_Path> <FsConfig_File>
""")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("FsPatcher: Insufficient parameters.")
        usage()
        sys.exit(1)
        
    folder_arg, config_arg = sys.argv[1], sys.argv[2]
    
    if os.path.isdir(os.path.abspath(folder_arg)) and os.path.isfile(os.path.abspath(config_arg)):
        main(folder_arg, config_arg)
    else:
        print("Error: Specified directory or fs_config file does not exist.")
        usage()
        sys.exit(1)