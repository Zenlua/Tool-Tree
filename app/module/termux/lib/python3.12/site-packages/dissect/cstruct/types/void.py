from __future__ import annotations

from typing import TYPE_CHECKING, Any, BinaryIO

from dissect.cstruct.types.base import BaseArray, BaseType

if TYPE_CHECKING:
    from typing_extensions import Self


class VoidArray(list, BaseArray):
    """Array type representing void elements, primarily used for no-op reading and writing operations."""

    @classmethod
    def __default__(cls) -> Self:
        return cls()

    @classmethod
    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:
        return cls()

    @classmethod
    def _write(cls, stream: BinaryIO, data: bytes) -> int:
        return 0


class Void(BaseType):
    """Void type."""

    ArrayType = VoidArray

    def __bool__(self) -> bool:
        return False

    def __eq__(self, value: object) -> bool:
        return isinstance(value, Void)

    @classmethod
    def _read(cls, stream: BinaryIO, context: dict[str, Any] | None = None) -> Self:
        return cls.__new__(cls)

    @classmethod
    def _write(cls, stream: BinaryIO, data: Void) -> int:
        return 0
