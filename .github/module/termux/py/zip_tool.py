#!/data/data/com.tool.tree/files/home/termux/bin/python
import argparse
import datetime
import fnmatch
import glob
import os
import shutil
import sys
import xml.etree.ElementTree as ET
import zipfile

CACHE_FILENAME = ".cache_zip"


def parse_timestamp(ts_str):
  """Parses a timestamp string 'YYYY-MM-DD HH:MM:SS' into a zipfile date_time tuple."""
  try:
    dt = datetime.datetime.strptime(ts_str, "%Y-%m-%d %H:%M:%S")
    return (dt.year, dt.month, dt.day, dt.hour, dt.minute, dt.second)
  except Exception:
    return (2009, 1, 1, 0, 0, 0)  # Default Android Epoch fallback


def list_zip_contents(zip_path, password=None, quiet=False, patterns=None):
  """Prints contents of ZIP archive filtered by optional multi-patterns."""
  if not os.path.exists(zip_path):
    print(f"Error: File '{zip_path}' not found.")
    return False

  pwd_bytes = password.encode("utf-8") if password else None
  try:
    with zipfile.ZipFile(zip_path, "r") as zip_ref:
      if pwd_bytes:
        zip_ref.setpassword(pwd_bytes)

      infolist = zip_ref.infolist()
      if patterns:
        filtered_info = []
        for info in infolist:
          if any(fnmatch.fnmatch(info.filename, p) for p in patterns):
            filtered_info.append(info)
        infolist = filtered_info

      print(f"\nContents of archive: '{zip_path}'")
      print(
          f"{'Filename':<45} {'Compressed':<12} {'Uncompressed':<14}"
          f" {'Method':<10}"
      )
      print("-" * 83)

      total_comp = 0
      total_uncomp = 0
      count = 0

      for info in infolist:
        comp_type = (
            "Stored"
            if info.compress_type == zipfile.ZIP_STORED
            else "Deflated"
        )
        print(
            f"{info.filename:<45} {info.compress_size:<12}"
            f" {info.file_size:<14} {comp_type:<10}"
        )
        total_comp += info.compress_size
        total_uncomp += info.file_size
        count += 1

      print("-" * 83)
      print(
          f"Total files: {count} | Compressed: {total_comp} bytes | Original:"
          f" {total_uncomp} bytes\n"
      )
  except Exception as e:
    print(f"Error reading ZIP contents (password might be required): {e}")
    return False
  return True


def extract_to_stdout(zip_path, member_patterns, password=None):
  """Extracts specified multi-pattern file(s) directly to stdout (pipe)."""
  if not os.path.exists(zip_path):
    print(f"Error: File '{zip_path}' not found.", file=sys.stderr)
    return False

  pwd_bytes = password.encode("utf-8") if password else None
  try:
    with zipfile.ZipFile(zip_path, "r") as zip_ref:
      if pwd_bytes:
        zip_ref.setpassword(pwd_bytes)

      matched = []
      for info in zip_ref.infolist():
        if any(fnmatch.fnmatch(info.filename, p) for p in member_patterns):
          matched.append(info.filename)

      if not matched:
        print(
            f"Error: No members matched patterns {member_patterns} in ZIP.",
            file=sys.stderr,
        )
        return False

      for name in matched:
        with zip_ref.open(name) as f:
          sys.stdout.buffer.write(f.read())
  except Exception as e:
    print(f"Error extracting to stdout: {e}", file=sys.stderr)
    return False
  return True


