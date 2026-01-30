#!/data/data/com.tool.tree/files/home/termux/bin/python

import os
import hashlib
import json
import argparse

def compute_hash(file_path):
    sha256 = hashlib.sha256()
    try:
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                sha256.update(chunk)
        return sha256.hexdigest()
    except Exception as e:
        print(f"Lỗi khi đọc file {file_path}: {e}")
        return None

def load_hash_db(hash_db_path):
    if os.path.exists(hash_db_path):
        with open(hash_db_path, "r") as f:
            return json.load(f)
    return {}

def save_hash_db(hash_db_path, hash_db):
    with open(hash_db_path, "w") as f:
        json.dump(hash_db, f, indent=2)

def scan_directory(root_dir, excluded_exts=None):
    if excluded_exts is None:
        excluded_exts = []
    current_hashes = {}
    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if any(filename.endswith(ext) for ext in excluded_exts):
                continue
            file_path = os.path.join(dirpath, filename)
            rel_path = os.path.relpath(file_path, root_dir)
            file_hash = compute_hash(file_path)
            if file_hash:
                current_hashes[rel_path] = file_hash
    return current_hashes

def check_changes(root_dir, hash_db_path):
    excluded_extensions = [".bak", ".tmp", ".log"]
    previous_hashes = load_hash_db(hash_db_path)
    current_hashes = scan_directory(root_dir, excluded_extensions)
    changed_files = []
    for rel_path, current_hash in current_hashes.items():
        if rel_path not in previous_hashes:
            print(f"new: {rel_path}")
            changed_files.append(rel_path)
        elif previous_hashes[rel_path] != current_hash:
            print(f"fixed: {rel_path}")
            changed_files.append(rel_path)
    for rel_path in previous_hashes:
        if rel_path not in current_hashes:
            print(f"deleted: {rel_path}")

    save_hash_db(hash_db_path, current_hashes)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Kiểm tra thay đổi file trong thư mục.")
    parser.add_argument("directory", help="Đường dẫn tới thư mục cần kiểm tra")
    parser.add_argument("hash_db", help="Đường dẫn file lưu trữ hash (VD: path/hash_db.json)")
    args = parser.parse_args()

    check_changes(args.directory, args.hash_db)
