#!/data/data/com.tool.tree/files/home/termux/bin/python

import os
import sys
import re
import shutil
import json
from concurrent.futures import ThreadPoolExecutor

DEFAULT_LIMIT = 60000  # Safe threshold for 1 DEX file

# --- DEFINITION REGEXES ---
CLASS_DEF = re.compile(r'^\.class\b.*?\s+(L[^;\s]+;)')
METHOD_DEF = re.compile(r'^\.method\b.*?\s+([^\s(]+)\(([^)]*)\)(\S+)')
FIELD_DEF = re.compile(r'^\.field\b.*?\s+([^\s:]+):(\S+)')

# --- REFERENCE REGEXES (ADVANCED SCANNING) ---
# Captures method calls: invoke-xxx {v0, v1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
INVOKE_REF = re.compile(r'\bephemeral_placeholder_if_needed\b|'
                        r'(?:invoke-[^\s]+)\s+{[^}]+},\s*(L[^;]+;)->([^\s(]+)\(([^)]*)\)(\S+)')

# Captures field accesses: iget-object, sput-boolean v0, Lcom/abc;->TAG:Ljava/lang/String;
FIELD_REF = re.compile(r'(?:[is][gsp]et-[^\s]*)\s+[^,]+,\s*(L[^;]+;)->([^\s:]+):(\S+)')

# Captures array or instance types appearing inside instructions (e.g., new-instance, check-cast)
TYPE_PATTERN = re.compile(r'\[*L[^;\s]+;|\[*[ZBCSIFJDV]')
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
    return os.path.join(base, "smali", "classes" if index == 1 else f"classes{index}")

def scan_smali_files(folder):
    result = []
    stack = [folder]
    while stack:
        current = stack.pop()
        try:
            with os.scandir(current) as entries:
                for entry in entries:
                    if entry.is_dir():
                        stack.append(entry.path)
                    elif entry.name.endswith(".smali"):
                        result.append(entry.path)
        except:
            pass
    return result

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
    old_dir = os.path.join(smali_dir, old_name)
    new_dir = os.path.join(smali_dir, f"classes{new_index}")

    old_json = os.path.join(old_dir, "dex-file.json")
    new_json = os.path.join(new_dir, "dex-file.json")

    if not os.path.exists(old_json):
        return

    version = 40
    try:
        with open(old_json, "r", encoding="utf-8") as f:
            version = json.load(f).get("version", 40)
    except:
        pass

    os.makedirs(new_dir, exist_ok=True)
    shutil.move(old_json, new_json)
    with open(old_json, "w", encoding="utf-8") as f:
        json.dump({"version": version}, f, indent=2)

# -------------------------------------------------
# Parse smali (Deep Scanning Logic)
# -------------------------------------------------

def parse_smali(path):
    methods, fields, types, protos, strings = set(), set(), set(), set(), set()
    current_class = None

    try:
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#"):
                    continue

                # 1. SCAN DEFINITIONS
                if line.startswith(".class"):
                    m = CLASS_DEF.match(line)
                    if m:
                        current_class = m.group(1)
                        types.add(current_class)
                    continue

                if line.startswith(".field") and current_class:
                    m = FIELD_DEF.match(line)
                    if m:
                        name, ftype = m.groups()
                        fields.add(f"{current_class}->{name}:{ftype}")
                        types.update(extract_types(ftype))
                    continue

                if line.startswith(".method") and current_class:
                    m = METHOD_DEF.match(line)
                    if m:
                        name, params, ret = m.groups()
                        proto = f"({params}){ret}"
                        methods.add(f"{current_class}->{name}{proto}")
                        protos.add(proto)
                        types.update(extract_types(params))
                        types.update(extract_types(ret))
                    continue

                # 2. SCAN IMPLICIT REFERENCES
                if line.startswith("invoke-"):
                    m = INVOKE_REF.match(line)
                    if m:
                        cls, name, params, ret = m.groups()
                        proto = f"({params}){ret}"
                        methods.add(f"{cls}->{name}{proto}")
                        protos.add(proto)
                        types.add(cls)
                        types.update(extract_types(params))
                        types.update(extract_types(ret))
                    continue

                if line.startswith(("iget", "iput", "sget", "sput")):
                    m = FIELD_REF.match(line)
                    if m:
                        cls, name, ftype = m.groups()
                        fields.add(f"{cls}->{name}:{ftype}")
                        types.add(cls)
                        types.update(extract_types(ftype))
                    continue

                if "const-string" in line:
                    m = STRING_PATTERN.search(line)
                    if m:
                        strings.add(m.group(1))
                    continue

                # Fallback catch for inline type initialization opcodes
                if any(x in line for x in ["new-instance", "check-cast", "instance-of", "new-array"]):
                    types.update(extract_types(line))

    except:
        pass

    weight = max(len(methods), len(fields), len(types), len(protos), len(strings))
    return path, methods, fields, types, protos, strings, weight

# -------------------------------------------------
# Collect & Optimize Rebalance Logic
# -------------------------------------------------

