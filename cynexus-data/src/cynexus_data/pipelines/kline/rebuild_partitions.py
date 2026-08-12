from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import date
from pathlib import Path
import re
from typing import Any

import pyarrow as pa
import pyarrow.compute as pc

from cynexus_data.config.settings import ClickHouseConfig
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.local.parquet_1m import (
    Parquet1mFile,
    discover_parquet_1m_files,
    read_parquet_1m_arrow,
)
from cynexus_data.storage.clickhouse.client import ClickHouseClient
from cynexus_data.storage.clickhouse.writers import ClickHouseKlineSink


DEFAULT_TARGET_TABLE = "cynexus.ch_dwd_kline_1m"
DEFAULT_STAGE_TABLE = "cynexus.ch_dwd_kline_1m_repair_stage"
DEFAULT_SOURCE_DIRS = ("/app/parquet/stock_1min", r"F:\data\stock_1min")
DEFAULT_YEARS = (2014, 2015, 2019, 2025)
SAFE_EXECUTE_WORKERS = 2
SAFE_EXECUTE_BATCH_ROWS = 25_000
SAFE_EXECUTE_FLUSH_ROWS = 100_000
_IDENTIFIER = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)?$")


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    params = request.params
    years = _parse_years(params.get("years", DEFAULT_YEARS))
    target_table = _safe_identifier(str(params.get("target_table", DEFAULT_TARGET_TABLE)))
    stage_table = _safe_identifier(str(params.get("stage_table", DEFAULT_STAGE_TABLE)))
    if target_table == stage_table:
        raise ValueError("stage_table must be different from target_table")

    source_dir = _resolve_source_dir(params.get("source_dir"))
    max_files = _as_int_or_none(params.get("max_files"))
    execute = _as_bool(params.get("execute"), False) and not request.dry_run
    ingestion = _resolve_ingestion_settings(params, years=years, execute=execute)
    workers = ingestion["workers"]
    batch_rows = ingestion["batch_rows"]
    flush_rows = ingestion["flush_rows"]
    max_partitions = ingestion["max_partitions"]
    keep_stage = _as_bool(params.get("keep_stage"), True)
    if execute and max_files is not None:
        raise ValueError("max_files is a dry-run guard and cannot be used with execute=true")

    config = ClickHouseConfig.from_env()
    client = ClickHouseClient(config)
    _ensure_stage_table(client, target_table, stage_table)
    client.command(f"TRUNCATE TABLE {stage_table}")

    files = discover_parquet_1m_files(source_dir)
    if max_files is not None:
        files = files[:max_files]
    if not files:
        raise ValueError(f"no parquet files found under {source_dir}")
    print(
        f"[rebuild] source_files={len(files)} years={years} workers={workers} "
        f"flush_rows={flush_rows} safe_memory_mode={ingestion['safe_memory_mode']} execute={execute}",
        flush=True,
    )

    source_summary = _populate_stage(
        files,
        years=years,
        stage_table=stage_table,
        config=config,
        workers=workers,
        batch_rows=batch_rows,
        flush_rows=flush_rows,
        max_partitions=max_partitions,
    )
    stage_summary = _validate_stage(client, stage_table, years, source_summary)
    target_summary = _validate_target_scope(client, target_table, years)

    warnings: list[str] = []
    preserved_target_scopes = _preserve_non_parquet_target_scopes(
        client,
        target_table=target_table,
        stage_table=stage_table,
        years=years,
        target_summary=target_summary,
        execute=execute,
    )
    if target_summary["unexpected_scopes"]:
        warnings.append("preserved non-parquet_1m target scopes while replacing the damaged partition")

    replaced: list[int] = []
    if execute:
        for year in years:
            client.command(
                f"ALTER TABLE {target_table} REPLACE PARTITION {year} FROM {stage_table}",
                settings={"mutations_sync": 2},
            )
            replaced.append(year)
            _validate_replaced_partition(
                client,
                target_table,
                stage_summary[str(year)],
                year,
                expected_preserved_scopes=preserved_target_scopes[str(year)],
            )
    else:
        warnings.append("dry-run only: target partitions were not replaced")

    if not keep_stage:
        client.command(f"DROP TABLE IF EXISTS {stage_table}")

    metadata = {
        "source_dir": str(source_dir),
        "target_table": target_table,
        "stage_table": stage_table,
        "years": years,
        "files": len(files),
        "workers": workers,
        "batch_rows": batch_rows,
        "flush_rows": flush_rows,
        "max_partitions_per_insert_block": max_partitions,
        "safe_memory_mode": ingestion["safe_memory_mode"],
        "requested_workers": ingestion["requested_workers"],
        "requested_batch_rows": ingestion["requested_batch_rows"],
        "requested_flush_rows": ingestion["requested_flush_rows"],
        "execute": execute,
        "replaced_years": replaced,
        "source_summary": source_summary,
        "stage_summary": stage_summary,
        "target_summary": target_summary,
        "preserved_target_scopes": preserved_target_scopes,
        "stage_preserved": keep_stage,
    }
    return {
        "success": True,
        "status": "success" if execute else "dry_run",
        "rows_read": sum(source_summary.values()),
        "rows_written": sum(stage_summary[str(year)]["rows"] for year in years),
        "warnings": warnings,
        "metadata": metadata,
    }


