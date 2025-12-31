from __future__ import annotations

import operator
import re
from enum import Enum

from dissect.util.exceptions import Error


class InvalidQueryError(Error):
    pass


class LogicalOperator(Enum):
    AND = "&"
    OR = "|"
    NOT = "!"


_LOGICAL_OPERATORS = tuple(op.value for op in LogicalOperator)


class ComparisonOperator(Enum):
    GE = ">="
    LE = "<="
    GT = ">"
    LT = "<"
    EQ = "="
    APPROX = "~="
    BIT = ":="
    EXTENDED = ":"


_NORMAL_COMPARISON_OPERATORS = [op for op in ComparisonOperator if op != ComparisonOperator.EXTENDED]
_SORTED_COMPARISON_OPERATORS = sorted(_NORMAL_COMPARISON_OPERATORS, key=lambda op: len(op.value), reverse=True)

_RE_EXTENDED = re.compile(r"(.+?):(.+?):=(.+)?")


class SearchFilter:
    """Represents an LDAP search filter (simple or nested).

    Args:
        query: The LDAP search filter string.
    """

    def __init__(self, query: str) -> None:
        self.query: str = query

        self.children: list[SearchFilter] = []
        self.operator: LogicalOperator | ComparisonOperator | None = None
        self.attribute: str | None = None
        self.value: str | None = None
        self._extended_rule: str | None = None

        _validate_syntax(query)

        if query[1:-1].startswith(_LOGICAL_OPERATORS):
            self._parse_nested()
        else:
            self._parse_simple()

    def __repr__(self) -> str:
        if self.is_nested():
            return f"<SearchFilter nested operator={self.operator.value!r} children={self.children}>"
        return f"<SearchFilter attribute={self.attribute!r} operator={self.operator.value!r} value={self.value}>"

    @classmethod
    def parse(cls, query: str, optimize: bool = True) -> SearchFilter:
        """Parse an LDAP query into a filter object, with optional optimization."""
        result = cls(query)
        if optimize:
            return optimize_ldap_query(result)[0]
        return result

    def is_nested(self) -> bool:
        """Return whether the filter is nested (i.e., contains logical operators and child filters)."""
        return isinstance(self.operator, LogicalOperator)

    def format(self) -> str:
        """Format the search filter back into an LDAP query string."""
        if self.is_nested():
            childs = "".join([child.format() for child in self.children])
            return f"({self.operator.value}{childs})"

        if self.operator == ComparisonOperator.EXTENDED:
            return f"({self.attribute}:{self._extended_rule}:={self.value})"

        return f"({self.attribute}{self.operator.value}{self.value})"

    def _parse_simple(self) -> None:
        """Parse simple filter."""
        query = self.query[1:-1]

        # Check for extended matching rules first
        if ":" in query and (match := _RE_EXTENDED.match(query)):
            self.operator = ComparisonOperator.EXTENDED
            self.attribute, self._extended_rule, self.value = match.groups()
            return

        # Regular operator parsing
        test = query
        operators: list[ComparisonOperator] = []
        for op in _SORTED_COMPARISON_OPERATORS:
            if op.value not in test:
                continue

            if test.count(op.value) > 1:
                raise InvalidQueryError(f"Comparison operator {op.value} found multiple times in query: {self.query}")

            operators.append(op)
            test = test.replace(op.value, "")

        if len(operators) == 0:
            raise InvalidQueryError(
                f"No comparison operator found in query: {self.query}. "
                f"Expected one of {[op.value for op in _NORMAL_COMPARISON_OPERATORS]}."
            )

        if len(operators) > 1:
            raise InvalidQueryError(
                f"Multiple comparison operators found in query: {self.query} -> {[o.value for o in operators]} "
                f"Expected only one of {[op.value for op in _NORMAL_COMPARISON_OPERATORS]}."
            )

        self.operator = operators[0]
        self.attribute, _, self.value = query.partition(self.operator.value)

    def _parse_nested(self) -> None:
        """Parse nested filter."""
        query = self.query[1:-1]
        self.operator = LogicalOperator(query[0])

        start = 1
        while start < len(query):
            end = start + 1
            depth = 1

            while end < len(query) and depth > 0:
                if query[end] == "(":
                    depth += 1
                elif query[end] == ")":
                    depth -= 1
                end += 1

            self.children.append(SearchFilter(query[start:end]))
            start = end


_ATTRIBUTE_WEIGHTS = {
    "objectGUID": 1,
    "distinguishedName": 1,
    "sAMAccountName": 2,
    "userPrincipalName": 2,
    "mail": 2,
    "sAMAccountType": 3,
    "servicePrincipalName": 3,
    "userAccountControl": 4,
    "memberOf": 5,
    "member": 5,
    "pwdLastSet": 5,
    "primaryGroupID": 6,
    "whenCreated": 6,
    "ou": 6,
    "lastLogonTimestamp": 6,
    "cn": 7,
    "givenName": 7,
    "name": 7,
    "telephoneNumber": 7,
    "objectCategory": 8,
    "description": 9,
    "objectClass": 10,
}


def optimize_ldap_query(query: SearchFilter) -> tuple[SearchFilter, int]:
    """Optimize an LDAP query in-place.

    Removes redundant conditions and sorts filters and conditions based on how specific they are.

    Args:
        query: The LDAP query to optimize.

    Returns:
        A tuple containing the optimized LDAP query and its weight.
    """
    # Simplify single-child AND/OR
    if query.is_nested() and len(query.children) == 1 and query.operator in (LogicalOperator.AND, LogicalOperator.OR):
        return optimize_ldap_query(query.children[0])

    # Sort nested children by weight
    if query.is_nested() and len(query.children) > 1:
        children = sorted((optimize_ldap_query(child) for child in query.children), key=operator.itemgetter(1))

        query.children = [child for child, _ in children]
        query.query = query.format()

        return query, max(weight for _, weight in children)

    # Handle NOT
    if query.is_nested() and len(query.children) == 1 and query.operator == LogicalOperator.NOT:
        child, weight = optimize_ldap_query(query.children[0])

        query.children[0] = child
        query.query = query.format()

        return query, weight

    # Base case: simple filter
    if not query.is_nested():
        return query, _ATTRIBUTE_WEIGHTS.get(query.attribute, max(_ATTRIBUTE_WEIGHTS.values()))

    return query, max(_ATTRIBUTE_WEIGHTS.values())


def _validate_syntax(query: str) -> None:
    """Validate basic LDAP query syntax.

    Args:
        query: The LDAP query to validate.
    """
    if not query:
        raise InvalidQueryError("Empty query")

    if not query.startswith("(") or not query.endswith(")"):
        raise InvalidQueryError(f"Query must be wrapped in parentheses: {query}")

    if query.count("(") != query.count(")"):
        raise InvalidQueryError(f"Unbalanced parentheses in query: {query}")

    # Check for empty parentheses
    if "()" in query:
        raise InvalidQueryError(f"Empty parentheses found in query: {query}")

    # Check for queries that start with double opening parentheses
    if query.startswith("(("):
        raise InvalidQueryError(f"Invalid query structure: {query}")
