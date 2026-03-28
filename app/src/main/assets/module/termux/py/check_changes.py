#!/data/data/com.tool.tree/files/home/termux/bin/python

import os
import hashlib
import json
import argparse
import multiprocessing as mp


def compute_hash(file_path):
    sha256 = hashlib.sha256()
    try:
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(65536), b""):
                sha256.update(chunk)
        return file_path, sha256.hexdigest()
    except Exception:
        return file_path, None


def load_hash_db(hash_db_path):
    if os.path.exists(hash_db_path):
        with open(hash_db_path, "r") as f:
            return json.load(f)
    return {}


def save_hash_db(hash_db_path, hash_db):
    with open(hash_db_path, "w") as f:
        json.dump(hash_db, f, indent=2)


def collect_files(root_dir, excluded_exts):
    files = []

    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:

            if filename.endswith(tuple(excluded_exts)):
                continue

            path = os.path.join(dirpath, filename)
            files.append(path)

    return files


def scan_directory(root_dir, excluded_exts):

    files = collect_files(root_dir, excluded_exts)

    cpu = mp.cpu_count()

    current_hashes = {}

    with mp.Pool(cpu) as pool:

        for file_path, file_hash in pool.imap_unordered(compute_hash, files):

            if file_hash:
                rel = os.path.relpath(file_path, root_dir)
                current_hashes[rel] = file_hash

    return current_hashes


def check_changes(root_dir, hash_db_path):

    excluded_extensions = [".bak", ".tmp", ".log"]

    previous_hashes = load_hash_db(hash_db_path)

    current_hashes = scan_directory(root_dir, excluded_extensions)

    for rel_path, current_hash in current_hashes.items():

        if rel_path not in previous_hashes:
            print(f"new: {rel_path}")

        elif previous_hashes[rel_path] != current_hash:
            print(f"fixed: {rel_path}")

    for rel_path in previous_hashes:
        if rel_path not in current_hashes:
            print(f"deleted: {rel_path}")

    save_hash_db(hash_db_path, current_hashes)


if __name__ == "__main__":

    parser = argparse.ArgumentParser()

    parser.add_argument("directory")

    parser.add_argument("hash_db")

    args = parser.parse_args()

    check_changes(args.directory, args.hash_db)