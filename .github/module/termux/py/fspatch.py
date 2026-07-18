#!/data/data/com.tool.tree/files/home/termux/bin/python

# -*- coding: utf-8 -*-
import os
import sys

def scanfs(file) -> dict:
    filesystem_config = {}
    with open(file, "r") as file_:
        for line_num, i in enumerate(file_.readlines(), 1):
            line = i.strip()
            if not line or line.startswith('#'):
                continue
            parts = line.split()
            if len(parts) < 1:
                continue
            filepath, *other = parts
            filesystem_config[filepath] = other
            if len(other) > 4:
                print(f"[Warn] Current {line_num}: {filepath} there are too many schools ({len(other)}).")
    return filesystem_config

def scan_dir(folder):
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

def islink(file) -> str and None:
    if os.name == 'nt':
        if not os.path.isdir(file):
            with open(file, 'rb') as f:
                if f.read(12) == b'!<symlink>\xff\xfe':
                    return f.read().decode("utf-8").replace('\x00', '')
                else:
                    return
    elif os.name == 'posix':
        if os.path.islink(file):
            return os.readlink(file)
        else:
            return


def fs_patch(fs_file, dir_path) -> tuple:  # 接收两个字典对比
    new_fs = {}
    new_add = 0
    r_fs = {}
    print("FsPatcher: Load origin %d" % (len(fs_file.keys())) + " entries")
    for i in scan_dir(os.path.abspath(dir_path)):
        if not i.isprintable():
            tmp = ''
            for c in i:
                tmp += c if c.isprintable() else '*'
            i = tmp
        if fs_file.get(i):
            new_fs[i] = fs_file[i]
        else:
            if r_fs.get(i):
                continue
            if os.name == 'nt':
                filepath = os.path.abspath(dir_path + os.sep + ".." + os.sep + i.replace('/', '\\'))
            elif os.name == 'posix':
                filepath = os.path.abspath(dir_path + os.sep + ".." + os.sep + i)
            else:
                filepath = os.path.abspath(dir_path + os.sep + ".." + os.sep + i)
            if os.path.isdir(filepath):
                uid = '0'
                if "system/bin" in i or "system/xbin" in i or "vendor/bin" in i:
                    gid = '2000'
                else:
                    gid = '0'
                mode = '0755'  # dir path always 755
                config = [uid, gid, mode]
            elif not os.path.exists(filepath):
                config = ['0', '0', '0755']
            elif islink(filepath):
                uid = '0'
                if ("system/bin" in i) or ("system/xbin" in i) or ("vendor/bin" in i):
                    gid = '2000'
                else:
                    gid = '0'
                if ("/bin" in i) or ("/xbin" in i):
                    mode = '0755'
                elif ".sh" in i:
                    mode = "0750"
                else:
                    mode = "0644"
                link = islink(filepath)
                config = [uid, gid, mode, link]
            elif ("/bin" in i) or ("/xbin" in i):
                uid = '0'
                mode = '0755'
                if ("system/bin" in i) or ("system/xbin" in i) or ("vendor/bin" in i):
                    gid = '2000'
                else:
                    gid = '0'
                    mode = '0755'
                if ".sh" in i:
                    mode = "0750"
                else:
                    for s in ["/bin/su", "/xbin/su", "disable_selinux.sh", "daemon", "ext/.su", "install-recovery",
                              'installed_su', 'bin/rw-system.sh', 'bin/getSPL']:
                        if s in i:
                            mode = "0755"
                config = [uid, gid, mode]
            else:
                uid = '0'
                gid = '0'
                mode = '0644'
                config = [uid, gid, mode]
            print(f'Add [{i}{config}]')
            r_fs[i] = 1
            new_add += 1
            new_fs[i] = config
    return new_fs, new_add


def main(dir_path, fs_config) -> None:
    new_fs, new_add = fs_patch(scanfs(os.path.abspath(fs_config)), dir_path)
    with open(fs_config, "w", encoding='utf-8', newline='\n') as f:
        f.writelines([i + " " + " ".join(new_fs[i]) + "\n" for i in sorted(new_fs.keys())])
    print('FsPatcher: Add %d' % new_add + " entries")


def usage():
    print("""
    FsPatcher: FsConfig Patching Tool
    Usage： ./FsPatcher [Folders] [FsConfig]
          """)


if __name__ == "__main__":
    if sys.argv.__len__() < 3:
        print("FsPatcher: Insufficient parameters")
        usage()
    elif os.path.isfile(os.path.abspath(sys.argv[2])) and os.path.isdir(os.path.abspath(sys.argv[1])):
        main(sys.argv[1], sys.argv[2])
    else:
        usage()
