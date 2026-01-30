# References:
# - https://github.com/lzfse/lzfse
from __future__ import annotations

import io
import struct
from typing import BinaryIO, NamedTuple

from dissect.util.compression import lzvn

LZFSE_ENDOFSTREAM_BLOCK_MAGIC = b"bvx$"  # 0x24787662 (end of stream)
LZFSE_UNCOMPRESSED_BLOCK_MAGIC = b"bvx-"  # 0x2d787662 (raw data)
LZFSE_COMPRESSEDV1_BLOCK_MAGIC = b"bvx1"  # 0x31787662 (lzfse compressed, uncompressed tables)
LZFSE_COMPRESSEDV2_BLOCK_MAGIC = b"bvx2"  # 0x32787662 (lzfse compressed, compressed tables)
LZFSE_COMPRESSEDLZVN_BLOCK_MAGIC = b"bvxn"  # 0x6e787662 (lzvn compressed)

# Throughout LZFSE we refer to "L", "M" and "D"; these will always appear as
# a triplet, and represent a "usual" LZ-style literal and match pair.  "L"
# is the number of literal bytes, "M" is the number of match bytes, and "D"
# is the match "distance"; the distance in bytes between the current pointer
# and the start of the match.
LZFSE_ENCODE_L_SYMBOLS = 20
LZFSE_ENCODE_M_SYMBOLS = 20
LZFSE_ENCODE_D_SYMBOLS = 64
LZFSE_ENCODE_LITERAL_SYMBOLS = 256
LZFSE_ENCODE_L_STATES = 64
LZFSE_ENCODE_M_STATES = 64
LZFSE_ENCODE_D_STATES = 256
LZFSE_ENCODE_LITERAL_STATES = 1024
LZFSE_MATCHES_PER_BLOCK = 10000
LZFSE_LITERALS_PER_BLOCK = 4 * LZFSE_MATCHES_PER_BLOCK

# fmt: off
_lzfse_freq_nbits_table = (
    2, 3, 2, 5, 2, 3, 2, 8, 2, 3, 2, 5, 2, 3, 2, 14,
    2, 3, 2, 5, 2, 3, 2, 8, 2, 3, 2, 5, 2, 3, 2, 14
)
_lzfse_freq_value_table = (
    0, 2, 1, 4, 0, 3, 1, -1, 0, 2, 1, 5, 0, 3, 1, -1,
    0, 2, 1, 6, 0, 3, 1, -1, 0, 2, 1, 7, 0, 3, 1, -1
)

# The L, M, D data streams are all encoded as a "base" value, which is
# FSE-encoded, and an "extra bits" value, which is the difference between
# value and base, and is simply represented as a raw bit value (because it
# is the low-order bits of a larger number, not much entropy can be
# extracted from these bits by more complex encoding schemes). The following
# tables represent the number of low-order bits to encode separately and the
# base values for each of L, M, and D.
_l_extra_bits = (
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 5, 8
)
_l_base_value = (
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 20, 28, 60
)
_m_extra_bits = (
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 5, 8, 11
)
_m_base_value = (
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 56, 312
)
_d_extra_bits = (
    0,  0,  0,  0,  1,  1,  1,  1,  2,  2,  2,  2,  3,  3,  3,  3,
    4,  4,  4,  4,  5,  5,  5,  5,  6,  6,  6,  6,  7,  7,  7,  7,
    8,  8,  8,  8,  9,  9,  9,  9,  10, 10, 10, 10, 11, 11, 11, 11,
    12, 12, 12, 12, 13, 13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15
)
_d_base_value = (
    0,      1,      2,      3,     4,     6,     8,     10,    12,    16,
    20,     24,     28,     36,    44,    52,    60,    76,    92,    108,
    124,    156,    188,    220,   252,   316,   380,   444,   508,   636,
    764,    892,    1020,   1276,  1532,  1788,  2044,  2556,  3068,  3580,
    4092,   5116,   6140,   7164,  8188,  10236, 12284, 14332, 16380, 20476,
    24572,  28668,  32764,  40956, 49148, 57340, 65532, 81916, 98300, 114684,
    131068, 163836, 196604, 229372
)
# fmt: on


_I = struct.Struct("<I")


def _clz(n: int) -> int:
    if n == 0:
        return 32
    return 32 - n.bit_length()


