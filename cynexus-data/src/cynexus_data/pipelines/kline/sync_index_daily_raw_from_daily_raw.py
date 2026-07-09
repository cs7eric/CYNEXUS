from __future__ import annotations

from datetime import date
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.ddl import create_index_daily_raw


DEFAULT_INDEX_CODES = ["sh000001", "sh000300", "sh000852", "sz399001", "sz399006"]


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    params = request.params or {}
    source_table = str(params.get("source_table") or "daily_raw")
    target_table = str(params.get("target_table") or "index_daily_raw")
    index_codes = _normalize_codes(params.get("index_codes") or params.get("codes") or DEFAULT_INDEX_CODES)
    start_date = _parse_date(params.get("start_date") or params.get("trade_date") or params.get("date"))
    end_date = _parse_date(params.get("end_date") or params.get("trade_date") or params.get("date"))
    create_table = _truthy(params.get("create_table"))
    result = {
        "pipeline": "kline.sync_index_daily_raw_from_daily_raw",
        "dry_run": request.dry_run,
        "source_table": source_table,
        "target_table": target_table,
        "index_codes": index_codes,
        "start_date": start_date.isoformat() if start_date else None,
        "end_date": end_date.isoformat() if end_date else None,
        "rows_read": 0,
        "rows_written": 0,
    }
    if request.dry_run:
        return result

    client = context.hooks.clickhouse.get_client()
    if create_table:
        create_index_daily_raw(client)

    rows_read = _count_source_rows(client, source_table, index_codes, start_date, end_date)
    _delete_target_rows(client, target_table, index_codes, start_date, end_date)
    rows_written = _insert_target_rows(client, source_table, target_table, index_codes, start_date, end_date)
    return {**result, "rows_read": rows_read, "rows_written": rows_written}


def _count_source_rows(client, source_table: str, index_codes: list[str], start_date: date | None, end_date: date | None) -> int:
    sql = f"SELECT count() FROM {_safe_table(source_table)} FINAL WHERE {_source_where(start_date, end_date)}"
    row = client.query_one(sql, _params(index_codes, start_date, end_date))
    return int(row[0]) if row else 0


def _delete_target_rows(client, target_table: str, index_codes: list[str], start_date: date | None, end_date: date | None) -> None:
    where = ["code IN %(index_codes)s"]
    if start_date is not None:
        where.append("trade_date >= %(start_date)s")
    if end_date is not None:
        where.append("trade_date <= %(end_date)s")
    client.command(
        f"ALTER TABLE {_safe_table(target_table)} DELETE WHERE {' AND '.join(where)}",
        _params(index_codes, start_date, end_date),
        settings={"mutations_sync": 1},
    )


def _insert_target_rows(client, source_table: str, target_table: str, index_codes: list[str], start_date: date | None, end_date: date | None) -> int:
    sql = f"""
        INSERT INTO {_safe_table(target_table)}
            (code, trade_date, open, high, low, close, volume, amount, source, ingested_at)
        SELECT
            tdx_code AS code,
            trade_date,
            toDecimal64(argMax(open, ingested_at), 4) AS open,
            toDecimal64(argMax(high, ingested_at), 4) AS high,
            toDecimal64(argMax(low, ingested_at), 4) AS low,
            toDecimal64(argMax(close, ingested_at), 4) AS close,
            toInt64(argMax(volume, ingested_at)) AS volume,
            toDecimal64(argMax(amount, ingested_at), 4) AS amount,
            argMax(source, ingested_at) AS source,
            now64(3) AS ingested_at
        FROM {_safe_table(source_table)} FINAL
        WHERE {_source_where(start_date, end_date)}
        GROUP BY tdx_code, trade_date
    """
    client.command(sql, _params(index_codes, start_date, end_date))
    row = client.query_one(f"SELECT count() FROM {_safe_table(target_table)} WHERE {' AND '.join(_target_where(start_date, end_date))}", _params(index_codes, start_date, end_date))
    return int(row[0]) if row else 0


def _source_where(start_date: date | None, end_date: date | None) -> str:
    where = ["tdx_code IN %(index_codes)s"]
    if start_date is not None:
        where.append("trade_date >= %(start_date)s")
    if end_date is not None:
        where.append("trade_date <= %(end_date)s")
    return " AND ".join(where)


def _target_where(start_date: date | None, end_date: date | None) -> list[str]:
    where = ["code IN %(index_codes)s"]
    if start_date is not None:
        where.append("trade_date >= %(start_date)s")
    if end_date is not None:
        where.append("trade_date <= %(end_date)s")
    return where


def _params(index_codes: list[str], start_date: date | None, end_date: date | None) -> dict[str, Any]:
    params: dict[str, Any] = {"index_codes": index_codes}
    if start_date is not None:
        params["start_date"] = start_date
    if end_date is not None:
        params["end_date"] = end_date
    return params


def _normalize_codes(value: Any) -> list[str]:
    if isinstance(value, str):
        items = [item.strip().lower() for item in value.split(",") if item.strip()]
    else:
        items = [str(item).strip().lower() for item in value if str(item).strip()]
    return items or DEFAULT_INDEX_CODES


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    text = str(value)
    if len(text) == 8 and text.isdigit():
        text = f"{text[:4]}-{text[4:6]}-{text[6:8]}"
    return date.fromisoformat(text[:10])


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}


def _safe_table(table: str) -> str:
    if not table.replace("_", "").replace(".", "").isalnum():
        raise ValueError(f"unsafe ClickHouse table name: {table}")
    return table
