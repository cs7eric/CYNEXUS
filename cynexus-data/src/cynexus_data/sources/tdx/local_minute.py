from __future__ import annotations

import re
import struct
from dataclasses import dataclass
from datetime import date, datetime
from pathlib import Path
from typing import Iterable

import pandas as pd
from pytdx.reader import TdxLCMinBarReader, TdxMinBarReader


TdxReader = TdxLCMinBarReader | TdxMinBarReader
_MINUTE_RECORD_SIZE = 32
_RECORD_STRUCTS = {
    "lc1": struct.Struct("<HHfffffII"),
    "min1": struct.Struct("<HHIIIIfII"),
}


@dataclass(frozen=True)
class TdxMinuteFile:
    path: Path
    market: str
    symbol: str
    tdx_code: str
    reader_kind: str


def discover_1m_files(tdx_root: Path) -> list[TdxMinuteFile]:
    root = tdx_root.expanduser().resolve()
    markets = ["sh", "sz", "bj"]
    patterns = [f"vipdoc/{market}/minline/*.lc1" for market in markets]
    patterns += [f"vipdoc/{market}/minline/*.1" for market in markets]
    patterns += [f"vipdoc/{market}/fzline/*.1" for market in markets]
    files: list[TdxMinuteFile] = []
    for pattern in patterns:
        for path in root.glob(pattern):
            files.append(parse_tdx_minute_file(path))
    return sorted({file.path: file for file in files}.values(), key=lambda item: str(item.path).lower())


def parse_tdx_minute_file(path: Path, market: str | None = None, symbol: str | None = None) -> TdxMinuteFile:
    resolved = path.expanduser().resolve()
    suffix = resolved.suffix.lower()
    if suffix == ".lc1":
        reader_kind = "lc1"
    elif suffix == ".1":
        reader_kind = "min1"
    else:
        raise ValueError(f"Unsupported TDX 1m file suffix: {resolved.suffix}")

    name = resolved.stem.lower()
    inferred_market = market or _infer_market(resolved, name)
    inferred_symbol = symbol or _infer_symbol(name)
    tdx_code = f"{inferred_market.lower()}{inferred_symbol}"
    return TdxMinuteFile(
        path=resolved,
        market=inferred_market.upper(),
        symbol=inferred_symbol,
        tdx_code=tdx_code,
        reader_kind=reader_kind,
    )


def read_tdx_1m_file(
    file: TdxMinuteFile,
    *,
    start_date: date | str | None = None,
    end_date: date | str | None = None,
) -> pd.DataFrame:
    if start_date is not None or end_date is not None:
        rows = _read_filtered_records(file, _coerce_date(start_date), _coerce_date(end_date))
        if not rows:
            return _empty_frame()
        frame = pd.DataFrame(rows)
    else:
        reader = _reader_for(file)
        df = reader.get_df(str(file.path))
        if df is None or df.empty:
            return _empty_frame()

        frame = df.copy()
        frame.index = pd.to_datetime(frame.index)
        frame = frame.reset_index(names="trade_time")
    numeric_columns = ["open", "high", "low", "close", "amount", "volume"]
    for column in numeric_columns:
        frame[column] = pd.to_numeric(frame[column], errors="coerce")
    frame = frame.dropna(subset=["trade_time", "open", "high", "low", "close"])
    if frame.empty:
        return _empty_frame()

    frame["amount"] = frame["amount"].fillna(0.0)
    frame["volume"] = frame["volume"].fillna(0).round().astype("int64")
    frame["trade_date"] = frame["trade_time"].dt.date
    frame["market"] = file.market
    frame["symbol"] = file.symbol
    frame["tdx_code"] = file.tdx_code
    frame["period"] = 1
    frame["source"] = "tdx_local"
    frame["source_file"] = str(file.path)
    frame["source_file_mtime"] = datetime.fromtimestamp(file.path.stat().st_mtime)
    frame["ingested_at"] = datetime.now()

    columns = [
        "market",
        "symbol",
        "tdx_code",
        "period",
        "trade_time",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "amount",
        "volume",
        "source",
        "source_file",
        "source_file_mtime",
        "ingested_at",
    ]
    return frame[columns].sort_values("trade_time")