class LZFSECompressedBlockHeader(NamedTuple):
    """LZFSE compressed block header."""

    __struct_v1__ = struct.Struct(
        "<IIIIIIi4HiHHH"
        f"{LZFSE_ENCODE_L_SYMBOLS}H"
        f"{LZFSE_ENCODE_M_SYMBOLS}H"
        f"{LZFSE_ENCODE_D_SYMBOLS}H"
        f"{LZFSE_ENCODE_LITERAL_SYMBOLS}H"
    )
    __struct_v2__ = struct.Struct("<I3Q")

    n_raw_bytes: int
    n_payload_bytes: int
    n_literals: int
    n_matches: int
    n_literal_payload_bytes: int
    n_lmd_payload_bytes: int
    literal_bits: int
    literal_state: tuple[int, int, int, int]
    lmd_bits: int
    l_state: int
    m_state: int
    d_state: int
    l_freq: tuple[int, ...]
    m_freq: tuple[int, ...]
    d_freq: tuple[int, ...]
    literal_freq: tuple[int, ...]

    @classmethod
    def parse_v1(cls, fh: BinaryIO) -> LZFSECompressedBlockHeader:
        """Decode all fields from a v1 header."""
        values = cls.__struct_v1__.unpack(fh.read(cls.__struct_v1__.size))

        return cls(
            n_raw_bytes=values[0],
            n_payload_bytes=values[1],
            n_literals=values[2],
            n_matches=values[3],
            n_literal_payload_bytes=values[4],
            n_lmd_payload_bytes=values[5],
            literal_bits=values[6],
            literal_state=values[7:11],
            lmd_bits=values[11],
            l_state=values[12],
            m_state=values[13],
            d_state=values[14],
            l_freq=values[15 : 15 + LZFSE_ENCODE_L_SYMBOLS],
            m_freq=values[35 : 35 + LZFSE_ENCODE_M_SYMBOLS],
            d_freq=values[55 : 55 + LZFSE_ENCODE_D_SYMBOLS],
            literal_freq=values[119 : 119 + LZFSE_ENCODE_LITERAL_SYMBOLS],
        )

    @classmethod
    def parse_v2(cls, fh: BinaryIO) -> LZFSECompressedBlockHeader:
        """Decode all fields from a v2 header."""
        values = cls.__struct_v2__.unpack(fh.read(cls.__struct_v2__.size))
        v0, v1, v2 = values[1:4]

        n_literal_payload_bytes = _get_field(v0, 20, 20)
        n_lmd_payload_bytes = _get_field(v1, 40, 20)
        n_payload_bytes = n_literal_payload_bytes + n_lmd_payload_bytes

        freq_tables_size = _get_field(v2, 0, 32) - cls.__struct_v2__.size - 4  # exclude magic

        if freq_tables_size == 0:
            l_freq = (0,) * 20
            m_freq = (0,) * 20
            d_freq = (0,) * 64
            literal_freq = (0,) * 256
        else:
            accum = 0
            accum = int.from_bytes(fh.read(freq_tables_size), "little")
            result = [0] * 720

            for i in range(
                LZFSE_ENCODE_L_SYMBOLS + LZFSE_ENCODE_M_SYMBOLS + LZFSE_ENCODE_D_SYMBOLS + LZFSE_ENCODE_LITERAL_SYMBOLS
            ):
                # Decode and store value
                nbits, value = _decode_v1_freq_value(accum)
                result[i] = value

                # Consume nbits bits
                accum >>= nbits

            l_freq = tuple(result[0:20])
            m_freq = tuple(result[20:40])
            d_freq = tuple(result[40:104])
            literal_freq = tuple(result[104:360])

        return cls(
            n_raw_bytes=values[0],
            n_payload_bytes=n_payload_bytes,
            n_literals=_get_field(v0, 0, 20),
            n_matches=_get_field(v0, 40, 20),
            n_literal_payload_bytes=n_literal_payload_bytes,
            n_lmd_payload_bytes=n_lmd_payload_bytes,
            literal_bits=_get_field(v0, 60, 3) - 7,
            literal_state=(
                _get_field(v1, 0, 10),
                _get_field(v1, 10, 10),
                _get_field(v1, 20, 10),
                _get_field(v1, 30, 10),
            ),
            lmd_bits=_get_field(v1, 60, 3) - 7,
            l_state=_get_field(v2, 32, 10),
            m_state=_get_field(v2, 42, 10),
            d_state=_get_field(v2, 52, 10),
            l_freq=l_freq,
            m_freq=m_freq,
            d_freq=d_freq,
            literal_freq=literal_freq,
        )


def _get_field(value: int, offset: int, nbits: int) -> int:
    """Extracts up to 32 bits from a 64-bit field beginning at offset, and zero-extends them to a 32-bit int.

    If we number the bits of ``value`` from 0 (least significant) to 63 (most significant),
    the result is bits ``offset`` to ``offset+nbits-1``.
    """
    return (value >> offset) & ((1 << nbits) - 1)


