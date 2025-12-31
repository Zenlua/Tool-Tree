from __future__ import annotations

import string
from typing import TYPE_CHECKING, ClassVar

from dissect.cstruct.exceptions import ExpressionParserError, ExpressionTokenizerError

if TYPE_CHECKING:
    from collections.abc import Callable

    from dissect.cstruct import cstruct


HEXBIN_SUFFIX = {"x", "X", "b", "B"}


class ExpressionTokenizer:
    def __init__(self, expression: str):
        self.expression = expression
        self.pos = 0
        self.tokens = []

    def equal(self, token: str, expected: str | set[str]) -> bool:
        if isinstance(expected, set):
            return token in expected
        return token == expected

    def alnum(self, token: str) -> bool:
        return token.isalnum()

    def alpha(self, token: str) -> bool:
        return token.isalpha()

    def digit(self, token: str) -> bool:
        return token.isdigit()

    def hexdigit(self, token: str) -> bool:
        return token in string.hexdigits

    def operator(self, token: str) -> bool:
        return token in {"*", "/", "+", "-", "%", "&", "^", "|", "(", ")", "~"}

    def match(
        self,
        func: Callable[[str], bool] | None = None,
        expected: str | None = None,
        consume: bool = True,
        append: bool = True,
    ) -> bool:
        if self.eol():
            return False

        token = self.get_token()

        if expected and self.equal(token, expected):
            if append:
                self.tokens.append(token)
            if consume:
                self.consume()
            return True

        if func and func(token):
            if append:
                self.tokens.append(token)
            if consume:
                self.consume()
            return True

        return False

    def consume(self) -> None:
        self.pos += 1

    def eol(self) -> bool:
        return self.pos >= len(self.expression)

    def get_token(self) -> str:
        if self.eol():
            raise ExpressionTokenizerError(f"Out of bounds index: {self.pos}, length: {len(self.expression)}")
        return self.expression[self.pos]

    def tokenize(self) -> list[str]:
        token = ""

        # Loop over expression runs in linear time
        while not self.eol():
            # If token is a single character operand add it to tokens
            if self.match(self.operator):
                continue

            # If token is a single digit, keep looping over expression and build the number
            if self.match(self.digit, consume=False, append=False):
                token += self.get_token()
                self.consume()

                # Support for binary and hexadecimal notation
                if self.match(expected=HEXBIN_SUFFIX, consume=False, append=False):
                    token += self.get_token()
                    self.consume()

                while self.match(self.hexdigit, consume=False, append=False):
                    token += self.get_token()
                    self.consume()
                    if self.eol():
                        break

                # Checks for suffixes in numbers
                if self.match(expected={"u", "U"}, consume=False, append=False):
                    self.consume()
                    self.match(expected={"l", "L"}, append=False)
                    self.match(expected={"l", "L"}, append=False)

                elif self.match(expected={"l", "L"}, append=False):
                    self.match(expected={"l", "L"}, append=False)
                    self.match(expected={"u", "U"}, append=False)
                else:
                    pass

                # Number cannot end on x or b in the case of binary or hexadecimal notation
                if len(token) == 2 and token[-1] in HEXBIN_SUFFIX:
                    raise ExpressionTokenizerError("Invalid binary or hex notation")

                if len(token) > 1 and token[0] == "0" and token[1] not in HEXBIN_SUFFIX:
                    token = token[:1] + "o" + token[1:]
                self.tokens.append(token)
                token = ""

            # If token is alpha or underscore we need to build the identifier
            elif self.match(self.alpha, consume=False, append=False) or self.match(
                expected="_", consume=False, append=False
            ):
                while self.match(self.alnum, consume=False, append=False) or self.match(
                    expected="_", consume=False, append=False
                ):
                    token += self.get_token()
                    self.consume()
                    if self.eol():
                        break
                self.tokens.append(token)
                token = ""
            # If token is length 2 operand make sure next character is part of length 2 operand append to tokens
            elif self.match(expected=">", append=False) and self.match(expected=">", append=False):
                self.tokens.append(">>")
            elif self.match(expected="<", append=False) and self.match(expected="<", append=False):
                self.tokens.append("<<")
            elif self.match(expected={" ", "\n", "\t"}, append=False):
                continue
            else:
                raise ExpressionTokenizerError(
                    f"Tokenizer does not recognize following token '{self.expression[self.pos]}'"
                )
        return self.tokens


