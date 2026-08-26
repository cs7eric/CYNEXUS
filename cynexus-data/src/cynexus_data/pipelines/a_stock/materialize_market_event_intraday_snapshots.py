from __future__ import annotations

from datetime import date, datetime, timedelta, timezone
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import read_event_minute_bars
from cynexus_data.storage.postgres.market_event_intraday_snapshot import MarketEventIntradaySnapshotRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    code = str(request.params.get("code") or "").strip()[-6:]
    trade_date = date.fromisoformat(str(request.params.get("trade_date") or ""))
    anchor_at = _anchor_at(request.params.get("anchor_at"), trade_date)
    pre_minutes = _bounded_int(request.params.get("pre_minutes"), 120, 1, 240)
    post_minutes = _bounded_int(request.params.get("post_minutes"), 30, 0, 60)
    result = {"pipeline": "a_stock.materialize_market_event_intraday_snapshots", "code": code,
              "trade_date": trade_date.isoformat(), "anchor_at": anchor_at.isoformat(), "dry_run": request.dry_run,
              "rows_read": 0, "rows_written": 0, "warnings": []}
    if len(code) != 6 or not code.isdigit():
        raise ValueError("code must be a six-digit stock code")
    if request.dry_run:
        return {**result, "notes": ["dry_run skips ClickHouse and PostgreSQL access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    bars = read_event_minute_bars(context.hooks.clickhouse.get_client(), code=code, trade_date=trade_date,
                                  start_at=anchor_at - timedelta(minutes=pre_minutes), end_at=anchor_at + timedelta(minutes=post_minutes))
    if not bars:
        return {**result, "success": False, "error": "stock 1m source is unavailable for the requested event window",
                "warnings": ["STOCK_1M_UNAVAILABLE"]}
    primitive_fields = (
        ("stock.open", "open"), ("stock.high", "high"), ("stock.low", "low"), ("stock.close", "close"),
        ("stock.volume", "volume"), ("stock.amount", "amount"),
    )
    records = [
        {"minute_at": _as_shanghai(bar["minute_at"]), "primitive_code": primitive, "value": bar.get(field)}
        for bar in bars for primitive, field in primitive_fields
        if bar.get(field) is not None
    ]
    market = "SH" if code.startswith("6") else "SZ"
    written = MarketEventIntradaySnapshotRepository(context.hooks.postgres.get_client()).replace_event(
        code=code, market=market, trade_date=trade_date.isoformat(), anchor_at=anchor_at, records=records)
    checkpoint = {"status": "completed", "code": code, "trade_date": trade_date.isoformat(), "anchor_at": anchor_at.isoformat(),
                  "rows_read": len(records), "rows_written": written}
    context.checkpoints.put(str(request.params.get("checkpoint_key") or "a_stock.market_event_intraday.watermark"), request.job_key, checkpoint)
    if written != len(records):
        return {**result, "rows_read": len(records), "rows_written": written, "success": False, "error": "event intraday snapshot coverage does not match source"}
    return {**result, "rows_read": len(records), "rows_written": written, "watermark": checkpoint}


def _anchor_at(value: Any, trade_date: date) -> datetime:
    if value:
        parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
        return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone(timedelta(hours=8)))
    return datetime.combine(trade_date, datetime.min.time(), tzinfo=timezone(timedelta(hours=8))).replace(hour=15)


def _bounded_int(value: Any, default: int, minimum: int, maximum: int) -> int:
    return max(minimum, min(maximum, int(value if value is not None else default)))


def _as_shanghai(value: datetime) -> datetime:
    return value.replace(tzinfo=timezone(timedelta(hours=8))) if value.tzinfo is None else value.astimezone(timezone(timedelta(hours=8)))
