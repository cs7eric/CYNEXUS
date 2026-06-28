from __future__ import annotations

import struct
from dataclasses import dataclass
from pathlib import Path

import pandas as pd


_DAY_RECORD_FMT = "<IIIIIfII"
_DAY_RECORD_SIZE = 32


@dataclass(frozen=True)
class DayParseResult:
    code: str
    frame: pd.DataFrame
    unit_scale: int = 1


def parse_day_file(path: Path, unit_scale: int = 1) -> DayParseResult:
    buffer = path.read_bytes()
    if len(buffer) % _DAY_RECORD_SIZE != 0:
        raise ValueError(f"file size is not a multiple of {_DAY_RECORD_SIZE}: {path}")
    rows = []
    for offset in range(0, len(buffer), _DAY_RECORD_SIZE):
        date_int, open_raw, high_raw, low_raw, close_raw, amount, volume, _prev = struct.unpack(
            _DAY_RECORD_FMT, buffer[offset : offset + _DAY_RECORD_SIZE]
        )
        rows.append((date_int, open_raw, high_raw, low_raw, close_raw, amount, volume))
    raw = pd.DataFrame(rows, columns=["date_int", "open_raw", "high_raw", "low_raw", "close_raw", "amount", "volume"])
    trade_date = pd.to_datetime(raw["date_int"].astype(str), format="%Y%m%d").dt.date
    frame = pd.DataFrame(
        {
            "code": path.stem[2:],
            "trade_date": trade_date,
            "open": raw["open_raw"] / 1000.0 * unit_scale,
            "high": raw["high_raw"] / 1000.0 * unit_scale,
            "low": raw["low_raw"] / 1000.0 * unit_scale,
            "close": raw["close_raw"] / 1000.0 * unit_scale,
            "volume": raw["volume"].astype("int64"),
            "amount": raw["amount"].astype("float64"),
            "unit_scale": unit_scale,
            "source": "tdx_day",
        }
    )
    return DayParseResult(code=str(frame["code"].iloc[0]) if not frame.empty else path.stem[2:], frame=frame, unit_scale=unit_scale)

