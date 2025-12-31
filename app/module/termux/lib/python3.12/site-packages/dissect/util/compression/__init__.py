from dissect.util.compression import lz4, lzo

lz4_python = lz4
lzo_python = lzo

# This selects between a native Rust version of lz4 and lzo (when available) and our own
# pure-Python implementation.
#
# By doing a:
#  from dissect.util.compression import lz4
# or
#  from dissect.util.compression import lzo
#
# in another project will automatically give you one or the other.
#
# The native Rust version is also available as dissect.util.compression.lz4_native
# and dissect.util.compression.lzo_native (when available) and the pure Python
# version is always available as dissect.util.compression.lz4_python and
# dissect.util.compression.lzo_python.
#
# Note that the pure Python implementation and the Rust implementation are NOT a full replacement
# for the "official" lz4 and lzo Python packages: only the decompress() function is implemented.
try:
    from dissect.util import _native

    lz4 = lz4_native = _native.compression.lz4
    lzo = lzo_native = _native.compression.lzo
except (ImportError, AttributeError):
    lz4_native = lzo_native = None

__all__ = [
    "lz4",
    "lz4_native",
    "lz4_python",
    "lzbitmap",
    "lzfse",
    "lznt1",
    "lzo",
    "lzo_native",
    "lzo_python",
    "lzvn",
    "lzxpress",
    "lzxpress_huffman",
    "sevenbit",
]
