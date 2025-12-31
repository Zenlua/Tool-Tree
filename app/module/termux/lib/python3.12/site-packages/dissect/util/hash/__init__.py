from dissect.util.hash import crc32c

crc32c_python = crc32c

# This selects between a native Rust version of crc32c (when available) and our own
# pure-Python implementation.
#
# By doing a:
#  from dissect.util.hash import crc32c
#
# in another project will automatically give you one or the other.
#
# The native Rust version is also available as dissect.util.hash.crc32c_native (when available)
# and the pure Python version is always available as dissect.util.hash.crc32c_python.
try:
    from dissect.util import _native

    crc32c = crc32c_native = _native.hash.crc32c
except (ImportError, AttributeError):
    crc32c_native = None

__all__ = [
    "crc32",
    "crc32c",
    "crc32c_native",
    "crc32c_python",
    "jenkins",
]
