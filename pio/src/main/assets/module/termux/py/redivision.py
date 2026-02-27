#!/data/data/com.termux/files/usr/bin/python3

import os
import sys
import re
import shutil
import multiprocessing
import json
from concurrent.futures import ProcessPoolExecutor

DEFAULT_LIMIT = 50000

CLASS_DEF = re.compile(r'^\.class\b.*?\s+(L[^;\s]+;)')
METHOD_DEF = re.compile(r'^\.method\b.*?\s+([^\s(]+)\(([^)]*)\)(\S+)')
FIELD_DEF = re.compile(r'^\.field\b.*?\s+([^\s:]+):(\S+)')
TYPE_PATTERN = re.compile(r'\[*L[^;]+;|\[*[ZBCSIFJDV]')
STRING_PATTERN = re.compile(r'"((?:\\.|[^"\\])*)"')


# -------------------------------------------------
# Utils
# -------------------------------------------------

def extract_types(desc):
    return TYPE_PATTERN.findall(desc)


def detect_mode(base):
    smali_dir = os.path.join(base, "smali")
    if not os.path.isdir(smali_dir):
        return None
    if os.path.isdir(os.path.join(smali_dir, "classes")):
        return "apkeditor"
    return "apktool"


def get_dex_dir(base, mode, index):
    if mode == "apktool":
        return os.path.join(base, "smali" if index == 1 else f"smali_classes{index}")
    else:
        smali_base = os.path.join(base, "smali")
        return os.path.join(smali_base, "classes" if index == 1 else f"classes{index}")


# -------------------------------------------------
# APKEDITOR dex-file.json handler (GIỮ NGUYÊN)
# -------------------------------------------------

def handle_apkeditor_dex_json_latest(base, new_index):

    smali_dir = os.path.join(base, "smali")

    max_index = 1
    for name in os.listdir(smali_dir):
        if name.startswith("classes"):
            if name == "classes":
                idx = 1
            else:
                try:
                    idx = int(name.replace("classes", ""))
                except:
                    continue
            if idx < new_index and idx > max_index:
                max_index = idx

    old_name = "classes" if max_index == 1 else f"classes{max_index}"
    new_name = f"classes{new_index}"

    old_dir = os.path.join(smali_dir, old_name)
    new_dir = os.path.join(smali_dir, new_name)

    old_json = os.path.join(old_dir, "dex-file.json")
    new_json = os.path.join(new_dir, "dex-file.json")

    if not os.path.exists(old_json):
        return

    version = 40

    try:
        with open(old_json, "r", encoding="utf-8") as f:
            data = json.load(f)
            version = data.get("version", 40)
    except:
        pass

    shutil.move(old_json, new_json)

    with open(old_json, "w", encoding="utf-8") as f:
        json.dump({"version": version}, f, indent=2)


# -------------------------------------------------
# Parse smali
# -------------------------------------------------

def parse_smali(path):
    methods = set()
    fields = set()
    types = set()
    protos = set()
    strings = set()
    current_class = None
    instruction_count = 0
    invoke_count = 0
    const_string_count = 0

    try:
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                line = line.strip()
                
                if (
                    line
                    and not line.startswith(".")
                    and not line.startswith(":")
                    and not line.startswith("#")
                ):
                    instruction_count += 1

                if line.startswith("invoke-"):
                    invoke_count += 1

                if line.startswith("const-string"):
                    const_string_count += 1

                m = CLASS_DEF.match(line)
                if m:
                    current_class = m.group(1)
                    types.add(current_class)
                    continue

                m = FIELD_DEF.match(line)
                if m and current_class:
                    name, ftype = m.groups()
                    fields.add(f"{current_class}->{name}:{ftype}")
                    for t in extract_types(ftype):
                        types.add(t)
                    continue

                m = METHOD_DEF.match(line)
                if m and current_class:
                    name, params, ret = m.groups()
                    proto = f"({params}){ret}"

                    methods.add(f"{current_class}->{name}{proto}")
                    protos.add(proto)

                    for t in extract_types(params):
                        types.add(t)
                    for t in extract_types(ret):
                        types.add(t)
                    continue

                for s in STRING_PATTERN.findall(line):
                    strings.add(s)

    except:
        pass

    weight = (
        len(methods) * 5 +
        len(fields) * 2 +
        len(types) +
        len(protos) * 2 +
        len(strings) * 2 +
        instruction_count // 10 +
        invoke_count * 2
    )

    return path, methods, fields, types, protos, strings, weight


# -------------------------------------------------
# Collect dex data
# -------------------------------------------------

def collect_dex_data(base, mode):
    dex_data = {}
    file_cache = {}

    index = 1
    cpu = max(1, multiprocessing.cpu_count() - 1)

    while True:
        dex_dir = get_dex_dir(base, mode, index)
        if not os.path.isdir(dex_dir):
            break

        smali_files = []
        for root, _, files in os.walk(dex_dir):
            for f in files:
                if f.endswith(".smali"):
                    smali_files.append(os.path.join(root, f))

        methods = set()
        fields = set()
        types = set()
        protos = set()
        strings = set()

        with ProcessPoolExecutor(max_workers=cpu) as executor:
            results = executor.map(parse_smali, smali_files, chunksize=100)
            for path, m, f, t, p, s, w in results:
                methods.update(m)
                fields.update(f)
                types.update(t)
                protos.update(p)
                strings.update(s)

                file_cache[path] = {
                    "methods": m,
                    "fields": f,
                    "types": t,
                    "protos": p,
                    "strings": s,
                    "weight": w
                }

        dex_data[index] = {
            "dir": dex_dir,
            "files": smali_files,
            "methods": methods,
            "fields": fields,
            "types": types,
            "protos": protos,
            "strings": strings
        }

        index += 1

    return dex_data, file_cache


