class Error(Exception):
    pass


class ParserError(Error):
    pass


class ResolveError(Error):
    pass


class NullPointerDereference(Error):
    pass


class ArraySizeError(Error):
    pass


class ExpressionParserError(Error):
    pass


class ExpressionTokenizerError(Error):
    pass