def extract_zip(
    zip_path,
    extract_to,
    password=None,
    member_patterns=None,
    quiet=False,
    clear_source=False,
    force=False,
):
  """Extracts a ZIP file or specific multi-pattern members and records metadata with timestamps."""
  if not os.path.exists(zip_path):
    if not quiet:
      print(f"Error: File '{zip_path}' not found.")
    return False

  if os.path.exists(extract_to) and os.listdir(extract_to):
    if not force:
      if not quiet:
        print(
            f"Error: Target directory '{extract_to}' already exists and is not"
            " empty. Use -f to overwrite."
        )
      return False

  os.makedirs(extract_to, exist_ok=True)
  pwd_bytes = password.encode("utf-8") if password else None

  try:
    with zipfile.ZipFile(zip_path, "r") as zip_ref:
      if pwd_bytes:
        zip_ref.setpassword(pwd_bytes)

      if member_patterns:
        matched_members = [
            info.filename
            for info in zip_ref.infolist()
            if any(fnmatch.fnmatch(info.filename, p) for p in member_patterns)
        ]
        if not matched_members:
          if not quiet:
            print(
                f"Error: No files matched patterns '{member_patterns}' inside"
                f" '{zip_path}'."
            )
          return False

        if not quiet:
          print(
              f"Extracting {len(matched_members)} matching files from"
              f" '{zip_path}' to '{extract_to}'..."
          )
        for member in matched_members:
          zip_ref.extract(member, extract_to)
        if not quiet:
          print("Selective extraction successful!")
      else:
        if not quiet:
          print(
              f"Extracting '{zip_path}' to '{extract_to}' and generating"
              f" {CACHE_FILENAME}..."
          )
        xml_meta_path = os.path.join(extract_to, CACHE_FILENAME)
        root = ET.Element("ZipMetadata")

        zip_ref.extractall(extract_to)
        for info in zip_ref.infolist():
          file_path = os.path.join(extract_to, info.filename)
          mtime = os.path.getmtime(file_path) if os.path.exists(file_path) else 0

          dt = info.date_time
          try:
            ts_str = f"{dt[0]:04d}-{dt[1]:02d}-{dt[2]:02d} {dt[3]:02d}:{dt[4]:02d}:{dt[5]:02d}"
          except Exception:
            ts_str = "2009-01-01 00:00:00"

          file_elem = ET.SubElement(root, "File")
          file_elem.set("filename", info.filename)
          file_elem.set("compress_type", str(info.compress_type))
          file_elem.set("external_attr", str(info.external_attr))
          file_elem.set("mtime", str(mtime))
          file_elem.set("timestamp", ts_str)
          extra_hex = info.extra.hex() if info.extra else ""
          file_elem.set("extra_hex", extra_hex)

        tree = ET.ElementTree(root)
        tree.write(xml_meta_path, encoding="utf-8", xml_declaration=True)
        if not quiet:
          print(f"Extraction successful! Metadata saved to '{xml_meta_path}'.")

    if clear_source and os.path.exists(zip_path):
      os.remove(zip_path)
      if not quiet:
        print(f"Deleted original ZIP file: '{zip_path}'")

  except Exception as e:
    if not quiet:
      print(
          "Error during extraction (password might be required or incorrect):"
          f" {e}"
      )
    return False

  return True


def apply_basic_alignment(zinfo, alignment=4):
  """Automatically calculates and applies basic padding to zinfo.extra."""
  header_base_size = 30
  filename_len = len(zinfo.filename.encode("utf-8"))
  current_extra_len = len(zinfo.extra)

  current_offset = header_base_size + filename_len + current_extra_len
  remainder = current_offset % alignment

  if remainder != 0:
    padding_needed = alignment - remainder
    zinfo.extra += b"\x00" * padding_needed


def inject_file_to_zip(
    zip_path,
    file_to_inject,
    arcname=None,
    forced_timestamp="2009-01-01 00:00:00",
    quiet=False,
):
  """Injects or updates a single file directly into an existing ZIP archive."""
  if not os.path.exists(zip_path):
    if not quiet:
      print(f"Error: Target ZIP '{zip_path}' not found.")
    return False
  if not os.path.exists(file_to_inject):
    if not quiet:
      print(f"Error: File to inject '{file_to_inject}' not found.")
    return False

  if not arcname:
    arcname = os.path.basename(file_to_inject)

  if not quiet:
    print(f"Injecting '{file_to_inject}' as '{arcname}' into '{zip_path}'...")
  parsed_ts = parse_timestamp(forced_timestamp)

  temp_zip = zip_path + ".tmp"
  with zipfile.ZipFile(zip_path, "r") as zin, zipfile.ZipFile(
      temp_zip, "w"
  ) as zout:
    for item in zin.infolist():
      if item.filename == arcname:
        continue
      zout.writestr(item, zin.read(item.filename))

    zinfo = zipfile.ZipInfo.from_file(file_to_inject, arcname)
    zinfo.compress_type = zipfile.ZIP_DEFLATED
    zinfo.date_time = parsed_ts
    apply_basic_alignment(zinfo, alignment=4)
    with open(file_to_inject, "rb") as f:
      zout.writestr(zinfo, f.read())

  os.replace(temp_zip, zip_path)
  if not quiet:
    print(f"Injection successful into '{zip_path}'.")
  return True


