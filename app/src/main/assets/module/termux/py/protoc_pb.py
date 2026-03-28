#!/data/data/com.tool.tree/files/home/termux/bin/python
# -*- coding: utf-8 -*-

import json
import argparse
import sys
import os
import xml.etree.ElementTree as ET
from google.protobuf.internal.decoder import _DecodeVarint
from google.protobuf.internal.encoder import _VarintEncoder

# ---------- WIRE TYPES ----------

WIRE_VARINT = 0
WIRE_64BIT  = 1
WIRE_LEN    = 2
WIRE_32BIT  = 5


# ---------- VARINT ----------

def encode_varint(v: int) -> bytes:
    out = []
    _VarintEncoder()(out.append, v, False)
    return b"".join(out)


def decode_varint(buf, pos):
    v, pos = _DecodeVarint(buf, pos)
    return v, pos


# ---------- BYTES <-> TEXT (BYTE-EXACT) ----------

def bytes_to_text(raw: bytes) -> str:
    return raw.decode("latin1").encode("unicode_escape").decode("ascii")


def text_to_bytes(text: str) -> bytes:
    return text.encode("ascii").decode("unicode_escape").encode("latin1")


# ---------- TEXT <-> XML VIEW ----------

def text_to_xml(text: str) -> str:
    return text.replace("\\n", "\n")


def xml_to_text(text: str) -> str:
    return text.replace("\n", "\\n")


# ---------- DECODE PB ----------

def decode_message(buf: bytes):
    pos = 0
    out = []

    while pos < len(buf):
        tag, pos = decode_varint(buf, pos)
        field = tag >> 3
        wire  = tag & 0x7

        entry = {"field": field, "wire": wire}

        if wire == WIRE_VARINT:
            val, pos = decode_varint(buf, pos)
            entry["text"] = str(val)

        elif wire == WIRE_64BIT:
            raw = buf[pos:pos+8]
            pos += 8
            entry["text"] = bytes_to_text(raw)

        elif wire == WIRE_32BIT:
            raw = buf[pos:pos+4]
            pos += 4
            entry["text"] = bytes_to_text(raw)

        elif wire == WIRE_LEN:
            size, pos = decode_varint(buf, pos)
            raw = buf[pos:pos+size]
            pos += size
            entry["text"] = bytes_to_text(raw)

        else:
            raise ValueError("Unknown wire type")

        out.append(entry)

    return out


# ---------- ENCODE PB ----------

def encode_message(entries):
    out = b""

    for e in entries:
        field = int(e["field"])
        wire  = int(e["wire"])
        text  = e["text"]

        tag = (field << 3) | wire
        out += encode_varint(tag)

        if wire == WIRE_VARINT:
            out += encode_varint(int(text))

        elif wire in (WIRE_32BIT, WIRE_64BIT):
            out += text_to_bytes(text)

        elif wire == WIRE_LEN:
            raw = text_to_bytes(text)
            out += encode_varint(len(raw))
            out += raw

        else:
            raise ValueError("Unknown wire type")

    return out


# ---------- JSON ----------

def json_dump(data):
    print(json.dumps(data, indent=2, ensure_ascii=False))


def json_load(path):
    return json.load(open(path, "r", encoding="utf-8"))


# ---------- XML ----------

def xml_dump(entries):
    root = ET.Element("protobuf")

    for e in entries:
        item = ET.SubElement(
            root, "entry",
            field=str(e["field"]),
            wire=str(e["wire"])
        )
        text = ET.SubElement(item, "text")
        text.text = text_to_xml(e["text"])

    ET.indent(root, space="  ")
    ET.ElementTree(root).write(
        sys.stdout, encoding="unicode", xml_declaration=False
    )


def xml_load(path):
    tree = ET.parse(path)
    root = tree.getroot()
    out = []

    for item in root.findall("entry"):
        text = item.findtext("text", "")
        out.append({
            "field": int(item.attrib["field"]),
            "wire": int(item.attrib["wire"]),
            "text": xml_to_text(text)
        })

    return out


# ---------- CLI ----------

def main():
    ap = argparse.ArgumentParser(
        description="Protobuf TEXT-only codec (JSON / XML / PB, byte-exact)"
    )
    ap.add_argument("-d", "--decode", help="decode pb -> json/xml")
    ap.add_argument("-e", "--encode", help="encode json/xml -> pb")
    ap.add_argument("--xml", action="store_true", help="use XML instead of JSON")
    ap.add_argument("-o", "--out", help="output pb file")
    ap.add_argument("-c", "--delete_input", action="store_true", help="delete input file after processing")
    args = ap.parse_args()

    # ---- DECODE ----
    if args.decode:
        in_file = args.decode
        data = open(in_file, "rb").read()
        decoded = decode_message(data)
        xml_dump(decoded) if args.xml else json_dump(decoded)

        if args.delete_input:
            try:
                os.remove(in_file)
            except Exception as e:
                print(f"[!] Failed to delete {in_file}: {e}", file=sys.stderr)
        return

    # ---- ENCODE ----
    if args.encode:
        in_file = args.encode
        entries = xml_load(in_file) if args.xml else json_load(in_file)
        pb = encode_message(entries)

        if args.out:
            open(args.out, "wb").write(pb)
        else:
            print(pb.hex())

        if args.delete_input:
            try:
                os.remove(in_file)
            except Exception as e:
                print(f"[!] Failed to delete {in_file}: {e}", file=sys.stderr)
        return

    ap.print_help()


if __name__ == "__main__":
    main()