def iter_tdx_1m_frames(
    files: Iterable[TdxMinuteFile],
    *,
    start_date: date | str | None = None,
    end_date: date | str | None = None,
) -> Iterable[tuple[TdxMinuteFile, pd.DataFrame]]:
    for file in files:
        yield file, read_tdx_1m_file(file, start_date=start_date, end_date=end_date)


def latest_tdx_1m_date(files: Iterable[TdxMinuteFile]) -> date | None:
    latest: date | None = None
    for file in files:
        current = _read_last_record_date(file)
        if current is not None and (latest is None or current > latest):
            latest = current
    return latest


def _read_filtered_records(
    file: TdxMinuteFile,
    start_date: date | None,
    end_date: date | None,
) -> list[dict[str, object]]:
    record_struct = _RECORD_STRUCTS[file.reader_kind]
    record_count = file.path.stat().st_size // _MINUTE_RECORD_SIZE
    if record_count <= 0:
        return []

    last_record_date = _read_last_record_date(file)
    if start_date is not None and (last_record_date is None or last_record_date < start_date):
        return []

    selected: list[dict[str, object]] = []
    block_records = 512
    with file.path.open("rb") as handle:
        cursor = record_count
        while cursor > 0:
            first = max(0, cursor - block_records)
            handle.seek(first * _MINUTE_RECORD_SIZE)
            payload = handle.read((cursor - first) * _MINUTE_RECORD_SIZE)
            for raw in reversed(list(record_struct.iter_unpack(payload))):
                row = _decode_record(file, raw)
                trade_date = row["trade_time"].date()
                if end_date is not None and trade_date > end_date:
                    continue
                if start_date is not None and trade_date < start_date:
                    return list(reversed(selected))
                selected.append(row)
            cursor = first
    return list(reversed(selected))


def _read_last_record_date(file: TdxMinuteFile) -> date | None:
    record_count = file.path.stat().st_size // _MINUTE_RECORD_SIZE
    if record_count <= 0:
        return None
    record_struct = _RECORD_STRUCTS[file.reader_kind]
    with file.path.open("rb") as handle:
        handle.seek((record_count - 1) * _MINUTE_RECORD_SIZE)
        payload = handle.read(_MINUTE_RECORD_SIZE)
    if len(payload) != _MINUTE_RECORD_SIZE:
        return None
    return _decode_record(file, record_struct.unpack(payload))["trade_time"].date()


def _decode_record(file: TdxMinuteFile, raw: tuple[object, ...]) -> dict[str, object]:
    date_code, time_code = int(raw[0]), int(raw[1])
    year = date_code // 2048 + 2004
    month = (date_code % 2048) // 100
    day = (date_code % 2048) % 100
    trade_time = datetime(year, month, day, time_code // 60, time_code % 60)
    if file.reader_kind == "lc1":
        open_price, high, low, close, amount, volume = raw[2:8]
    else:
        open_price, high, low, close = (float(value) / 100 for value in raw[2:6])
        amount, volume = raw[6:8]
    return {
        "trade_time": trade_time,
        "open": open_price,
        "high": high,
        "low": low,
        "close": close,
        "amount": amount,
        "volume": volume,
    }


def _coerce_date(value: date | str | None) -> date | None:
    if value is None:
        return None
    if isinstance(value, date):
        return value
    return pd.to_datetime(value).date()


def _reader_for(file: TdxMinuteFile) -> TdxReader:
    if file.reader_kind == "lc1":
        return TdxLCMinBarReader()
    return TdxMinBarReader()


def _infer_market(path: Path, stem: str) -> str:
    for market in ["sh", "sz", "bj"]:
        if stem.startswith(market):
            return market.upper()

    parts = {part.lower() for part in path.parts}
    for market in ["sh", "sz", "bj"]:
        if market in parts:
            return market.upper()

    raise ValueError(f"Cannot infer market from file path: {path}")


def _infer_symbol(stem: str) -> str:
    match = re.search(r"(\d{6})", stem)
    if not match:
        raise ValueError(f"Cannot infer 6 digit symbol from file name: {stem}")
    return match.group(1)


def _empty_frame() -> pd.DataFrame:
    return pd.DataFrame(
        columns=[
            "market",
            "symbol",
            "tdx_code",
            "period",
            "trade_time",
            "trade_date",
            "open",
            "high",
            "low",
            "close",
            "amount",
            "volume",
            "source",
            "source_file",
            "source_file_mtime",
            "ingested_at",
        ]
    )