def repack_zip(
    source_dir,
    output_zip,
    exclude_patterns=None,
    no_compress_patterns=None,
    compression_level=6,
    update_only=False,
    forced_timestamp=None,
    quiet=False,
    clear_source=False,
    force=False,
):
  """Packs a directory into a ZIP file following cache priority or forced timestamp rules."""
  if not os.path.exists(source_dir):
    if not quiet:
      print(f"Error: Source directory '{source_dir}' not found.")
    return False

  if os.path.exists(output_zip):
    if not force:
      if not quiet:
        print(
            f"Error: Output file '{output_zip}' already exists. Use -f to"
            " overwrite."
        )
      return False
    elif os.path.isdir(output_zip):
      if not quiet:
        print(f"Error: Output path '{output_zip}' is a directory.")
      return False

  # Tự động tạo thư mục chứa file đầu ra nếu chưa tồn tại
  output_dir = os.path.dirname(os.path.abspath(output_zip))
  if output_dir:
    os.makedirs(output_dir, exist_ok=True)

  xml_meta_path = os.path.join(source_dir, CACHE_FILENAME)
  meta_map = {}

  if os.path.exists(xml_meta_path):
    if not quiet:
      print(f"Loading metadata mapping from '{xml_meta_path}'...")
    tree = ET.parse(xml_meta_path)
    root = tree.getroot()
    for file_elem in root.findall("File"):
      fname = file_elem.get("filename")
      meta_map[fname] = {
          "compress_type": int(
              file_elem.get("compress_type", zipfile.ZIP_DEFLATED)
          ),
          "external_attr": int(file_elem.get("external_attr", 0)),
          "mtime": float(file_elem.get("mtime", 0)),
          "extra": bytes.fromhex(file_elem.get("extra_hex", "")),
          "timestamp": file_elem.get("timestamp", "2009-01-01 00:00:00"),
      }
  else:
    if not quiet:
      print(
          f"Notice: No '{CACHE_FILENAME}' found. All files will use basic"
          " automatic alignment."
      )

  if not quiet:
    print(f"Packing directory '{source_dir}' into archive...")
  if os.path.exists(output_zip):
    os.remove(output_zip)

  if exclude_patterns is None:
    exclude_patterns = []
  if no_compress_patterns is None:
    no_compress_patterns = []

  forced_ts_parsed = parse_timestamp(forced_timestamp) if forced_timestamp else None

  with zipfile.ZipFile(
      output_zip,
      "w",
      zipfile.ZIP_DEFLATED,
      compresslevel=compression_level,
  ) as zipf:
    for root_dir, dirs, files in os.walk(source_dir):
      for file in files:
        file_path = os.path.join(root_dir, file)
        arcname = os.path.relpath(file_path, source_dir)
        arcname = arcname.replace(os.sep, "/")

        if arcname == CACHE_FILENAME or file == CACHE_FILENAME:
          continue

        excluded = False
        for pattern in exclude_patterns:
          if fnmatch.fnmatch(file, pattern) or fnmatch.fnmatch(arcname, pattern):
            excluded = True
            break
        if excluded:
          continue

        if update_only and arcname in meta_map:
          current_mtime = os.path.getmtime(file_path)
          stored_mtime = meta_map[arcname]["mtime"]
          if current_mtime <= stored_mtime:
            continue

        zinfo = zipfile.ZipInfo.from_file(file_path, arcname)

        if arcname in meta_map:
          zinfo.compress_type = meta_map[arcname]["compress_type"]
          zinfo.external_attr = meta_map[arcname]["external_attr"]
          zinfo.extra = meta_map[arcname]["extra"]
        else:
          zinfo.compress_type = zipfile.ZIP_DEFLATED
          apply_basic_alignment(zinfo, alignment=4)

        for pattern in no_compress_patterns:
          if fnmatch.fnmatch(file, pattern) or fnmatch.fnmatch(arcname, pattern):
            zinfo.compress_type = zipfile.ZIP_STORED
            break

        if zinfo.compress_type == zipfile.ZIP_STORED and arcname not in meta_map:
          apply_basic_alignment(zinfo, alignment=4)

        if forced_ts_parsed is not None:
          zinfo.date_time = forced_ts_parsed
        elif arcname in meta_map and "timestamp" in meta_map[arcname]:
          zinfo.date_time = parse_timestamp(meta_map[arcname]["timestamp"])
        else:
          zinfo.date_time = parse_timestamp("2009-01-01 00:00:00")

        with open(file_path, "rb") as f:
          data = f.read()

        zipf.writestr(zinfo, data)

  if not quiet:
    print(f"ZIP file successfully saved at: '{output_zip}'")

  if clear_source and os.path.exists(source_dir):
    shutil.rmtree(source_dir)
    if not quiet:
      print(f"Deleted source directory: '{source_dir}'")

  return True