def collect_dex_data(base, mode):
    dex_data = {}
    file_cache = {}
    index = 1
    cpu = min(32, max(4, (os.cpu_count() or 4) * 2))

    while True:
        dex_dir = get_dex_dir(base, mode, index)
        if not os.path.isdir(dex_dir):
            break

        smali_files = scan_smali_files(dex_dir)
        methods, fields, types, protos, strings = set(), set(), set(), set(), set()

        if smali_files:
            with ThreadPoolExecutor(max_workers=cpu) as executor:
                results = executor.map(parse_smali, smali_files)
                for path, m, f, t, p, s, w in results:
                    methods.update(m)
                    fields.update(f)
                    types.update(t)
                    protos.update(p)
                    strings.update(s)
                    file_cache[path] = {"methods": m, "fields": f, "types": t, "protos": p, "strings": s, "weight": w}

        dex_data[index] = {
            "dir": dex_dir, "files": smali_files,
            "methods": methods, "fields": fields, "types": types, "protos": protos, "strings": strings
        }
        index += 1

    return dex_data, file_cache

def get_count(dex):
    return max(len(dex["methods"]), len(dex["fields"]), len(dex["types"]), len(dex["protos"]), len(dex["strings"]))

def print_stats(dex_data):
    for i in sorted(dex_data.keys()):
        dex = dex_data[i]
        print(f"DEX{i}: max:{get_count(dex)} | m:{len(dex['methods'])} f:{len(dex['fields'])} t:{len(dex['types'])} p:{len(dex['protos'])} s:{len(dex['strings'])}")

def rebalance_overflow_only(base, mode, limit):
    dex_data, file_cache = collect_dex_data(base, mode)
    print("Stats before optimization (Including Deep References):")
    print_stats(dex_data)

    overflow_indices = [i for i in dex_data if get_count(dex_data[i]) > limit]
    if not overflow_indices:
        print("\nCongratulations! All DEX structures are normalized and well within limits.")
        return

    next_index = max(dex_data.keys()) + 1
    move_plan = []

    virtual_dex = {
        i: {k: set(dex_data[i][k]) for k in ["methods", "fields", "types", "protos", "strings"]}
        for i in dex_data
    }

    for idx in sorted(overflow_indices):
        files_in_dex = sorted(dex_data[idx]["files"], key=lambda f: file_cache[f]["weight"], reverse=True)
        current_dst_dex = None

        for f_path in files_in_dex:
            v_max = max(len(virtual_dex[idx][k]) for k in ["methods", "fields", "types", "protos", "strings"])
            if v_max <= limit:
                break

            f_data = file_cache[f_path]

            if current_dst_dex is None:
                current_dst_dex = next_index
                virtual_dex[current_dst_dex] = {k: set() for k in ["methods", "fields", "types", "protos", "strings"]}
                next_index += 1

            temp_counts = []
            for k in ["methods", "fields", "types", "protos", "strings"]:
                temp_counts.append(len(virtual_dex[current_dst_dex][k] | f_data[k]))
            
            if max(temp_counts) > limit:
                current_dst_dex = next_index
                virtual_dex[current_dst_dex] = {k: set() for k in ["methods", "fields", "types", "protos", "strings"]}
                next_index += 1

            for k in ["methods", "fields", "types", "protos", "strings"]:
                virtual_dex[idx][k] -= f_data[k]
                virtual_dex[current_dst_dex][k].update(f_data[k])

            move_plan.append((f_path, idx, current_dst_dex))

    if not move_plan:
        return

    print(f"\nStarting migration plan for {len(move_plan)} smali files...")
    created_dex_indices = set()

    for src_path, src_idx, dst_idx in move_plan:
        src_dir = dex_data[src_idx]["dir"]
        dst_dir = get_dex_dir(base, mode, dst_idx)

        if dst_idx not in created_dex_indices:
            os.makedirs(dst_dir, exist_ok=True)
            if mode == "apkeditor":
                handle_apkeditor_dex_json_latest(base, dst_idx)
            created_dex_indices.add(dst_idx)

        rel = os.path.relpath(src_path, src_dir)
        dst_path = os.path.join(dst_dir, rel)
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        
        try:
            shutil.move(src_path, dst_path)
        except Exception as e:
            print(f"Migration error: {src_path} -> {e}")

    final_dex_data, _ = collect_dex_data(base, mode)
    print("\nStats after optimization:")
    print_stats(final_dex_data)

# -------------------------------------------------
# MAIN
# -------------------------------------------------

def main():
    if len(sys.argv) < 2:
        print("Usage: python redivision.py <project_folder> [limit]")
        return

    base = sys.argv[1]
    if not os.path.isdir(base):
        print("Invalid project directory path")
        return

    try:
        limit = int(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_LIMIT
    except:
        limit = DEFAULT_LIMIT

    mode = detect_mode(base)
    if not mode:
        print("Failed to recognize project architecture (Requires Apktool or ApkEditor directory tree)", file=sys.stderr)
        sys.exit(1)

    print(f"Detected project mode: {mode}")
    print(f"Identifier limit cap: {limit}\n")

    rebalance_overflow_only(base, mode, limit)
    print("\nProcess completed successfully!\n")

if __name__ == "__main__":
    main()