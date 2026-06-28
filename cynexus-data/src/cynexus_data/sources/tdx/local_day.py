from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Iterable

import pandas as pd

from cynexus_data.sources.tdx.parser import parse_day_file


@dataclass(frozen=True)
class TdxDayFile:
    path: Path
    market: str
    symbol: str
    tdx_code: str
    size: int

    @property
    def record_count(self) -> int:
        return self.size // 32


def discover_day_files(root: Path, markets: Iterable[str] = ("sh", "sz", "bj")) -> list[TdxDayFile]:
    base = root.expanduser().resolve()
    files: list[TdxDayFile] = []
    for market in [item.lower() for item in markets]:
        for lday in _candidate_lday_dirs(base, market):
            if not lday.is_dir():
                continue
            for path in sorted(lday.glob(f"{market}*.day")):
                if path.stat().st_size == 0:
                    continue
                files.append(parse_tdx_day_file(path))
    return sorted({file.path: file for file in files}.values(), key=lambda item: str(item.path).lower())


def parse_tdx_day_file(path: Path, market: str | None = None, symbol: str | None = None) -> TdxDayFile:
    resolved = path.expanduser().resolve()
    if resolved.suffix.lower() != ".day":
        raise ValueError(f"Unsupported TDX day file suffix: {resolved.suffix}")
    name = resolved.stem.lower()
    inferred_market = (market or _infer_market(resolved, name)).upper()
    inferred_symbol = symbol or _infer_symbol(name)
    return TdxDayFile(
        path=resolved,
        market=inferred_market,
        symbol=inferred_symbol,
        tdx_code=f"{inferred_market.lower()}{inferred_symbol}",
        size=resolved.stat().st_size,
    )


def read_tdx_day_file(file: TdxDayFile, unit_scale: int = 1, start_date=None, end_date=None) -> pd.DataFrame:
    result = parse_day_file(file.path, unit_scale=unit_scale)
    frame = result.frame.copy()
    if frame.empty:
        return _empty_frame()
    if start_date is not None:
        start = pd.to_datetime(start_date).date()
        frame = frame[frame["trade_date"] >= start]
    if end_date is not None:
        end = pd.to_datetime(end_date).date()
        frame = frame[frame["trade_date"] <= end]
    if frame.empty:
        return _empty_frame()

    frame["market"] = file.market
    frame["symbol"] = file.symbol
    frame["code"] = file.symbol
    frame["tdx_code"] = file.tdx_code
    frame["source_file"] = str(file.path)
    frame["source_file_mtime"] = datetime.fromtimestamp(file.path.stat().st_mtime)
    frame["ingested_at"] = datetime.now()
    columns = [
        "market",
        "symbol",
        "code",
        "tdx_code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "amount",
        "volume",
        "unit_scale",
        "source",
        "source_file",
        "source_file_mtime",
        "ingested_at",
    ]
    return frame[columns].sort_values("trade_date")


def iter_tdx_day_frames(files: Iterable[TdxDayFile], unit_scale: int = 1, start_date=None, end_date=None):
    for file in files:
        yield file, read_tdx_day_file(file, unit_scale=unit_scale, start_date=start_date, end_date=end_date)


def _candidate_lday_dirs(base: Path, market: str) -> list[Path]:
    return [
        base / "vipdoc" / market / "lday",
        base / market / "lday",
        base / "hsjday" / market / "lday",
    ]


def _infer_market(path: Path, stem: str) -> str:
    for market in ["sh", "sz", "bj"]:
        if stem.startswith(market):
            return market
    parts = {part.lower() for part in path.parts}
    for market in ["sh", "sz", "bj"]:
        if market in parts:
            return market
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
            "code",
            "tdx_code",
            "trade_date",
            "open",
            "high",
            "low",
            "close",
            "amount",
            "volume",
            "unit_scale",
            "source",
            "source_file",
            "source_file_mtime",
            "ingested_at",
        ]
    )