def _decode_v1_freq_value(bits: int) -> tuple[int, int]:
    """Decode an entry value from next bits of stream."""
    b = bits & 31
    n = _lzfse_freq_nbits_table[b]

    # Special cases for > 5 bits encoding
    if n == 8:
        value = 8 + ((bits >> 4) & 0xF)
    elif n == 14:
        value = 24 + ((bits >> 4) & 0x3FF)
    else:
        value = _lzfse_freq_value_table[b]

    return n, value


class DecoderEntry(NamedTuple):
    """Entry for one state in the decoder table."""

    k: int  # Number of bits to read
    symbol: int  # Emitted symbol
    delta: int  # Signed increment used to compute next state (+bias)


class ValueDecoderEntry(NamedTuple):
    """Entry for one state in the value decoder table."""

    total_bits: int  # state bits + extra value bits = shift for next decode
    value_bits: int  # extra value bits
    delta: int  # state base (delta)
    vbase: int  # value base


def _init_decoder_table(nstates: int, freq: tuple[int, ...]) -> list[DecoderEntry]:
    """Initialize decoder table ``T[nstates]``.

    ``nstates = sum freq[i]`` is the number of states (a power of 2).
    ``freq`` is a normalized histogram of symbol frequencies, with ``freq[i] >= 0``.

    Some symbols may have a 0 frequency. In that case, they should not be present in the data.
    """
    table = []

    n_clz = _clz(nstates)
    sum_of_freq = 0

    for i, f in enumerate(freq):
        if f == 0:
            # skip this symbol, no occurrences
            continue

        sum_of_freq += f
        if sum_of_freq > nstates:
            raise ValueError("Invalid frequency table")
        k = _clz(f) - n_clz  # shift needed to ensure N <= (F<<K) < 2*N
        j0 = ((2 * nstates) >> k) - f

        # Initialize all states S reached by this symbol: OFFSET <= S < OFFSET + F
        table.extend(
            DecoderEntry(
                k=k if j < j0 else k - 1,
                symbol=i,
                delta=((f + j) << k) - nstates if j < j0 else (j - j0) << (k - 1),
            )
            for j in range(f)
        )

    return table


def _init_value_decoder_table(
    nstates: int, freq: tuple[int, ...], symbol_vbits: tuple[int, ...], symbol_vbase: tuple[int, ...]
) -> list[ValueDecoderEntry]:
    """Initialize value decoder table ``T[nstates]``.

    ``nstates = sum freq[i]`` is the number of states (a power of 2).
    ``freq`` is a normalized histogram of symbol frequencies, with ``freq[i] >= 0``.
    ``symbol_vbits`` and ``symbol_vbase`` are the number of value bits to read and the base value for each symbol.

    Some symbols may have a 0 frequency. In that case, they should not be present in the data.
    """
    table = []

    n_clz = _clz(nstates)
    for i, f in enumerate(freq):
        if f == 0:
            # skip this symbol, no occurrences
            continue

        k = _clz(f) - n_clz  # shift needed to ensure N <= (F<<K) < 2*N
        j0 = ((2 * nstates) >> k) - f

        # Initialize all states S reached by this symbol: OFFSET <= S < OFFSET + F
        table.extend(
            ValueDecoderEntry(
                total_bits=k + symbol_vbits[i] if j < j0 else (k - 1) + symbol_vbits[i],
                value_bits=symbol_vbits[i],
                delta=(((f + j) << k) - nstates) if j < j0 else ((j - j0) << (k - 1)),
                vbase=symbol_vbase[i],
            )
            for j in range(f)
        )

    return table


class _BitStream:
    def __init__(self, data: bytes, nbits: int):
        self.accum = int.from_bytes(data, "little")
        self.nbits = nbits + (len(data) * 8)

    def pull(self, n: int) -> int:
        self.nbits -= n
        result = self.accum >> self.nbits
        self.accum &= (1 << self.nbits) - 1
        return result


def _decode(state: int, decoder_table: list[DecoderEntry], in_stream: _BitStream) -> tuple[int, int]:
    """Decode and return symbol using the decoder table, and update state."""
    e = decoder_table[state]

    # Update state from K bits of input + DELTA
    state = e.delta + in_stream.pull(e.k)

    # Return the symbol for this state
    return state, e.symbol


