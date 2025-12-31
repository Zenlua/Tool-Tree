from __future__ import annotations

import ast
import re
from typing import TYPE_CHECKING, Any

from dissect.cstruct import compiler
from dissect.cstruct.exceptions import (
    ExpressionParserError,
    ExpressionTokenizerError,
    ParserError,
)
from dissect.cstruct.expression import Expression
from dissect.cstruct.types import BaseArray, BaseType, Field, Structure

if TYPE_CHECKING:
    from dissect.cstruct import cstruct


class Parser:
    """Base class for definition parsers.

    Args:
        cs: An instance of cstruct.
    """

    def __init__(self, cs: cstruct):
        self.cstruct = cs

    def parse(self, data: str) -> None:
        """This function should parse definitions to cstruct types.

        Args:
            data: Data to parse definitions from, usually a string.
        """
        raise NotImplementedError


class TokenParser(Parser):
    """
    Args:
        cs: An instance of cstruct.
        compiled: Whether structs should be compiled or not.
    """

    def __init__(self, cs: cstruct, compiled: bool = True, align: bool = False):
        super().__init__(cs)

        self.compiled = compiled
        self.align = align
        self.TOK = self._tokencollection()
        self._conditionals = []
        self._conditionals_depth = 0

    @staticmethod
    def _tokencollection() -> TokenCollection:
        TOK = TokenCollection()
        TOK.add(r"#\[(?P<values>[^\]]+)\](?=\s*)", "CONFIG_FLAG")
        TOK.add(r"#define\s+(?P<name>[^\s]+)(?P<value>[^\r\n]*)", "DEFINE")
        TOK.add(r"#undef\s+(?P<name>[^\s]+)\s*", "UNDEF")
        TOK.add(r"#ifdef\s+(?P<name>[^\s]+)\s*", "IFDEF")
        TOK.add(r"#ifndef\s+(?P<name>[^\s]+)\s*", "IFNDEF")
        TOK.add(r"#else\s*", "ELSE")
        TOK.add(r"#endif\s*", "ENDIF")
        TOK.add(r"typedef(?=\s)", "TYPEDEF")
        TOK.add(r"(?:struct|union)(?=\s|{)", "STRUCT")
        TOK.add(
            r"(?P<enumtype>enum|flag)\s+(?P<name>[^\s:{]+)?\s*(:\s"
            r"*(?P<type>[^{]+?)\s*)?\{(?P<values>[^}]+)\}\s*(?=;)",
            "ENUM",
        )
        TOK.add(r"(?<=})\s*(?P<defs>(?:[a-zA-Z0-9_]+\s*,\s*)+[a-zA-Z0-9_]+)\s*(?=;)", "DEFS")
        TOK.add(r"(?P<name>\**?\s*[a-zA-Z0-9_]+)(?:\s*:\s*(?P<bits>\d+))?(?:\[(?P<count>[^;]*)\])?\s*(?=;)", "NAME")
        TOK.add(r"#include\s+(?P<name>[^\s]+)\s*", "INCLUDE")
        TOK.add(r"[a-zA-Z_][a-zA-Z0-9_]*", "IDENTIFIER")
        TOK.add(r"[{}]", "BLOCK")
        TOK.add(r"\$(?P<name>[^\s]+) = (?P<value>{[^}]+})\w*[\r\n]+", "LOOKUP")
        TOK.add(r";", "EOL")
        TOK.add(r"\s+", None)
        TOK.add(r".", None)

        return TOK

    def _identifier(self, tokens: TokenConsumer) -> str:
        idents = []
        while tokens.next == self.TOK.IDENTIFIER:
            idents.append(tokens.consume())
        return " ".join([i.value for i in idents])

    def _conditional(self, tokens: TokenConsumer) -> None:
        token = tokens.consume()
        pattern = self.TOK.patterns[token.token]
        match = pattern.match(token.value).groupdict()

        value = match["name"]

        if token.token == self.TOK.IFDEF:
            self._conditionals.append(value in self.cstruct.consts)
        elif token.token == self.TOK.IFNDEF:
            self._conditionals.append(value not in self.cstruct.consts)

    def _check_conditional(self, tokens: TokenConsumer) -> bool:
        """Check and handle conditionals. Return a boolean indicating if we need to continue to the next token."""
        if self._conditionals and self._conditionals_depth == len(self._conditionals):
            # If we have a conditional and the depth matches, handle it accordingly
            if tokens.next == self.TOK.ELSE:
                # Flip the last conditional
                tokens.consume()
                self._conditionals[-1] = not self._conditionals[-1]
                return True

            if tokens.next == self.TOK.ENDIF:
                # Pop the last conditional
                tokens.consume()
                self._conditionals.pop()
                self._conditionals_depth -= 1
                return True

        if tokens.next in (self.TOK.IFDEF, self.TOK.IFNDEF):
            # If we encounter a new conditional, increase the depth
            self._conditionals_depth += 1

        if tokens.next == self.TOK.ENDIF:
            # Similarly, decrease the depth if needed
            self._conditionals_depth -= 1

        if self._conditionals and not self._conditionals[-1]:
            # If the last conditional evaluated to False, skip the next token
            tokens.consume()
            return True

        if tokens.next in (self.TOK.IFDEF, self.TOK.IFNDEF):
            # If the next token is a conditional, process it
            self._conditional(tokens)
            return True

        return False

    def _constant(self, tokens: TokenConsumer) -> None:
        const = tokens.consume()
        pattern = self.TOK.patterns[self.TOK.DEFINE]
        match = pattern.match(const.value).groupdict()

        value = match["value"].strip()
        try:
            value = ast.literal_eval(value)
        except (ValueError, SyntaxError):
            pass

        if isinstance(value, str):
            try:
                value = Expression(value).evaluate(self.cstruct)
            except (ExpressionParserError, ExpressionTokenizerError):
                pass

        self.cstruct.consts[match["name"]] = value

    def _undef(self, tokens: TokenConsumer) -> None:
        const = tokens.consume()
        pattern = self.TOK.patterns[self.TOK.UNDEF]
        match = pattern.match(const.value).groupdict()

        if match["name"] in self.cstruct.consts:
            del self.cstruct.consts[match["name"]]
        else:
            raise ParserError(f"line {self._lineno(const)}: constant {match['name']!r} not defined")

    def _enum(self, tokens: TokenConsumer) -> None:
        # We cheat with enums because the entire enum is in the token
        etok = tokens.consume()

        pattern = self.TOK.patterns[self.TOK.ENUM]
        # Dirty trick because the regex expects a ; but we don't want it to be part of the value
        d = pattern.match(etok.value + ";").groupdict()
        enumtype = d["enumtype"]

        nextval = 0
        if enumtype == "flag":
            nextval = 1

        values = {}
        for line in d["values"].splitlines():
            for v in line.split(","):
                key, _, val = v.partition("=")
                key = key.strip()
                val = val.strip()
                if not key:
                    continue

                val = nextval if not val else Expression(val).evaluate(self.cstruct, values)

                if enumtype == "flag":
                    high_bit = val.bit_length() - 1
                    nextval = 2 ** (high_bit + 1)
                else:
                    nextval = val + 1

                values[key] = val

        if not d["type"]:
            d["type"] = "uint32"

        factory = self.cstruct._make_flag if enumtype == "flag" else self.cstruct._make_enum

        enum = factory(d["name"] or "", self.cstruct.resolve(d["type"]), values)
        if not enum.__name__:
            self.cstruct.consts.update(enum.__members__)
        else:
            self.cstruct.add_type(enum.__name__, enum)

        tokens.eol()

    def _typedef(self, tokens: TokenConsumer) -> None:
        tokens.consume()
        type_ = None

        names = []

        if tokens.next == self.TOK.IDENTIFIER:
            type_ = self.cstruct.resolve(self._identifier(tokens))
        elif tokens.next == self.TOK.STRUCT:
            type_ = self._struct(tokens)
            if not type_.__anonymous__:
                names.append(type_.__name__)

        names.extend(self._names(tokens))
        for name in names:
            if issubclass(type_, Structure) and type_.__anonymous__:
                type_.__anonymous__ = False
                type_.__name__ = name
                type_.__qualname__ = name

            type_, name, bits = self._parse_field_type(type_, name)
            if bits is not None:
                raise ParserError(f"line {self._lineno(tokens.previous)}: typedefs cannot have bitfields")
            self.cstruct.add_type(name, type_)

    def _struct(self, tokens: TokenConsumer, register: bool = False) -> type[Structure]:
        stype = tokens.consume()

        factory = self.cstruct._make_union if stype.value.startswith("union") else self.cstruct._make_struct

        st = None
        names = []
        registered = False

        if tokens.next == self.TOK.IDENTIFIER:
            ident = tokens.consume()
            if register:
                # Pre-register an empty struct for self-referencing
                # We update this instance later with the fields
                st = factory(ident.value, [], align=self.align)
                if self.compiled and "nocompile" not in tokens.flags:
                    st = compiler.compile(st)
                self.cstruct.add_type(ident.value, st)
                registered = True
            else:
                names.append(ident.value)

        if tokens.next == self.TOK.NAME:
            # As part of a struct field
            # struct type_name field_name;
            if not names:
                raise ParserError(f"line {self._lineno(tokens.next)}: unexpected anonymous struct")
            return self.cstruct.resolve(names[0])

        if tokens.next != self.TOK.BLOCK:
            raise ParserError(f"line {self._lineno(tokens.next)}: expected start of block '{tokens.next}'")

        fields = []
        tokens.consume()
        while len(tokens):
            if tokens.next == self.TOK.BLOCK and tokens.next.value == "}":
                tokens.consume()
                break

            if self._check_conditional(tokens):
                continue

            field = self._parse_field(tokens)
            fields.append(field)

        if register:
            names.extend(self._names(tokens))

        # If the next token is EOL, consume it
        # Otherwise we're part of a typedef or field definition
        if tokens.next == self.TOK.EOL:
            tokens.eol()

        name = names[0] if names else None

        if st is None:
            is_anonymous = False
            if not name:
                is_anonymous = True
                name = self.cstruct._next_anonymous()

            st = factory(name, fields, align=self.align, anonymous=is_anonymous)
            if self.compiled and "nocompile" not in tokens.flags:
                st = compiler.compile(st)
        else:
            st.__fields__.extend(fields)
            st.commit()

        # This is pretty dirty
        if register:
            if not names and not registered:
                raise ParserError(f"line {self._lineno(stype)}: struct has no name")

            for name in names:
                self.cstruct.add_type(name, st)

        tokens.reset_flags()
        return st

    def _lookup(self, tokens: TokenConsumer) -> None:
        # Just like enums, we cheat and have the entire lookup in the token
        ltok = tokens.consume()

        pattern = self.TOK.patterns[self.TOK.LOOKUP]
        # Dirty trick because the regex expects a ; but we don't want it to be part of the value
        m = pattern.match(ltok.value + ";")
        d = ast.literal_eval(m.group(2))
        self.cstruct.lookups[m.group(1)] = {self.cstruct.consts[k]: v for k, v in d.items()}

    def _parse_field(self, tokens: TokenConsumer) -> Field:
        type_ = None
        if tokens.next == self.TOK.IDENTIFIER:
            type_ = self.cstruct.resolve(self._identifier(tokens))
        elif tokens.next == self.TOK.STRUCT:
            type_ = self._struct(tokens)

            if tokens.next != self.TOK.NAME:
                return Field(None, type_, None)

        if tokens.next != self.TOK.NAME:
            raise ParserError(f"line {self._lineno(tokens.next)}: expected name, got {tokens.next!r}")
        nametok = tokens.consume()

        type_, name, bits = self._parse_field_type(type_, nametok.value)

        tokens.eol()
        return Field(name.strip(), type_, bits)

    def _parse_field_type(self, type_: type[BaseType], name: str) -> tuple[type[BaseType], str, int | None]:
        pattern = self.TOK.patterns[self.TOK.NAME]
        # Dirty trick because the regex expects a ; but we don't want it to be part of the value
        d = pattern.match(name + ";").groupdict()

        name = d["name"]
        count_expression = d["count"]

        while name.startswith("*"):
            name = name[1:]
            type_ = self.cstruct._make_pointer(type_)

        if count_expression is not None:
            # Poor mans multi-dimensional array by abusing the eager regex match of count
            counts = count_expression.split("][") if "][" in count_expression else [count_expression]

            for count in reversed(counts):
                if count == "":
                    count = None
                else:
                    count = Expression(count)
                    try:
                        count = count.evaluate(self.cstruct)
                    except Exception:
                        pass

                if issubclass(type_, BaseArray) and count is None:
                    raise ParserError("Depth required for multi-dimensional array")

                type_ = self.cstruct._make_array(type_, count)

        return type_, name.strip(), int(d["bits"]) if d["bits"] else None

    def _names(self, tokens: TokenConsumer) -> list[str]:
        names = []
        while True:
            if tokens.next == self.TOK.EOL:
                tokens.eol()
                break

            if tokens.next not in (self.TOK.NAME, self.TOK.DEFS, self.TOK.IDENTIFIER):
                break

            ntoken = tokens.consume()
            if ntoken in (self.TOK.NAME, self.TOK.IDENTIFIER):
                names.append(ntoken.value.strip())
            elif ntoken == self.TOK.DEFS:
                names.extend([name.strip() for name in ntoken.value.strip().split(",")])

        return names

    def _include(self, tokens: TokenConsumer) -> None:
        include = tokens.consume()
        pattern = self.TOK.patterns[self.TOK.INCLUDE]
        match = pattern.match(include.value).groupdict()

        self.cstruct.includes.append(match["name"].strip().strip("'\""))

    @staticmethod
    def _remove_comments(string: str) -> str:
        # https://stackoverflow.com/a/18381470
        pattern = r"(\".*?\"|\'.*?\')|(/\*.*?\*/|//[^\r\n]*$)"
        # first group captures quoted strings (double or single)
        # second group captures comments (//single-line or /* multi-line */)
        regex = re.compile(pattern, re.MULTILINE | re.DOTALL)

        def _replacer(match: re.Match) -> str:
            # if the 2nd group (capturing comments) is not None,
            # it means we have captured a non-quoted (real) comment string.
            if comment := match.group(2):
                return "\n" * comment.count("\n")  # so we will return empty to remove the comment
            # otherwise, we will return the 1st group
            return match.group(1)  # captured quoted-string

        return regex.sub(_replacer, string)

    @staticmethod
    def _lineno(tok: Token) -> int:
        """Quick and dirty line number calculator"""

        match = tok.match
        return match.string.count("\n", 0, match.start()) + 1

    def _config_flag(self, tokens: TokenConsumer) -> None:
        flag_token = tokens.consume()
        pattern = self.TOK.patterns[self.TOK.CONFIG_FLAG]
        tok_dict = pattern.match(flag_token.value).groupdict()
        tokens.flags.extend(tok_dict["values"].split(","))

    def parse(self, data: str) -> None:
        scanner = re.Scanner(self.TOK.tokens)
        data = self._remove_comments(data)
        tokens, remaining = scanner.scan(data)

        if len(remaining):
            lineno = data.count("\n", 0, len(data) - len(remaining))
            raise ParserError(f"line {lineno}: invalid syntax in definition")

        tokens = TokenConsumer(tokens)
        while True:
            token = tokens.next
            if token is None:
                break

            if self._check_conditional(tokens):
                continue

            if token == self.TOK.CONFIG_FLAG:
                self._config_flag(tokens)
            elif token == self.TOK.DEFINE:
                self._constant(tokens)
            elif token == self.TOK.UNDEF:
                self._undef(tokens)
            elif token == self.TOK.TYPEDEF:
                self._typedef(tokens)
            elif token == self.TOK.STRUCT:
                self._struct(tokens, register=True)
            elif token == self.TOK.ENUM:
                self._enum(tokens)
            elif token == self.TOK.LOOKUP:
                self._lookup(tokens)
            elif token == self.TOK.INCLUDE:
                self._include(tokens)
            else:
                raise ParserError(f"line {self._lineno(token)}: unexpected token {token!r}")

        if self._conditionals:
            raise ParserError(f"line {self._lineno(tokens.previous)}: unclosed conditional statement")