# -------------------------------------------------
# Stats
# -------------------------------------------------

def get_count(dex):
    return max(
        len(dex["methods"]),
        len(dex["fields"]),
        len(dex["types"]),
        len(dex["protos"]),
        len(dex["strings"])
    )


def print_stats(dex_data):
    for i in sorted(dex_data.keys()):
        dex = dex_data[i]
        print(
            f"DEX{i}: max:{get_count(dex)} "
            f"m:{len(dex['methods'])} "
            f"f:{len(dex['fields'])} "
            f"t:{len(dex['types'])} "
            f"p:{len(dex['protos'])} "
            f"s:{len(dex['strings'])}"
        )

# -------------------------------------------------
# Move file
# -------------------------------------------------

def move_file(file_path, src_index, dst_index,
              dex_data, file_cache, base, mode):

    src_dir = dex_data[src_index]["dir"]
    dst_dir = get_dex_dir(base, mode, dst_index)
    os.makedirs(dst_dir, exist_ok=True)

    rel = os.path.relpath(file_path, src_dir)
    dst_path = os.path.join(dst_dir, rel)
    os.makedirs(os.path.dirname(dst_path), exist_ok=True)

    shutil.move(file_path, dst_path)

    data = file_cache[file_path]

    dex_data[src_index]["files"].remove(file_path)
    dex_data[dst_index]["files"].append(dst_path)

    for key in ["methods", "fields", "types", "protos", "strings"]:
        dex_data[src_index][key] -= data[key]
        dex_data[dst_index][key] |= data[key]

    file_cache[dst_path] = data
    del file_cache[file_path]

# -------------------------------------------------
# Rebalance – chuẩn Android
# Không dex nào được vượt limit
# Tự tạo thêm dex nếu cần
# -------------------------------------------------

def rebalance_overflow_only(base, mode, limit):

    dex_data, file_cache = collect_dex_data(base, mode)
    print("Overflow before rebalance:\n")
    print_stats(dex_data)

    overflow_exists = any(
        get_count(dex_data[i]) > limit
        for i in dex_data
    )

    if not overflow_exists:
        return

    next_index = max(dex_data.keys()) + 1
    printed_newline = False

    def create_new_dex():
        nonlocal next_index, printed_newline

        if not printed_newline:
            print()
            printed_newline = True

        new_dir = get_dex_dir(base, mode, next_index)
        os.makedirs(new_dir, exist_ok=True)

        dex_data[next_index] = {
            "dir": new_dir,
            "files": [],
            "methods": set(),
            "fields": set(),
            "types": set(),
            "protos": set(),
            "strings": set()
        }

        if mode == "apkeditor":
            handle_apkeditor_dex_json_latest(base, next_index)

        print(f"Created DEX {next_index}")

        next_index += 1
        return next_index - 1

    current_new_dex = None

    for index in sorted(list(dex_data.keys())):

        while get_count(dex_data[index]) > limit:

            files_sorted = sorted(
                dex_data[index]["files"],
                key=lambda f: file_cache[f]["weight"],
                reverse=True
            )

            if not files_sorted:
                break

            moved = False

            for f in files_sorted:

                file_weight = file_cache[f]["weight"]

                # nếu file lớn hơn limit → vẫn phải move 1 lần duy nhất
                if file_weight > limit:
                    if current_new_dex is None:
                        current_new_dex = create_new_dex()

                    move_file(
                        f,
                        index,
                        current_new_dex,
                        dex_data,
                        file_cache,
                        base,
                        mode
                    )
                    moved = True
                    break

                # nếu chưa có dex mới
                if current_new_dex is None:
                    current_new_dex = create_new_dex()

                # nếu dex mới không đủ chỗ → tạo dex mới khác
                if get_count(dex_data[current_new_dex]) + file_weight > limit:
                    current_new_dex = create_new_dex()

                move_file(
                    f,
                    index,
                    current_new_dex,
                    dex_data,
                    file_cache,
                    base,
                    mode
                )

                moved = True
                break

            if not moved:
                # không còn file nào có thể move
                break

    print("\nOverflow after rebalance:\n")
    print_stats(dex_data)


# -------------------------------------------------
# MAIN
# -------------------------------------------------

def main():

    if len(sys.argv) < 2:
        print("Usage: python redivision.py <project_folder> [limit]")
        return

    base = sys.argv[1]

    if not os.path.isdir(base):
        print("Invalid project path")
        return

    try:
        limit = int(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_LIMIT
    except:
        limit = DEFAULT_LIMIT

    mode = detect_mode(base)

    if not mode:
        print("Cannot detect project structure\n", file=sys.stderr)
        sys.exit(1)

    print(f"Detected mode: {mode}")
    print(f"Limit: {limit}\n")

    rebalance_overflow_only(base, mode, limit)
    
    print(f"\nDone\n")

if __name__ == "__main__":
    main()
    