class Expression:
    """Expression parser for calculations in definitions."""

    binary_operators: ClassVar[dict[str, Callable[[int, int], int]]] = {
        "|": lambda a, b: a | b,
        "^": lambda a, b: a ^ b,
        "&": lambda a, b: a & b,
        "<<": lambda a, b: a << b,
        ">>": lambda a, b: a >> b,
        "+": lambda a, b: a + b,
        "-": lambda a, b: a - b,
        "*": lambda a, b: a * b,
        "/": lambda a, b: a // b,
        "%": lambda a, b: a % b,
    }

    unary_operators: ClassVar[dict[str, Callable[[int], int]]] = {
        "u": lambda a: -a,
        "~": lambda a: ~a,
    }

    precedence_levels: ClassVar[dict[str, int]] = {
        "|": 0,
        "^": 1,
        "&": 2,
        "<<": 3,
        ">>": 3,
        "+": 4,
        "-": 4,
        "*": 5,
        "/": 5,
        "%": 5,
        "u": 6,
        "~": 6,
        "sizeof": 6,
    }

    def __init__(self, expression: str):
        self.expression = expression
        self.tokens = ExpressionTokenizer(expression).tokenize()
        self.stack = []
        self.queue = []

    def __repr__(self) -> str:
        return self.expression

    def precedence(self, o1: str, o2: str) -> bool:
        return self.precedence_levels[o1] >= self.precedence_levels[o2]

    def evaluate_exp(self) -> None:
        operator = self.stack.pop(-1)
        res = 0

        if len(self.queue) < 1:
            raise ExpressionParserError("Invalid expression: not enough operands")

        right = self.queue.pop(-1)
        if operator in self.unary_operators:
            res = self.unary_operators[operator](right)
        else:
            if len(self.queue) < 1:
                raise ExpressionParserError("Invalid expression: not enough operands")

            left = self.queue.pop(-1)
            res = self.binary_operators[operator](left, right)

        self.queue.append(res)

    def is_number(self, token: str) -> bool:
        return token.isnumeric() or (len(token) > 2 and token[0] == "0" and token[1] in ("x", "X", "b", "B", "o", "O"))

    def evaluate(self, cs: cstruct, context: dict[str, int] | None = None) -> int:
        """Evaluates an expression using a Shunting-Yard implementation."""

        self.stack = []
        self.queue = []
        operators = set(self.binary_operators.keys()) | set(self.unary_operators.keys())

        context = context or {}
        tmp_expression = self.tokens

        # Unary minus tokens; we change the semantic of '-' depending on the previous token
        for i in range(len(self.tokens)):
            if self.tokens[i] == "-":
                if i == 0:
                    self.tokens[i] = "u"
                    continue
                if self.tokens[i - 1] in operators or self.tokens[i - 1] == "u" or self.tokens[i - 1] == "(":
                    self.tokens[i] = "u"
                    continue

        i = 0
        while i < len(tmp_expression):
            current_token = tmp_expression[i]
            if self.is_number(current_token):
                self.queue.append(int(current_token, 0))
            elif current_token in context:
                self.queue.append(int(context[current_token]))
            elif current_token in cs.consts:
                self.queue.append(int(cs.consts[current_token]))
            elif current_token in self.unary_operators:
                self.stack.append(current_token)
            elif current_token == "sizeof":
                if len(tmp_expression) < i + 3 or (tmp_expression[i + 1] != "(" or tmp_expression[i + 3] != ")"):
                    raise ExpressionParserError("Invalid sizeof operation")
                self.queue.append(len(cs.resolve(tmp_expression[i + 2])))
                i += 3
            elif current_token in operators:
                while (
                    len(self.stack) != 0 and self.stack[-1] != "(" and (self.precedence(self.stack[-1], current_token))
                ):
                    self.evaluate_exp()
                self.stack.append(current_token)
            elif current_token == "(":
                if i > 0:
                    previous_token = tmp_expression[i - 1]
                    if self.is_number(previous_token):
                        raise ExpressionParserError(
                            f"Parser expected sizeof or an arethmethic operator instead got: '{previous_token}'"
                        )

                self.stack.append(current_token)
            elif current_token == ")":
                if i > 0:
                    previous_token = tmp_expression[i - 1]
                    if previous_token == "(":
                        raise ExpressionParserError(
                            f"Parser expected an expression, instead received empty parenthesis. Index: {i}"
                        )

                if len(self.stack) == 0:
                    raise ExpressionParserError("Invalid expression")

                while self.stack[-1] != "(":
                    self.evaluate_exp()

                self.stack.pop(-1)
            else:
                raise ExpressionParserError(f"Unmatched token: '{current_token}'")
            i += 1

        while len(self.stack) != 0:
            if self.stack[-1] == "(":
                raise ExpressionParserError("Invalid expression")

            self.evaluate_exp()

        if len(self.queue) != 1:
            raise ExpressionParserError("Invalid expression")

        return self.queue[0]
