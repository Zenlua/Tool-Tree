from __future__ import annotations

from enum import IntFlag

from dissect.cstruct.types.base import BaseType
from dissect.cstruct.types.enum import PY_311, EnumMetaType


class Flag(BaseType, IntFlag, metaclass=EnumMetaType):
    """Flag type supercharged with cstruct functionality.

    Flags are (mostly) compatible with the Python 3 standard library ``IntFlag`` with some notable differences:
        - Flag members are only considered equal if the flag class is the same

    Flags can be made using any integer type.

    Example:
        When using the default C-style parser, the following syntax is supported::

            flag <name> [: <type>] {
                <values>
            };

        For example, a flag that has A=1, B=4 and C=8 could be written like so::

            flag Test : uint16 {
                A, B=4, C
            };
    """

    def __repr__(self) -> str:
        result = super().__repr__()
        if not self.__class__.__name__:
            # Deal with anonymous flags by stripping off the first bit
            # I.e. <.RED: 1> -> <RED: 1>
            result = f"<{result[2:]}"
        return result

    if PY_311:

        def __str__(self) -> str:
            # We differentiate with standard Python flags in that we use a more descriptive str representation
            # Standard Python flags just use the integer value as str, we use FlagName.ValueName
            # In case of anonymous flags, we just use the ValueName
            base = f"{self.__class__.__name__}." if self.__class__.__name__ else ""
            return f"{base}{self.name}"

    else:

        def __str__(self) -> str:
            result = IntFlag.__str__(self)
            if not self.__class__.__name__:
                # Deal with anonymous flags
                # I.e. .RED -> RED
                result = result[1:]
            return result

    def __eq__(self, other: int | Flag) -> bool:
        if isinstance(other, Flag) and other.__class__ is not self.__class__:
            return False

        # Python <= 3.10 compatibility
        if isinstance(other, Flag):
            other = other.value

        return self.value == other

    def __ne__(self, value: int | Flag) -> bool:
        return not self.__eq__(value)

    def __hash__(self) -> int:
        return hash((self.__class__, self.name, self.value))
