from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import date, datetime, timedelta
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

_PARQUET_FILE_NAME = re.compile(
    r"(?P<symbol>\d{6})\.(?P<market>[A-Za-z]{2})(?:\((?P<copy>\d+)\))?\.parquet"
)


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
    files_by_code: dict[str, Parquet1mFile] = {}
    for path in base.glob("*.parquet"):
        candidate = parse_parquet_1m_file(path)
        existing = files_by_code.get(candidate.ts_code)
        if existing is None or _prefer_parquet_1m_file(candidate, existing):
            files_by_code[candidate.ts_code] = candidate
    return sorted(files_by_code.values(), key=lambda item: item.path.name)


def parse_parquet_1m_file(path: Path) -> Parquet1mFile:
    resolved = path.expanduser().resolve()
    match = _PARQUET_FILE_NAME.fullmatch(resolved.name)
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


def _prefer_parquet_1m_file(candidate: Parquet1mFile, existing: Parquet1mFile) -> bool:
    """Select the most complete copy when a local export contains name collisions.

    Windows copy operations append ``(1)`` to duplicate filenames.  The local
    1m archive may therefore contain two exports for the same security; using
    both would duplicate every bar.  The export with more rows is the more
    complete one.  Exact ties keep the canonical filename when present, then
    use the latest mtime as a deterministic fallback.
    """
    if candidate.rows != existing.rows:
        return candidate.rows > existing.rows
    candidate_copy = "(" in candidate.path.stem
    existing_copy = "(" in existing.path.stem
    if candidate_copy != existing_copy:
        return not candidate_copy
    if candidate.mtime != existing.mtime:
        return candidate.mtime > existing.mtime
    return candidate.path.name < existing.path.name


def read_parquet_1m_arrow(
    file: Parquet1mFile,
    *,
    max_rows: int | None = None,
    start_date: date | None = None,
    end_date: date | None = None,
    years: set[int] | None = None,
) -> pa.Table:
    if years is not None and (start_date is not None or end_date is not None):
        raise ValueError("years cannot be combined with start_date or end_date")
    if start_date is not None and end_date is not None and end_date < start_date:
        raise ValueError("end_date must be greater than or equal to start_date")

    # Keep the source scan bounded to the requested year/window.  The source
    # files are often large single-row-group Parquets, so selecting only the
    # columns needed by the kline contract avoids loading unrelated payloads.
    filters = []
    if start_date is not None:
        filters.append(("trade_date", ">=", datetime.combine(start_date, datetime.min.time())))
    if end_date is not None:
        end_exclusive = end_date + timedelta(days=1)
        filters.append(("trade_date", "<", datetime.combine(end_exclusive, datetime.min.time())))
    table = pq.read_table(file.path, columns=sorted(EXPECTED_COLUMNS), filters=filters or None)
    missing = EXPECTED_COLUMNS - set(table.column_names)
    if missing:
        raise ValueError(f"{file.path} is missing columns: {sorted(missing)}")
    if start_date is not None or end_date is not None:
        trade_date = table["trade_date"]
        if start_date is not None:
            trade_date = pc.greater_equal(
                trade_date,
                pa.scalar(datetime.combine(start_date, datetime.min.time()), type=pa.timestamp("ns")),
            )
        if end_date is not None:
            end_exclusive = end_date + timedelta(days=1)
            upper = pc.less(
                table["trade_date"],
                pa.scalar(datetime.combine(end_exclusive, datetime.min.time()), type=pa.timestamp("ns")),
            )
            trade_date = pc.and_(trade_date, upper) if start_date is not None else upper
        table = table.filter(trade_date)
    if years is not None:
        if not years:
            table = table.slice(0, 0)
        else:
            table = table.filter(pc.is_in(pc.year(table["trade_date"]), value_set=pa.array(sorted(years))))
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
