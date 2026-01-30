# https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-frs2/8cb5bae9-edf3-4833-9f0a-9d7e24218d3d
# https://winprotocoldoc.blob.core.windows.net/productionwindowsarchives/MS-XCA/[MS-XCA].pdf
from __future__ import annotations

import io
import struct
from typing import BinaryIO, NamedTuple


class Symbol(NamedTuple):
    length: int
    symbol: int


def _read_16_bit(fh: BinaryIO) -> int:
    return struct.unpack("<H", fh.read(2).rjust(2, b"\x00"))[0]


class Node:
    __slots__ = ("children", "is_leaf", "symbol")

    def __init__(self, symbol: Symbol | None = None, is_leaf: bool = False):
        self.symbol = symbol
        self.is_leaf = is_leaf
        self.children = [None, None]


def _add_leaf(nodes: list[Node], idx: int, mask: int, bits: int) -> int:
    node = nodes[0]
    i = idx + 1

    while bits > 1:
        bits -= 1
        childidx = (mask >> bits) & 1
        if node.children[childidx] is None:
            node.children[childidx] = nodes[i]
            nodes[i].is_leaf = False
            i += 1
        node = node.children[childidx]

    node.children[mask & 1] = nodes[idx]
    return i


def _build_tree(buf: bytes) -> Node:
    if len(buf) != 256:
        raise ValueError("Not enough data for Huffman code tree")

    nodes = [Node() for _ in range(1024)]
    symbols: list[Symbol] = []

    for i, c in enumerate(buf):
        symbols.append(Symbol(c & 0x0F, i * 2))
        symbols.append(Symbol((c >> 4) & 0x0F, i * 2 + 1))

    symbols = sorted(symbols)

    symbol_index_start = 0
    for s in symbols:
        if s.length > 0:
            break
        symbol_index_start += 1

    mask = 0
    bits = 1

    root = nodes[0]

    tree_index = 1
    for symbol_index in range(symbol_index_start, 512):
        s = symbols[symbol_index]

        node = nodes[tree_index]
        node.symbol = s.symbol
        node.is_leaf = True

        mask = (mask << s.length - bits) & 0xFFFFFFFF
        bits = s.length

        tree_index = _add_leaf(nodes, tree_index, mask, bits)
        mask += 1

    return root


class BitString:
    def __init__(self):
        self.source = None
        self.mask = 0
        self.bits = 0

    @property
    def index(self) -> int:
        return self.source.tell()

    def init(self, fh: BinaryIO) -> None:
        self.mask = (_read_16_bit(fh) << 16) + _read_16_bit(fh)
        self.bits = 32
        self.source = fh

    def read(self, n: int) -> bytes:
        return self.source.read(n)

    def lookup(self, n: int) -> int:
        if n == 0:
            return 0

        return self.mask >> (32 - n)

    def skip(self, n: int) -> None:
        self.mask = (self.mask << n) & 0xFFFFFFFF
        self.bits -= n
        if self.bits < 16:
            self.mask += _read_16_bit(self.source) << (16 - self.bits)
            self.bits += 16

    def decode(self, root: Node) -> Symbol:
        node = root
        while not node.is_leaf:
            bit = self.lookup(1)
            self.skip(1)
            node = node.children[bit]
        return node.symbol


def decompress(src: bytes | BinaryIO) -> bytes:
    """LZXPRESS decompress from a file-like object or bytes.

    Decompresses until EOF of the input data.

    Args:
        src: File-like object or bytes to decompress.

    Returns:
        The decompressed data.
    """
    if not hasattr(src, "read"):
        src = io.BytesIO(src)

    dst = bytearray()

    start_offset = src.tell()
    src.seek(0, io.SEEK_END)
    size = src.tell() - start_offset
    src.seek(start_offset, io.SEEK_SET)

    bitstring = BitString()

    while src.tell() - start_offset < size:
        root = _build_tree(src.read(256))
        bitstring.init(src)

        chunk_size = 0
        while chunk_size < 65536 and src.tell() - start_offset < size:
            symbol = bitstring.decode(root)
            if symbol < 256:
                dst.append(symbol)
                chunk_size += 1
            else:
                symbol -= 256
                length = symbol & 0x0F
                symbol >>= 4

                offset = (1 << symbol) + bitstring.lookup(symbol)

                if length == 15:
                    length = ord(bitstring.read(1)) + 15

                    if length == 270:
                        length = _read_16_bit(bitstring.source)

                bitstring.skip(symbol)

                length += 3

                remaining = length
                while remaining > 0:
                    match_size = min(remaining, offset)
                    dst += dst[-offset : (-offset + match_size) or None]
                    remaining -= match_size

                chunk_size += length

    return bytes(dst)