def _populate_stage(
    files: list[Parquet1mFile],
    *,
    years: list[int],
    stage_table: str,
    config: ClickHouseConfig,
    workers: int,
    batch_rows: int,
    flush_rows: int,
    max_partitions: int,
) -> dict[str, int]:
    totals = {str(year): 0 for year in years}

    def ingest(files_for_worker: list[Parquet1mFile]) -> dict[str, Any]:
        sink = ClickHouseKlineSink(config, table=stage_table)
        counts = {str(year): 0 for year in years}
        pending: list[pa.Table] = []
        pending_rows = 0

        def flush() -> None:
            nonlocal pending, pending_rows
            if not pending:
                return
            # Keep the repair bounded: a single source table can already be
            # large. Avoid allocating a second concatenated Arrow table when
            # there is only one pending file, then stream it in small blocks.
            payload = pending[0] if len(pending) == 1 else pa.concat_tables(pending)
            sink.insert_arrow_1m(
                payload,
                batch_rows=batch_rows,
                max_partitions_per_insert_block=max_partitions,
            )
            pending = []
            pending_rows = 0

        processed = 0
        for file in files_for_worker:
            table = _read_rebuild_file(file, years)
            pending.append(table)
            pending_rows += table.num_rows
            for item in pc.value_counts(pc.year(table["trade_date"])).to_pylist():
                key = str(item["values"])
                if key in counts:
                    counts[key] += int(item["counts"])
            if pending_rows >= flush_rows:
                flush()
            processed += 1
            if processed % 100 == 0:
                print(f"[rebuild] scanned_files={processed}/{len(files_for_worker)}", flush=True)
        flush()
        return counts

    groups = [[] for _ in range(min(workers, len(files)))]
    for index, file in enumerate(files):
        groups[index % len(groups)].append(file)
    with ThreadPoolExecutor(max_workers=workers) as executor:
        futures = [executor.submit(ingest, group) for group in groups if group]
        for future in as_completed(futures):
            result = future.result()
            for year, rows in result.items():
                totals[year] += rows
    return totals


def _validate_stage(
    client: ClickHouseClient,
    stage_table: str,
    years: list[int],
    expected: dict[str, int],
) -> dict[str, dict[str, Any]]:
    rows = client.query_rows(
        f"""
        SELECT toYear(trade_date), count(), min(trade_date), max(trade_date),
               uniqExact(symbol), uniqExact(source), uniqExact(period)
        FROM {stage_table}
        GROUP BY toYear(trade_date)
        ORDER BY toYear(trade_date)
        """
    )
    summary: dict[str, dict[str, Any]] = {}
    for year, count, minimum, maximum, symbols, sources, periods in rows:
        key = str(year)
        if key not in expected:
            raise ValueError(f"staging contains an unexpected year: {year}")
        if int(count) != expected[key]:
            raise ValueError(f"staging row count mismatch for {year}: source={expected[key]} stage={count}")
        if sources != 1 or periods != 1:
            raise ValueError(f"staging scope mismatch for {year}: sources={sources} periods={periods}")
        summary[key] = {
            "rows": int(count),
            "min_trade_date": str(minimum),
            "max_trade_date": str(maximum),
            "symbols": int(symbols),
            "sources": int(sources),
            "periods": int(periods),
        }

    missing = [str(year) for year in years if str(year) not in summary and expected[str(year)] > 0]
    if missing:
        raise ValueError(f"staging has no rows for years: {', '.join(missing)}")
    for year in years:
        summary.setdefault(str(year), {"rows": 0, "min_trade_date": None, "max_trade_date": None, "symbols": 0, "sources": 0, "periods": 0})
    return {str(year): summary[str(year)] for year in years}