class CStyleParser(Parser):
    """Definition parser for C-like structure syntax.

    Args:
        cs: An instance of cstruct
        compiled: Whether structs should be compiled or not.
    """

    def __init__(self, cs: cstruct, compiled: bool = True):
        self.compiled = compiled
        super().__init__(cs)

    def _constants(self, data: str) -> None:
        r = re.finditer(r"#define\s+(?P<name>[^\s]+)\s+(?P<value>[^\r\n]+)\s*\n", data)
        for t in r:
            d = t.groupdict()
            v = d["value"].rsplit("//")[0]

            try:
                v = ast.literal_eval(v)
            except (ValueError, SyntaxError):
                pass

            self.cstruct.consts[d["name"]] = v

    def _enums(self, data: str) -> None:
        r = re.finditer(
            r"(?P<enumtype>enum|flag)\s+(?P<name>[^\s:{]+)\s*(:\s*(?P<type>[^\s]+)\s*)?\{(?P<values>[^}]+)\}\s*;",
            data,
        )
        for t in r:
            d = t.groupdict()
            enumtype = d["enumtype"]

            nextval = 0
            if enumtype == "flag":
                nextval = 1

            values = {}
            for line in d["values"].split("\n"):
                line, _, _ = line.partition("//")
                for v in line.split(","):
                    key, _, val = v.partition("=")
                    key = key.strip()
                    val = val.strip()
                    if not key:
                        continue

                    val = nextval if not val else Expression(val).evaluate(self.cstruct)

                    if enumtype == "flag":
                        high_bit = val.bit_length() - 1
                        nextval = 2 ** (high_bit + 1)
                    else:
                        nextval = val + 1

                    values[key] = val

            if not d["type"]:
                d["type"] = "uint32"

            factory = self.cstruct._make_enum
            if enumtype == "flag":
                factory = self.cstruct._make_flag

            enum = factory(d["name"], self.cstruct.resolve(d["type"]), values)
            self.cstruct.add_type(enum.__name__, enum)

    def _structs(self, data: str) -> None:
        r = re.finditer(
            r"(#(?P<flags>(?:compile))\s+)?"
            r"((?P<typedef>typedef)\s+)?"
            r"(?P<type>[^\s]+)\s+"
            r"(?P<name>[^\s]+)?"
            r"(?P<fields>"
            r"\s*{[^}]+\}(?P<defs>\s+[^;\n]+)?"
            r")?\s*;",
            data,
        )
        for t in r:
            d = t.groupdict()

            if d["name"]:
                name = d["name"]
            elif d["defs"]:
                name = d["defs"].strip().split(",")[0].strip()
            else:
                raise ParserError("No name for struct")

            if d["type"] == "struct":
                data = self._parse_fields(d["fields"][1:-1].strip())
                st = self.cstruct._make_struct(name, data)
                if d["flags"] == "compile" or self.compiled:
                    st = compiler.compile(st)
            elif d["typedef"] == "typedef":
                st = d["type"]
            else:
                continue

            if d["name"]:
                self.cstruct.add_type(d["name"], st)

            if d["defs"]:
                for td in d["defs"].strip().split(","):
                    td = td.strip()
                    self.cstruct.add_type(td, st)

    def _parse_fields(self, data: str) -> None:
        fields = re.finditer(
            r"(?P<type>[^\s]+)\s+(?P<name>[^\s\[:]+)(:(?P<bits>\d+))?(\[(?P<count>[^;\n]*)\])?;",
            data,
        )

        result = []
        for f in fields:
            d = f.groupdict()
            if d["type"].startswith("//"):
                continue

            type_ = self.cstruct.resolve(d["type"])

            d["name"] = d["name"].replace("(", "").replace(")", "")

            # Maybe reimplement lazy type references later
            # _type = TypeReference(self, d['type'])
            if d["count"] is not None:
                if d["count"] == "":
                    count = None
                else:
                    count = Expression(d["count"])
                    try:
                        count = count.evaluate(self.cstruct)
                    except Exception:
                        pass

                type_ = self.cstruct._make_array(type_, count)

            if d["name"].startswith("*"):
                d["name"] = d["name"][1:]
                type_ = self.cstruct._make_pointer(type_)

            field = Field(d["name"], type_, int(d["bits"]) if d["bits"] else None)
            result.append(field)

        return result

    def _lookups(self, data: str, consts: dict[str, int]) -> None:
        r = re.finditer(r"\$(?P<name>[^\s]+) = ({[^}]+})\w*\n", data)

        for t in r:
            d = ast.literal_eval(t.group(2))
            self.cstruct.lookups[t.group(1)] = {self.cstruct.consts[k]: v for k, v in d.items()}

    def parse(self, data: str) -> None:
        self._constants(data)
        self._enums(data)
        self._structs(data)
        self._lookups(data, self.cstruct.consts)


