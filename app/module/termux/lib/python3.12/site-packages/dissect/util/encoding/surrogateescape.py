import codecs


def error_handler(error: Exception) -> tuple[str, int]:
    if not isinstance(error, UnicodeDecodeError):
        raise error

    result = []
    for i in range(error.start, error.end):
        byte = error.object[i]
        if byte < 128:
            raise error
        result.append(chr(0xDC00 + byte))

    return "".join(result), error.end


try:
    codecs.lookup_error("surrogateescape")
except LookupError:
    codecs.register_error("surrogateescape", error_handler)