def _validate_target_scope(client: ClickHouseClient, target_table: str, years: list[int]) -> dict[str, Any]:
    rows = client.query_rows(
        f"""
        SELECT toYear(trade_date), source, period, count()
        FROM {target_table}
        WHERE toYear(trade_date) IN ({', '.join(str(year) for year in years)})
        GROUP BY toYear(trade_date), source, period
        ORDER BY toYear(trade_date), source, period
        """
    )
    scopes = [
        {"year": int(year), "source": str(source), "period": int(period), "rows": int(count)}
        for year, source, period, count in rows
    ]
    unexpected = [item for item in scopes if item["source"] != "parquet_1m" or item["period"] != 1]
    return {"scopes": scopes, "unexpected_scopes": unexpected}


def _preserve_non_parquet_target_scopes(
    client: ClickHouseClient,
    *,
    target_table: str,
    stage_table: str,
    years: list[int],
    target_summary: dict[str, Any],
    execute: bool,
) -> dict[str, list[dict[str, Any]]]:
    expected = {
        str(year): [item for item in target_summary["unexpected_scopes"] if item["year"] == year]
        for year in years
    }
    if not execute or not any(expected.values()):
        return expected
    year_filter = ", ".join(str(year) for year in years)
    # REPLACE PARTITION operates on a whole year. Retain other providers'
    # rows in the staging partition so a parquet repair cannot erase the
    # current TDX/Tencent/MooTDX data sharing that partition.
    client.command(
        f"""
        INSERT INTO {stage_table}
        SELECT *
        FROM {target_table}
        WHERE toYear(trade_date) IN ({year_filter})
          AND NOT (source = 'parquet_1m' AND period = 1)
        """,
        settings={
            "max_threads": 1,
            "max_insert_block_size": 100_000,
            "min_insert_block_size_rows": 100_000,
        },
    )
    rows = client.query_rows(
        f"""
        SELECT toYear(trade_date), source, period, count()
        FROM {stage_table}
        WHERE toYear(trade_date) IN ({year_filter})
          AND NOT (source = 'parquet_1m' AND period = 1)
        GROUP BY toYear(trade_date), source, period
        ORDER BY toYear(trade_date), source, period
        """
    )
    actual = {
        str(year): []
        for year in years
    }
    for year, source, period, rows_count in rows:
        actual[str(year)].append(
            {"year": int(year), "source": str(source), "period": int(period), "rows": int(rows_count)}
        )
    for year in years:
        if _scope_signature(actual[str(year)]) != _scope_signature(expected[str(year)]):
            raise ValueError(
                f"failed to preserve non-parquet target scopes for {year}: "
                f"expected={expected[str(year)]!r} actual={actual[str(year)]!r}"
            )
    return actual


def _validate_replaced_partition(
    client: ClickHouseClient,
    target_table: str,
    expected: dict[str, Any],
    year: int,
    *,
    expected_preserved_scopes: list[dict[str, Any]],
) -> None:
    row = client.query_one(
        f"SELECT count(), min(trade_date), max(trade_date), uniqExact(symbol), uniqExact(source), uniqExact(period) "
        f"FROM {target_table} WHERE toYear(trade_date) = {year} AND source = 'parquet_1m' AND period = 1"
    )
    if row is None:
        raise ValueError(f"target partition {year} disappeared after replacement")
    count, minimum, maximum, symbols, sources, periods = row
    if int(count) != int(expected["rows"]) or int(sources) != 1 or int(periods) != 1:
        raise ValueError(f"target partition {year} failed post-replace validation: {row!r}")
    if str(minimum) != str(expected["min_trade_date"]) or str(maximum) != str(expected["max_trade_date"]):
        raise ValueError(f"target partition {year} date range mismatch: {row!r}")
    preserved_rows = client.query_rows(
        f"""
        SELECT toYear(trade_date), source, period, count()
        FROM {target_table}
        WHERE toYear(trade_date) = {year}
          AND NOT (source = 'parquet_1m' AND period = 1)
        GROUP BY toYear(trade_date), source, period
        ORDER BY source, period
        """
    )
    actual_preserved = [
        {"year": int(scope_year), "source": str(source), "period": int(period), "rows": int(rows_count)}
        for scope_year, source, period, rows_count in preserved_rows
    ]
    if _scope_signature(actual_preserved) != _scope_signature(expected_preserved_scopes):
        raise ValueError(
            f"target partition {year} failed preserved-scope validation: "
            f"expected={expected_preserved_scopes!r} actual={actual_preserved!r}"
        )


