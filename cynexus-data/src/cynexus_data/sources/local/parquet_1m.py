from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import date, datetime
from pathlib import Path

import pyarrow as pa
import pyarrow.compute as pc
import pyarrow.parquet as pq


EXPECTED_COLUMNS = {
    "ts_code",
    "open",
    "high",
    "low",
    "close",
    "vol",
    "amount",
    "adj_factor",
    "trade_date",
    "trade_time",
}


@dataclass(frozen=True)
class Parquet1mFile:
    path: Path
    market: str
    symbol: str
    ts_code: str
    rows: int
    size: int
    mtime: float


def discover_parquet_1m_files(root: Path) -> list[Parquet1mFile]:
    base = root.expanduser().resolve()
    files = []
    for path in base.glob("*.parquet"):
        files.append(parse_parquet_1m_file(path))
    return sorted(files, key=lambda item: item.path.name)


def parse_parquet_1m_file(path: Path) -> Parquet1mFile:
    resolved = path.expanduser().resolve()
    match = re.fullmatch(r"(?P<symbol>\d{6})\.(?P<market>[A-Za-z]{2})\.parquet", resolved.name)
    if not match:
        raise ValueError(f"Unsupported 1m parquet file name: {resolved.name}")

    metadata = pq.ParquetFile(resolved).metadata
    stat = resolved.stat()
    symbol = match.group("symbol")
    market = match.group("market").upper()
    return Parquet1mFile(
        path=resolved,
        market=market,
        symbol=symbol,
        ts_code=f"{symbol}.{market}",
        rows=metadata.num_rows,
        size=stat.st_size,
        mtime=stat.st_mtime,
    )


def read_parquet_1m_arrow(
    file: Parquet1mFile,
    *,
    max_rows: int | None = None,
    start_date: date | None = None,
) -> pa.Table:
    table = pq.read_table(file.path)
    missing = EXPECTED_COLUMNS - set(table.column_names)
    if missing:
        raise ValueError(f"{file.path} is missing columns: {sorted(missing)}")
    if start_date is not None:
        table = table.filter(pc.greater_equal(table["trade_date"], pa.scalar(start_date, type=pa.date32())))
    if max_rows is not None:
        table = table.slice(0, max_rows)

    rows = table.num_rows
    source_file_mtime = datetime.fromtimestamp(file.mtime)
    ingested_at = datetime.now()

    return pa.table(
        {
            "market": _repeat_string(file.market, rows),
            "symbol": _repeat_string(file.symbol, rows),
            "tdx_code": _repeat_string(file.ts_code, rows),
            "period": pa.array([1] * rows, type=pa.uint16()),
            "trade_time": pc.cast(table["trade_time"], pa.timestamp("s")),
            "trade_date": pc.cast(table["trade_date"], pa.date32()),
            "open": pc.cast(table["open"], pa.float64()),
            "high": pc.cast(table["high"], pa.float64()),
            "low": pc.cast(table["low"], pa.float64()),
            "close": pc.cast(table["close"], pa.float64()),
            "amount": pc.cast(table["amount"], pa.float64()),
            "volume": pc.cast(pc.round(table["vol"]), pa.int64()),
            "adj_factor": pc.cast(table["adj_factor"], pa.float64()),
            "source": _repeat_string("parquet_1m", rows),
            "source_file": _repeat_string(str(file.path), rows),
            "source_file_mtime": pa.array([source_file_mtime] * rows, type=pa.timestamp("ms")),
            "ingested_at": pa.array([ingested_at] * rows, type=pa.timestamp("ms")),
        }
    )


def _repeat_string(value: str, rows: int) -> pa.Array:
    return pa.array([value] * rows, type=pa.string())
