#!/data/data/com.tool.tree/files/home/termux/bin/python
import os
import sys
import shutil
import re
import json
import multiprocessing
from concurrent.futures import ProcessPoolExecutor


DEFAULT_LIMIT = 60000


CLASS_DEF = re.compile(r'^\.class\s+[^\n]*?(L[^;]+;)')
SUPER_DEF = re.compile(r'^\.super\s+(L[^;]+;)')
IMPLEMENTS_DEF = re.compile(r'^\.implements\s+(L[^;]+;)')
FIELD_DEF = re.compile(r'^\.field\s+[^\n]*?([^\s:]+):([^\s]+)')
METHOD_DEF = re.compile(r'^\.method\s+[^\n]*?([^\s\(]+)\((.*?)\)([^\s]+)')
INVOKE_REF = re.compile(r'(L[^;]+;)->([^\(]+)\((.*?)\)([^\s]+)')
FIELD_REF = re.compile(r'(L[^;]+;)->([^\:]+):([^\s]+)')
TYPE_REF = re.compile(r'(?:L[^;]+;|\[[ZBCSIFJDV]|\[+L[^;]+;)')


def parse_smali(file_path):
    method_ids = set()
    field_ids = set()
    type_ids = set()
    proto_ids = set()
    current_class = None

    try:
        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                line = line.strip()

                m = CLASS_DEF.match(line)
                if m:
                    current_class = m.group(1)
                    type_ids.add(current_class)
                    continue

                m = SUPER_DEF.match(line)
                if m:
                    type_ids.add(m.group(1))
                    continue

                m = IMPLEMENTS_DEF.match(line)
                if m:
                    type_ids.add(m.group(1))
                    continue

                m = FIELD_DEF.match(line)
                if m and current_class:
                    name, ftype = m.groups()
                    field_ids.add(f"{current_class}->{name}:{ftype}")
                    for t in TYPE_REF.findall(ftype):
                        type_ids.add(t)
                    continue

                m = METHOD_DEF.match(line)
                if m and current_class:
                    name, params, ret = m.groups()
                    proto = f"({params}){ret}"
                    method_ids.add(f"{current_class}->{name}{proto}")
                    proto_ids.add(proto)

                    for t in TYPE_REF.findall(proto):
                        type_ids.add(t)
                    continue

                for cls, name, params, ret in INVOKE_REF.findall(line):
                    proto = f"({params}){ret}"
                    method_ids.add(f"{cls}->{name}{proto}")
                    proto_ids.add(proto)
                    type_ids.add(cls)

                for cls, name, ftype in FIELD_REF.findall(line):
                    field_ids.add(f"{cls}->{name}:{ftype}")
                    type_ids.add(cls)

    except Exception:
        pass

    return file_path, method_ids, field_ids, type_ids, proto_ids


def collect_all_classes_dirs(base, mode):
    if mode == "apkeditor":
        return sorted(d for d in os.listdir(base) if d.startswith("classes"))

    elif mode == "apktool":
        return sorted(d for d in os.listdir(base)
                      if d == "smali" or d.startswith("smali_classes"))

    else:
        print("Invalid mode")
        sys.exit(1)


def collect_smali_files(base, dirs):
    files = []
    for d in dirs:
        root_dir = os.path.join(base, d)
        if not os.path.isdir(root_dir):
            continue
        for root, _, filenames in os.walk(root_dir):
            for name in filenames:
                if name.endswith(".smali"):
                    files.append(os.path.join(root, name))
    return files


def move_file(src, base, dex_dir):
    rel = os.path.relpath(src, base)
    rel_parts = rel.split(os.sep)[1:]
    if not rel_parts:
        return

    new_rel = os.path.join(*rel_parts)
    dest = os.path.join(dex_dir, new_rel)

    os.makedirs(os.path.dirname(dest), exist_ok=True)
    shutil.move(src, dest)


def remove_empty_dirs(base):
    for d in os.listdir(base):
        if d.startswith("classes") or d.startswith("smali"):
            full = os.path.join(base, d)
            if os.path.isdir(full) and not any(os.scandir(full)):
                shutil.rmtree(full)


def detect_mode(base_input):
    apktool_yml = os.path.join(base_input, "apktool.yml")
    if os.path.isfile(apktool_yml):
        print("Detected mode: apktool")
        return "apktool"
    else:
        print("Detected mode: apkeditor")
        return "apkeditor"


def main():
    if len(sys.argv) < 2:
        print("Usage: python redivision.py <path> [limit]")
        return

    base_input = sys.argv[1]
    limit = int(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_LIMIT

    mode = detect_mode(base_input)

    if mode == "apkeditor":
        base = os.path.join(base_input, "smali")
        if not os.path.isdir(base):
            print("Error: smali folder not found inside:", base_input)
            sys.exit(1)
    else:
        base = base_input

    print("Collecting directories...")
    class_dirs = collect_all_classes_dirs(base, mode)

    print("Collecting smali files...")
    all_files = collect_smali_files(base, class_dirs)
    print("Total smali files:", len(all_files))

    print("Parsing smali files...")
    cpu_count = max(1, multiprocessing.cpu_count() - 1)

    with ProcessPoolExecutor(max_workers=cpu_count) as executor:
        parsed = list(executor.map(parse_smali, all_files, chunksize=100))

    print("Packing classes dex...")

    dex_plan = []
    current_methods = set()
    current_fields = set()
    current_types = set()
    current_protos = set()
    current_files = []
    dex_index = 1

    for file_path, m, fd, t, p in parsed:

        nm = current_methods | m
        nf = current_fields | fd
        nt = current_types | t
        np = current_protos | p

        if (len(nm) > limit or
            len(nf) > limit or
            len(nt) > limit or
            len(np) > limit):

            dex_plan.append((dex_index, current_files))

            dex_index += 1
            current_methods = set()
            current_fields = set()
            current_types = set()
            current_protos = set()
            current_files = []

        current_methods |= m
        current_fields |= fd
        current_types |= t
        current_protos |= p
        current_files.append(file_path)

    if current_files:
        dex_plan.append((dex_index, current_files))

    print("Creating classes dex folders...")

    # apkeditor: kiểm tra xem classes có dex-file.json không
    dex_json_template = None
    if mode == "apkeditor":
        first_classes = os.path.join(base, "classes", "dex-file.json")
        if os.path.isfile(first_classes):
            dex_json_template = first_classes
            print("dex-file.json detected, will copy to new classes")

    for index, files in dex_plan:

        if mode == "apkeditor":
            dex_name = "classes" if index == 1 else f"classes{index}"
        else:
            dex_name = "smali" if index == 1 else f"smali_classes{index}"

        dex_dir = os.path.join(base, dex_name)
        os.makedirs(dex_dir, exist_ok=True)

        # Chỉ copy nếu có template và không phải classes đầu
        if mode == "apkeditor" and dex_json_template and index != 1:
            shutil.copy2(dex_json_template,
                         os.path.join(dex_dir, "dex-file.json"))

        for f in files:
            move_file(f, base, dex_dir)

    print("Cleaning empty dirs...")
    remove_empty_dirs(base)

    print("Total classes dex created:", len(dex_plan))


if __name__ == "__main__":
    main()