from __future__ import annotations

import struct
import sys
from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:
    from collections.abc import Iterator


def xmemoryview(view: bytes, format: str) -> memoryview | _xmemoryview:
    """Cast a memoryview to the specified format, including endianness.

    The regular ``memoryview.cast()`` method only supports host endianness. While that should be fine 99% of the time
    (most of the world runs on little endian systems), we'd rather it be fine 100% of the time. This utility method
    ensures that by transparently converting between endianness if it doesn't match the host endianness.

    If the host endianness matches the requested endianness, this simply returns a regular ``memoryview.cast()``.

    See ``memoryview.cast()`` for more details on what that actually does.

    Args:
        view: The bytes object or memoryview to cast.
        format: The format to cast to in ``struct`` format syntax.

    Raises:
        ValueError: If the format is invalid.
        TypeError: If the view is of an invalid type.
    """
    if len(format) != 2:
        raise ValueError("Invalid format specification")

    if isinstance(view, bytes | bytearray):
        view = memoryview(view)

    if not isinstance(view, memoryview):
        raise TypeError("view must be a memoryview, bytes or bytearray object")

    endian = format[0]
    view = view.cast(format[1])

    if (
        endian in ("@", "=")
        or (sys.byteorder == "little" and endian == "<")
        or (sys.byteorder == "big" and endian in (">", "!"))
    ):
        # Native endianness, don't need to do anything
        return view

    # Non-native endianness
    return _xmemoryview(view, format)


class _xmemoryview:  # noqa: N801
    """Wrapper for memoryview that converts between host and a different destination endianness.

    Args:
        view: The (already casted) memoryview to wrap.
        format: The format to convert to.
    """

    def __init__(self, view: memoryview, format: str):
        self._format = format

        fmt = format[1]
        self._view = view
        self._struct_frm = struct.Struct(f"={fmt}")
        self._struct_to = struct.Struct(format)

    def tolist(self) -> list[int]:
        return self._convert_from_native(self._view.tolist())

    def _convert_from_native(self, value: list[int] | int) -> int:
        if isinstance(value, list):
            endian = self._format[0]
            fmt = self._format[1]
            pck = f"{len(value)}{fmt}"
            return list(struct.unpack(f"{endian}{pck}", struct.pack(f"={pck}", *value)))
        return self._struct_to.unpack(self._struct_frm.pack(value))[0]

    def _convert_to_native(self, value: list[int] | int) -> int:
        if isinstance(value, list):
            endian = self._format[0]
            fmt = self._format[1]
            pck = f"{len(value)}{fmt}"
            return list(struct.unpack(f"={pck}", struct.pack(f"{endian}{pck}", *value)))
        return self._struct_frm.unpack(self._struct_to.pack(value))[0]

    def __getitem__(self, idx: int | slice) -> int | bytes:
        value = self._view[idx]
        if isinstance(idx, int):
            return self._convert_from_native(value)
        if isinstance(idx, slice):
            return _xmemoryview(self._view[idx], self._format)

        raise TypeError("Invalid index type")

    def __setitem__(self, idx: int | slice, value: list[int] | int) -> None:
        if isinstance(idx, int | slice):
            self._view[idx] = self._convert_to_native(value)
        else:
            raise TypeError("Invalid index type")

    def __len__(self) -> int:
        return len(self._view)

    def __eq__(self, other: memoryview | _xmemoryview) -> bool:
        if isinstance(other, _xmemoryview):
            other = other._view
        return self._view.__eq__(other)

    def __iter__(self) -> Iterator[int]:
        for value in self._view:
            yield self._convert_from_native(value)

    def __getattr__(self, attr: str) -> Any:
        return getattr(self._view, attr)
