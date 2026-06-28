from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Iterable

import pandas as pd
from pytdx.reader import TdxLCMinBarReader, TdxMinBarReader


TdxReader = TdxLCMinBarReader | TdxMinBarReader


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


def read_tdx_1m_file(file: TdxMinuteFile) -> pd.DataFrame:
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


def iter_tdx_1m_frames(files: Iterable[TdxMinuteFile]) -> Iterable[tuple[TdxMinuteFile, pd.DataFrame]]:
    for file in files:
        yield file, read_tdx_1m_file(file)


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