def _value_decode(state: int, decoder_table: list[ValueDecoderEntry], in_stream: _BitStream) -> tuple[int, int]:
    """Decode and return value using the decoder table, and update state."""
    entry = decoder_table[state]

    state_and_value_bits = in_stream.pull(entry.total_bits)
    state = entry.delta + (state_and_value_bits >> entry.value_bits)

    return state, entry.vbase + (state_and_value_bits & ((1 << entry.value_bits) - 1))


def _decode_lmd(
    header: LZFSECompressedBlockHeader,
    literals: list[int],
    l_decoder: list[ValueDecoderEntry],
    m_decoder: list[ValueDecoderEntry],
    d_decoder: list[ValueDecoderEntry],
    in_stream: _BitStream,
) -> bytes:
    symbols = header.n_matches

    l_state = header.l_state
    m_state = header.m_state
    d_state = header.d_state

    lit = io.BytesIO(bytes(literals))

    # ruff: noqa: N806
    L = 0
    M = 0
    D = None

    dst = bytearray()

    while symbols:
        # Decode the next L, M, D symbol from the input stream
        l_state, L = _value_decode(l_state, l_decoder, in_stream)
        if (lit.tell() + L) >= LZFSE_LITERALS_PER_BLOCK + 64:
            raise ValueError("Literal overflow")

        m_state, M = _value_decode(m_state, m_decoder, in_stream)
        d_state, new_d = _value_decode(d_state, d_decoder, in_stream)
        D = new_d if new_d != 0 else D

        if D is None or len(dst) + L < D:
            raise ValueError("Invalid match distance")

        dst += lit.read(L)

        remaining = M
        while remaining > 0:
            match_size = min(remaining, D)
            dst += dst[-D : (-D + match_size) or None]
            remaining -= match_size

        symbols -= 1

    return bytes(dst)


def decompress(src: bytes | BinaryIO) -> bytes:
    """LZFSE decompress from a file-like object or bytes.

    Decompresses until EOF or EOS of the input data.

    Args:
        src: File-like object or bytes to decompress.

    Returns:
        The decompressed data.
    """
    if not hasattr(src, "read"):
        src = io.BytesIO(src)

    dst = bytearray()

    while True:
        magic = src.read(4)

        if magic == LZFSE_ENDOFSTREAM_BLOCK_MAGIC:
            break

        if magic == LZFSE_UNCOMPRESSED_BLOCK_MAGIC:
            (n_raw_bytes,) = _I.unpack(src.read(4))
            if n_raw_bytes == 0:
                continue

            dst += src.read(n_raw_bytes)

        elif magic in (LZFSE_COMPRESSEDV1_BLOCK_MAGIC, LZFSE_COMPRESSEDV2_BLOCK_MAGIC):
            if magic == LZFSE_COMPRESSEDV1_BLOCK_MAGIC:
                header = LZFSECompressedBlockHeader.parse_v1(src)
            else:
                header = LZFSECompressedBlockHeader.parse_v2(src)

            literal_decoder = _init_decoder_table(LZFSE_ENCODE_LITERAL_STATES, header.literal_freq)
            l_decoder = _init_value_decoder_table(LZFSE_ENCODE_L_STATES, header.l_freq, _l_extra_bits, _l_base_value)
            m_decoder = _init_value_decoder_table(LZFSE_ENCODE_M_STATES, header.m_freq, _m_extra_bits, _m_base_value)
            d_decoder = _init_value_decoder_table(LZFSE_ENCODE_D_STATES, header.d_freq, _d_extra_bits, _d_base_value)

            in_stream = _BitStream(src.read(header.n_literal_payload_bytes), header.literal_bits)

            literals = []
            state0, state1, state2, state3 = header.literal_state
            for _ in range(0, header.n_literals, 4):
                state0, result = _decode(state0, literal_decoder, in_stream)
                literals.append(result)
                state1, result = _decode(state1, literal_decoder, in_stream)
                literals.append(result)
                state2, result = _decode(state2, literal_decoder, in_stream)
                literals.append(result)
                state3, result = _decode(state3, literal_decoder, in_stream)
                literals.append(result)

            in_stream = _BitStream(src.read(header.n_lmd_payload_bytes), header.lmd_bits)
            dst += _decode_lmd(header, literals, l_decoder, m_decoder, d_decoder, in_stream)

        elif magic == LZFSE_COMPRESSEDLZVN_BLOCK_MAGIC:
            (n_raw_bytes,) = _I.unpack(src.read(4))
            (n_payload_bytes,) = _I.unpack(src.read(4))

            dst += lzvn.decompress(src.read(n_payload_bytes))

        else:
            raise ValueError(f"Invalid LZFSE block magic: {magic!r}")

    return bytes(dst)