def _scope_signature(scopes: list[dict[str, Any]]) -> list[tuple[int, str, int, int]]:
    return sorted(
        (int(scope["year"]), str(scope["source"]), int(scope["period"]), int(scope["rows"]))
        for scope in scopes
    )


def _ensure_stage_table(client: ClickHouseClient, target_table: str, stage_table: str) -> None:
    client.command(f"CREATE TABLE IF NOT EXISTS {stage_table} AS {target_table}")


def _resolve_source_dir(value: object) -> Path:
    if value:
        path = Path(str(value)).expanduser()
        if path.exists() and path.is_dir():
            return path.resolve()
        raise ValueError(f"source_dir does not exist or is not a directory: {path}")
    for candidate in DEFAULT_SOURCE_DIRS:
        path = Path(candidate)
        if path.exists() and path.is_dir():
            return path.resolve()
    raise ValueError("source_dir was not provided and no default parquet directory is available")


def _parse_years(value: object) -> list[int]:
    if isinstance(value, str):
        raw = value.replace(",", " ").split()
    elif isinstance(value, (list, tuple, set)):
        raw = list(value)
    else:
        raw = list(DEFAULT_YEARS)
    years = sorted({int(item) for item in raw})
    if not years or any(year < 1990 or year > 2100 for year in years):
        raise ValueError("years must contain valid calendar years")
    return years


def _read_rebuild_file(file: Parquet1mFile, years: list[int]):
    """Read one repair file with Parquet row-group predicate pushdown when possible."""
    if len(years) == 1:
        year = years[0]
        return read_parquet_1m_arrow(
            file,
            start_date=date(year, 1, 1),
            end_date=date(year, 12, 31),
        )
    return read_parquet_1m_arrow(file, years=set(years))


def _resolve_ingestion_settings(params: dict[str, Any], *, years: list[int], execute: bool) -> dict[str, int | bool]:
    requested_workers = min(max(_as_int(params.get("workers"), 2), 1), 8)
    requested_batch_rows = max(_as_int(params.get("batch_rows"), 100_000), 1)
    requested_flush_rows = max(_as_int(params.get("flush_rows"), 1_000_000), requested_batch_rows)
    requested_max_partitions = max(_as_int(params.get("max_partitions_per_insert_block"), 300), 1)
    safe_memory_mode = execute and not _as_bool(params.get("allow_high_memory"), False)
    if not safe_memory_mode:
        return {
            "workers": requested_workers,
            "batch_rows": requested_batch_rows,
            "flush_rows": requested_flush_rows,
            "max_partitions": requested_max_partitions,
            "safe_memory_mode": False,
            "requested_workers": requested_workers,
            "requested_batch_rows": requested_batch_rows,
            "requested_flush_rows": requested_flush_rows,
        }

    # The rebuild feeds Arrow blocks directly to ClickHouse. Two workers each
    # holding one million rows let the server cross its global memory guard.
    # Repair favours bounded memory over throughput; high-memory parallelism
    # requires an explicit operator override.
    return {
        "workers": SAFE_EXECUTE_WORKERS,
        "batch_rows": min(requested_batch_rows, SAFE_EXECUTE_BATCH_ROWS),
        "flush_rows": min(requested_flush_rows, SAFE_EXECUTE_FLUSH_ROWS),
        "max_partitions": min(requested_max_partitions, max(len(years), 1)),
        "safe_memory_mode": True,
        "requested_workers": requested_workers,
        "requested_batch_rows": requested_batch_rows,
        "requested_flush_rows": requested_flush_rows,
    }


def _safe_identifier(value: str) -> str:
    if not _IDENTIFIER.fullmatch(value):
        raise ValueError(f"invalid ClickHouse identifier: {value}")
    return value


def _as_bool(value: object, default: bool) -> bool:
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}


def _as_int(value: object, default: int) -> int:
    try:
        return default if value is None else int(value)
    except (TypeError, ValueError):
        return default


def _as_int_or_none(value: object) -> int | None:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        raise ValueError(f"expected integer, got {value!r}") from None
