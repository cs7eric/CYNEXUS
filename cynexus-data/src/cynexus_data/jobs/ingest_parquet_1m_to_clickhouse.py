from __future__ import annotations

import argparse
import json
import os
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import date
from pathlib import Path
from time import monotonic

from cynexus_data.config.settings import ClickHouseConfig
from cynexus_data.sources.local.parquet_1m import (
    Parquet1mFile,
    discover_parquet_1m_files,
    read_parquet_1m_arrow,
)
from cynexus_data.storage.clickhouse.writers import ClickHouseKlineSink


DEFAULT_STATE = Path("var") / "parquet_1m_ingest_state.jsonl"
_THREAD_LOCAL = threading.local()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Ingest per-symbol 1m parquet files into ClickHouse.")
    parser.add_argument("--dir", required=True, help="Directory containing files like 000001.SZ.parquet.")
    parser.add_argument("--state-file", default=str(DEFAULT_STATE), help="JSONL state file for resume support.")
    parser.add_argument("--create-table", action="store_true", help="Create ClickHouse table before inserting.")
    parser.add_argument("--dry-run", action="store_true", help="Read metadata and one sample table without inserting.")
    parser.add_argument("--limit-files", type=int, help="Limit files for test runs.")
    parser.add_argument("--max-rows-per-file", type=int, help="Read only the first N rows from each file.")
    parser.add_argument(
        "--start-date",
        default="2010-01-01",
        help="Only ingest rows with trade_date >= this date. Use empty string to disable.",
    )
    parser.add_argument("--batch-rows", type=int, default=100_000, help="Arrow rows per ClickHouse insert block.")
    parser.add_argument(
        "--max-partitions-per-insert-block",
        type=int,
        default=300,
        help="ClickHouse max_partitions_per_insert_block setting for each Arrow insert.",
    )
    parser.add_argument("--workers", type=int, default=1, help="Concurrent parquet ingest workers.")
    parser.add_argument("--replace-state", action="store_true", help="Ignore previous successful state entries.")
    parser.add_argument("--clickhouse-host", default=None)
    parser.add_argument("--clickhouse-port", type=int, default=None)
    parser.add_argument("--clickhouse-database", default=None)
    parser.add_argument("--clickhouse-user", default=None)
    parser.add_argument("--clickhouse-password", default=None)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    start_date = _parse_start_date(args.start_date)
    files = discover_parquet_1m_files(Path(args.dir))
    if args.limit_files is not None:
        files = files[: args.limit_files]
    if not files:
        raise SystemExit(f"error: no parquet files found under {args.dir}")

    state_file = Path(args.state_file)
    completed = {} if args.replace_state else _load_completed(state_file)
    pending = [file for file in files if _state_key(file, start_date) not in completed]

    total_rows = sum(file.rows for file in files)
    pending_rows = sum(file.rows for file in pending)
    print(f"[scan] files={len(files)} rows={total_rows} pending_files={len(pending)} pending_rows={pending_rows}")
    if start_date is not None:
        print(f"[filter] trade_date >= {start_date.isoformat()}")

    if args.dry_run:
        _dry_run(files[0], args.max_rows_per_file, start_date)
        return

    clickhouse_config = _clickhouse_config(args)
    sink = ClickHouseKlineSink(clickhouse_config)
    if args.create_table:
        sink.create_table()

    state_file.parent.mkdir(parents=True, exist_ok=True)
    started = monotonic()
    inserted_total = 0
    completed_files = 0
    worker_count = max(1, args.workers)
    with ThreadPoolExecutor(max_workers=worker_count) as executor:
        futures = [
            executor.submit(
                _ingest_one,
                file,
                clickhouse_config,
                args.max_rows_per_file,
                start_date,
                args.batch_rows,
                args.max_partitions_per_insert_block,
            )
            for file in pending
        ]
        try:
            for future in as_completed(futures):
                record = future.result()
                inserted_total += record["rows"]
                completed_files += 1
                _append_state(state_file, record)
                total_elapsed = monotonic() - started
                rate = inserted_total / total_elapsed if total_elapsed > 0 else 0
                print(
                    f"[insert] {completed_files}/{len(pending)} {record['symbol']}.{record['market']} "
                    f"rows={record['rows']} elapsed={record['elapsed_seconds']:.1f}s "
                    f"total_rows={inserted_total} rate={rate:.0f}/s workers={worker_count}"
                )
        except BaseException:
            for future in futures:
                future.cancel()
            raise

    print(f"[done] inserted_rows={inserted_total} files={len(pending)}")


def _dry_run(file: Parquet1mFile, max_rows: int | None, start_date: date | None) -> None:
    table = read_parquet_1m_arrow(file, max_rows=max_rows or 5, start_date=start_date)
    print(f"[sample] {file.path}")
    print(f"source_rows={file.rows} sample_rows={table.num_rows}")
    print(table.schema)
    print(table.to_pandas().head(5).to_string(index=False))


def _load_completed(state_file: Path) -> dict[str, dict]:
    if not state_file.exists():
        return {}
    completed: dict[str, dict] = {}
    for line in state_file.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        record = json.loads(line)
        if record.get("status") == "success":
            completed[_record_key(record)] = record
    return completed


def _append_state(state_file: Path, record: dict) -> None:
    with state_file.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(record, ensure_ascii=False, sort_keys=True))
        handle.write("\n")


def _ingest_one(
    file: Parquet1mFile,
    config: ClickHouseConfig,
    max_rows_per_file: int | None,
    start_date: date | None,
    batch_rows: int,
    max_partitions_per_insert_block: int,
) -> dict:
    file_started = monotonic()
    table = read_parquet_1m_arrow(file, max_rows=max_rows_per_file, start_date=start_date)
    sink = _thread_sink(config)
    sink.insert_arrow_1m(
        table,
        batch_rows=batch_rows,
        max_partitions_per_insert_block=max_partitions_per_insert_block,
    )
    elapsed = monotonic() - file_started
    return {
        "status": "success",
        "path": str(file.path),
        "symbol": file.symbol,
        "market": file.market,
        "rows": table.num_rows,
        "source_rows": file.rows,
        "start_date": start_date.isoformat() if start_date is not None else None,
        "size": file.size,
        "mtime": file.mtime,
        "elapsed_seconds": round(elapsed, 3),
    }


def _thread_sink(config: ClickHouseConfig) -> ClickHouseKlineSink:
    sink = getattr(_THREAD_LOCAL, "sink", None)
    if sink is None:
        sink = ClickHouseKlineSink(config)
        _THREAD_LOCAL.sink = sink
    return sink


def _state_key(file: Parquet1mFile, start_date: date | None = None) -> str:
    start = start_date.isoformat() if start_date is not None else ""
    return f"{file.path}|{file.size}|{file.mtime}|{start}"


def _record_key(record: dict) -> str:
    return f"{record['path']}|{record['size']}|{record['mtime']}|{record.get('start_date') or ''}"


def _parse_start_date(value: str | None) -> date | None:
    if value is None or value.strip() == "":
        return None
    return date.fromisoformat(value)


def _clickhouse_config(args: argparse.Namespace) -> ClickHouseConfig:
    config = ClickHouseConfig.from_env()
    return ClickHouseConfig(
        host=args.clickhouse_host or config.host,
        port=args.clickhouse_port or config.port,
        database=args.clickhouse_database or config.database,
        username=args.clickhouse_user or config.username,
        password=args.clickhouse_password or config.password,
        secure=config.secure,
        send_receive_timeout=config.send_receive_timeout,
    )


if __name__ == "__main__":
    main()