def main():
  parser = argparse.ArgumentParser(
      description=(
          "Advanced Python ZIP tool supporting multi-pattern filters for list,"
          " pipe, and extraction."
      )
  )

  parser.add_argument(
      "-e",
      "--extract",
      metavar="FILE",
      help="Path to the ZIP file (supports wildcards in batch mode)",
  )
  parser.add_argument(
      "-d",
      "--dir",
      default="extracted_folder",
      metavar="DIR",
      help="Directory to extract files into",
  )
  parser.add_argument(
      "-c",
      "--clear",
      action="store_true",
      help="Clear (delete) original ZIP file after extraction, or delete source directory after repacking",
  )
  parser.add_argument(
      "-f",
      "--force",
      action="store_true",
      help="Force overwrite existing output file or target directory",
  )
  parser.add_argument(
      "-P",
      "--password",
      metavar="PWD",
      help="Password for encrypted ZIP files",
  )
  parser.add_argument(
      "-q",
      "--quiet",
      action="store_true",
      help="Quiet mode (suppresses logs during extraction or repacking)",
  )
  parser.add_argument(
      "-p",
      "--pipe",
      nargs="+",
      metavar="PATTERN",
      help="Extract multiple file(s) matching pattern(s) directly to stdout",
  )
  parser.add_argument(
      "-r", "--repack", metavar="DIR", help="Directory to pack into a ZIP file"
  )
  parser.add_argument(
      "-o", "--output", metavar="FILE", help="Output ZIP file name"
  )
  parser.add_argument(
      "-x",
      "--exclude",
      nargs="*",
      metavar="PATTERN",
      help="Files or patterns to exclude",
  )
  parser.add_argument(
      "-n",
      "--no-compress",
      nargs="*",
      metavar="PATTERN",
      help="Files or patterns to store without compression",
  )
  parser.add_argument(
      "-l",
      "--compression-level",
      type=int,
      default=6,
      metavar="LEVEL",
      help="Compression level (0-9)",
  )
  parser.add_argument(
      "--batch",
      action="store_true",
      help="Process multiple files matching pattern in extract mode",
  )
  parser.add_argument(
      "--update",
      action="store_true",
      help="Smart incremental repack: only pack files modified since extraction",
  )
  parser.add_argument(
      "--inject",
      nargs=2,
      metavar=("FILE", "TARGET_ZIP"),
      help="Directly inject/replace a single file into an existing ZIP archive",
  )
  parser.add_argument(
      "--timestamp",
      default=None,
      metavar="YYYY-MM-DD HH:MM:SS",
      help="Override modification timestamp for all files during repack",
  )
  parser.add_argument(
      "--list",
      nargs="+",
      metavar="ARGS",
      help="List contents: first argument is ZIP file, followed by optional patterns",
  )
  parser.add_argument(
      "--member",
      nargs="+",
      metavar="PATTERN",
      help="Extract specific file(s) or pattern(s) matching these arguments",
  )

  args = parser.parse_args()

  if args.list:
    zip_file = args.list[0]
    patterns = args.list[1:] if len(args.list) > 1 else None
    list_zip_contents(
        zip_file, password=args.password, quiet=args.quiet, patterns=patterns
    )
  elif args.pipe:
    target_zip = (
        args.extract if args.extract else (args.list[0] if args.list else "")
    )
    if not target_zip:
      print(
          "Error: Please specify target ZIP file using -e or --list alongside"
          " -p.",
          file=sys.stderr,
      )
    else:
      extract_to_stdout(target_zip, args.pipe, password=args.password)
  elif args.inject:
    inject_file_to_zip(
        args.inject[1],
        args.inject[0],
        forced_timestamp=args.timestamp if args.timestamp else "2009-01-01 00:00:00",
        quiet=args.quiet,
    )
  elif args.extract:
    if args.batch:
      files = glob.glob(args.extract)
      for f in files:
        base_name = os.path.splitext(os.path.basename(f))[0]
        out_dir = os.path.join(args.dir, base_name)
        extract_zip(
            f,
            out_dir,
            password=args.password,
            quiet=args.quiet,
            clear_source=args.clear,
            force=args.force,
        )
    else:
      extract_zip(
          args.extract,
          args.dir,
          password=args.password,
          member_patterns=args.member,
          quiet=args.quiet,
          clear_source=args.clear,
          force=args.force,
      )
  elif args.repack:
    output_file = args.output if args.output else args.repack + ".zip"
    repack_zip(
        args.repack,
        output_file,
        exclude_patterns=args.exclude,
        no_compress_patterns=args.no_compress,
        compression_level=args.compression_level,
        update_only=args.update,
        forced_timestamp=args.timestamp,
        quiet=args.quiet,
        clear_source=args.clear,
        force=args.force,
    )
  else:
    parser.print_help()


if __name__ == "__main__":
  main()
