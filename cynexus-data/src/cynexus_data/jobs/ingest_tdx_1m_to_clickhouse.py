from __future__ import annotations

import argparse
import os
from pathlib import Path

from cynexus_data.config.settings import ClickHouseConfig
from cynexus_data.sources.tdx.local_minute import (
    TdxMinuteFile,
    discover_1m_files,
    iter_tdx_1m_frames,
    parse_tdx_minute_file,
)
from cynexus_data.storage.clickhouse.writers import ClickHouseKlineSink


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Ingest local TongDaXin 1m K files into ClickHouse.")
    source = parser.add_mutually_exclusive_group()
    source.add_argument("--tdx-root", default=os.getenv("TDX_ROOT"), help="TongDaXin installation root, e.g. C:\\new_tdx.")
    source.add_argument("--file", action="append", help="One local .lc1 or .1 file. Can be specified multiple times.")

    parser.add_argument("--market", choices=["SH", "SZ", "BJ", "sh", "sz", "bj"], help="Override market for --file.")
    parser.add_argument("--symbol", help="Override 6 digit symbol for --file.")
    parser.add_argument("--limit-files", type=int, help="Limit discovered files for POC runs.")
    parser.add_argument("--create-table", action="store_true", help="Create ClickHouse table before inserting.")
    parser.add_argument("--dry-run", action="store_true", help="Read and normalize files without writing ClickHouse.")
    parser.add_argument("--batch-size", type=int, default=50_000, help="ClickHouse insert batch size.")

    parser.add_argument("--clickhouse-host", default=None)
    parser.add_argument("--clickhouse-port", type=int, default=None)
    parser.add_argument("--clickhouse-database", default=None)
    parser.add_argument("--clickhouse-user", default=None)
    parser.add_argument("--clickhouse-password", default=None)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    files = _resolve_files(args)
    if not files:
        raise SystemExit("error: No TDX 1m files found.")

    sink = None
    if not args.dry_run:
        sink = ClickHouseKlineSink(_clickhouse_config(args))
        if args.create_table:
            sink.create_table()

    total_rows = 0
    for file, frame in iter_tdx_1m_frames(files):
        rows = len(frame)
        total_rows += rows
        print(f"[read] {file.tdx_code} {file.reader_kind} rows={rows} path={file.path}")
        if args.dry_run:
            if rows:
                print(frame.head(3).to_string(index=False))
                print(frame.tail(3).to_string(index=False))
            continue

        assert sink is not None
        inserted = sink.insert_1m(frame, batch_size=args.batch_size)
        print(f"[insert] {file.tdx_code} rows={inserted}")

    action = "read" if args.dry_run else "inserted"
    print(f"[done] files={len(files)} rows_{action}={total_rows}")


def _resolve_files(args: argparse.Namespace) -> list[TdxMinuteFile]:
    if args.file:
        return [
            parse_tdx_minute_file(Path(path), market=args.market, symbol=args.symbol)
            for path in args.file
        ]

    if not args.tdx_root:
        raise SystemExit("error: --tdx-root is required when TDX_ROOT is not set.")

    files = discover_1m_files(Path(args.tdx_root))
    if args.limit_files is not None:
        files = files[: args.limit_files]
    if not files:
        raise SystemExit(
            f"error: No local TDX 1m files found under {args.tdx_root}. "
            "Expected files like vipdoc/sh/minline/sh600000.lc1, "
            "vipdoc/sz/minline/sz000001.lc1, or vipdoc/*/fzline/*.1. "
            "Download minute data in TongDaXin first, then rerun this job."
        )
    return files


def _clickhouse_config(args: argparse.Namespace) -> ClickHouseConfig:
    config = ClickHouseConfig.from_env()
    return ClickHouseConfig(
        host=args.clickhouse_host or config.host,
        port=args.clickhouse_port or config.port,
        database=args.clickhouse_database or config.database,
        username=args.clickhouse_user or config.username,
        password=args.clickhouse_password or config.password,
        secure=config.secure,
    )


if __name__ == "__main__":
    main()
