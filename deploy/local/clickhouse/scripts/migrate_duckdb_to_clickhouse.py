from __future__ import annotations

import argparse
import os
import shlex
import subprocess
import tempfile
from pathlib import Path

import duckdb


DEFAULT_DUCKDB = Path(r"F:\dev-repo\mp4-to-word-new\reference\stock\duckdb\market_data.duckdb")
DEFAULT_TABLES = [
    "daily_raw",
    "daily_qfq",
    "daily_hfq",
    "adj_factors",
    "index_daily_raw",
    "ingest_state",
    "intraday_bars",
    "quotes",
    "schema_meta",
]

TABLE_COLUMNS = {
    "daily_raw": [
        "code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "amount",
        "unit_scale",
        "source",
        "ingested_at",
    ],
    "daily_qfq": [
        "code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "amount",
        "adj_factor",
        "ingested_at",
    ],
    "daily_hfq": [
        "code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "amount",
        "adj_factor",
        "ingested_at",
    ],
    "adj_factors": ["code", "trade_date", "factor", "updated_at"],
    "index_daily_raw": [
        "code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "amount",
        "source",
        "ingested_at",
    ],
    "ingest_state": [
        "code",
        "day_file_path",
        "processed_bytes",
        "last_trade_date",
        "last_unit_scale",
        "last_run_at",
        "last_status",
    ],
    "intraday_bars": [
        "code",
        "period",
        "trade_time",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "amount",
        "source",
    ],
    "quotes": [
        "code",
        "quote_time",
        "price",
        "open",
        "high",
        "low",
        "pre_close",
        "volume",
        "amount",
        "bid_prices",
        "ask_prices",
        "bid_vols",
        "ask_vols",
        "source",
    ],
    "schema_meta": ["key", "value", "updated_at"],
}


def run(cmd: list[str], *, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, check=check, text=True, capture_output=True)


def clickhouse_query(container: str, database: str, user: str, password: str, query: str) -> str:
    result = run(
        [
            "docker",
            "exec",
            container,
            "clickhouse-client",
            "--user",
            user,
            "--password",
            password,
            "--database",
            database,
            "--query",
            query,
        ]
    )
    return result.stdout.strip()


def clickhouse_insert_parquet(
    container: str,
    database: str,
    user: str,
    password: str,
    table: str,
    container_file: str,
) -> None:
    command = (
        f"clickhouse-client --user {shlex.quote(user)} "
        f"--password {shlex.quote(password)} "
        f"--database {shlex.quote(database)} "
        f"--query {shlex.quote(f'INSERT INTO {table} FORMAT Parquet')} "
        f"< {shlex.quote(container_file)}"
    )
    subprocess.run(["docker", "exec", container, "bash", "-lc", command], check=True)


def export_table(con: duckdb.DuckDBPyConnection, table: str, output: Path) -> None:
    columns = TABLE_COLUMNS[table]
    select_list = ", ".join(f'"{column}"' for column in columns)
    sql = f'COPY (SELECT {select_list} FROM "{table}") TO ? (FORMAT PARQUET)'
    con.execute(sql, [str(output)])


def migrate_table(
    con: duckdb.DuckDBPyConnection,
    *,
    table: str,
    container: str,
    database: str,
    user: str,
    password: str,
    temp_dir: Path,
    replace: bool,
) -> None:
    source_count = con.execute(f'select count(*) from "{table}"').fetchone()[0]
    target_count_before = int(clickhouse_query(container, database, user, password, f"SELECT count() FROM {table}") or "0")
    print(f"[{table}] source={source_count} target_before={target_count_before}")

    if replace:
        clickhouse_query(container, database, user, password, f"TRUNCATE TABLE {table}")
        target_count_before = 0
    elif target_count_before == source_count:
        print(f"[{table}] skipped: target row count already matches source")
        return
    elif target_count_before:
        raise RuntimeError(
            f"{table} target has {target_count_before} rows but source has {source_count}. "
            "Run with --replace to truncate and reload."
        )

    if source_count == 0:
        print(f"[{table}] skipped insert: source is empty")
        return

    host_file = temp_dir / f"{table}.parquet"
    container_file = f"/tmp/cynexus_duckdb_{table}.parquet"
    if host_file.exists():
        host_file.unlink()

    print(f"[{table}] exporting parquet -> {host_file}")
    export_table(con, table, host_file)

    print(f"[{table}] copying parquet into {container}:{container_file}")
    subprocess.run(["docker", "cp", str(host_file), f"{container}:{container_file}"], check=True)

    try:
        print(f"[{table}] inserting into ClickHouse")
        clickhouse_insert_parquet(container, database, user, password, table, container_file)
    finally:
        subprocess.run(["docker", "exec", container, "bash", "-lc", f"rm -f {shlex.quote(container_file)}"], check=False)
        host_file.unlink(missing_ok=True)

    target_count_after = int(clickhouse_query(container, database, user, password, f"SELECT count() FROM {table}") or "0")
    print(f"[{table}] target_after={target_count_after}")
    if target_count_after != source_count:
        raise RuntimeError(f"{table} count mismatch: source={source_count}, target={target_count_after}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Migrate remaining DuckDB market tables to local ClickHouse.")
    parser.add_argument("--duckdb", default=str(DEFAULT_DUCKDB), help="Path to source DuckDB database.")
    parser.add_argument("--container", default="cynexus-clickhouse", help="ClickHouse Docker container name.")
    parser.add_argument("--database", default="cynexus", help="ClickHouse database name.")
    parser.add_argument("--user", default="cynexus", help="ClickHouse user.")
    parser.add_argument("--password", default=os.getenv("CLICKHOUSE_PASSWORD", "C020611."), help="ClickHouse password.")
    parser.add_argument("--tables", nargs="*", default=DEFAULT_TABLES, choices=DEFAULT_TABLES, help="Tables to migrate.")
    parser.add_argument("--replace", action="store_true", help="Truncate target tables before loading.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    source = Path(args.duckdb)
    if not source.exists():
        raise FileNotFoundError(source)

    with duckdb.connect(str(source), read_only=True) as con:
        with tempfile.TemporaryDirectory(prefix="cynexus-duckdb-clickhouse-") as tmp:
            temp_dir = Path(tmp)
            for table in args.tables:
                migrate_table(
                    con,
                    table=table,
                    container=args.container,
                    database=args.database,
                    user=args.user,
                    password=args.password,
                    temp_dir=temp_dir,
                    replace=args.replace,
                )


if __name__ == "__main__":
    main()