class Token:
    __slots__ = ("match", "token", "value")

    def __init__(self, token: str, value: str, match: re.Match):
        self.token = token
        self.value = value
        self.match = match

    def __eq__(self, other: object) -> bool:
        if isinstance(other, Token):
            other = other.token

        return self.token == other

    def __ne__(self, other: object) -> bool:
        return not self == other

    def __repr__(self) -> str:
        return f"<Token.{self.token} value={self.value!r}>"


class TokenCollection:
    def __init__(self):
        self.tokens: list[Token] = []
        self.lookup: dict[str, str] = {}
        self.patterns: dict[str, re.Pattern] = {}

    def __getattr__(self, attr: str) -> str | Any:
        try:
            return self.lookup[attr]
        except AttributeError:
            pass

        return object.__getattribute__(self, attr)

    def add(self, regex: str, name: str | None) -> None:
        if name is None:
            self.tokens.append((regex, None))
        else:
            self.lookup[name] = name
            self.patterns[name] = re.compile(regex)
            self.tokens.append((regex, lambda s, t: Token(name, t, s.match)))


class TokenConsumer:
    def __init__(self, tokens: list[Token]):
        self.tokens = tokens
        self.flags = []
        self.previous = None

    def __contains__(self, token: Token) -> bool:
        return token in self.tokens

    def __len__(self) -> int:
        return len(self.tokens)

    def __repr__(self) -> str:
        return f"<TokenConsumer next={self.next!r}>"

    @property
    def next(self) -> Token:
        try:
            return self.tokens[0]
        except IndexError:
            return None

    def consume(self) -> Token:
        self.previous = self.tokens.pop(0)
        return self.previous

    def reset_flags(self) -> None:
        self.flags = []

    def eol(self) -> None:
        token = self.consume()
        if token.token != "EOL":
            raise ParserError(f"line {self._lineno(token)}: expected EOL")
