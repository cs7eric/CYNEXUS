from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.msi_factors import build_limit_snapshot_rows, calculate_limit_emotion, extract_limit_stock_snapshots
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_object, parse_rows, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import get_previous_table_trade_date, read_limit_emotion_history_ohlcv_rows, read_limit_emotion_ohlcv_rows
from cynexus_data.storage.postgres.market_limit import MarketLimitRepository
from cynexus_data.storage.postgres.msi import MsiRepository
from cynexus_data.storage.postgres.percentile import PercentileRepository
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = parse_date(request.params.get("trade_date")) or date.today()
    prev_trade_date = parse_date(request.params.get("prev_trade_date"))
    today_rows = parse_rows(request.params.get("today_rows") or request.params.get("rows"))
    prev_rows = parse_rows(request.params.get("prev_rows"))
    score_overrides = parse_object(request.params.get("score_overrides")) or {}
    result = {"pipeline": "msi.build_limit_emotion", "dry_run": request.dry_run, "trade_date": trade_date.isoformat(), "rows_read": 0, "rows_written": 0, "stock_rows_written": 0}
    if request.dry_run:
        payload = calculate_limit_emotion(trade_date=trade_date, prev_trade_date=prev_trade_date, today_rows=today_rows or [], prev_rows=prev_rows or [], score_overrides=score_overrides)
        stock_rows = extract_limit_stock_snapshots(today_rows or [], trade_date=trade_date)
        return {**result, "rows_read": len(today_rows or []) + len(prev_rows or []), "payload": payload, "stock_rows": stock_rows, "notes": ["dry_run skipped ClickHouse and Postgres access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    ch = context.hooks.clickhouse.get_client()
    table = str(request.params.get("source_table") or "daily_raw")
    if prev_trade_date is None:
        prev_trade_date = get_previous_table_trade_date(ch, trade_date, table=table)
    streak_lookback_days = int(request.params.get("streak_lookback_days") or 120)
    stock_meta, meta_note = _load_stock_meta(context)
    if today_rows is None:
        today_ohlcv = read_limit_emotion_ohlcv_rows(ch, trade_date, table=table)
        today_ohlcv = _filter_active_stock_rows(today_ohlcv, stock_meta)
        codes = [str(row["code"]) for row in today_ohlcv if row.get("code")]
        tdx_codes = [str(row["tdxCode"]) for row in today_ohlcv if row.get("tdxCode")]
        history_rows = read_limit_emotion_history_ohlcv_rows(ch, trade_date, codes=codes, tdx_codes=tdx_codes, lookback_days=streak_lookback_days, table=table)
        today_rows = build_limit_snapshot_rows(today_ohlcv, stock_meta=stock_meta, history_rows=history_rows)
    if prev_rows is None and prev_trade_date:
        prev_ohlcv = read_limit_emotion_ohlcv_rows(ch, prev_trade_date, table=table)
        prev_ohlcv = _filter_active_stock_rows(prev_ohlcv, stock_meta)
        prev_codes = [str(row["code"]) for row in prev_ohlcv if row.get("code")]
        prev_tdx_codes = [str(row["tdxCode"]) for row in prev_ohlcv if row.get("tdxCode")]
        prev_history_rows = read_limit_emotion_history_ohlcv_rows(ch, prev_trade_date, codes=prev_codes, tdx_codes=prev_tdx_codes, lookback_days=streak_lookback_days, table=table)
        prev_rows = build_limit_snapshot_rows(prev_ohlcv, stock_meta=stock_meta, history_rows=prev_history_rows)
    elif prev_rows is None:
        prev_rows = []
    payload = calculate_limit_emotion(trade_date=trade_date, prev_trade_date=prev_trade_date, today_rows=today_rows, prev_rows=prev_rows, score_overrides=score_overrides)
    payload["source"] = f"clickhouse.{table}"
    pg_client = context.hooks.postgres.get_client()
    if not score_overrides:
        _apply_history_scores(payload, PercentileRepository(pg_client), trade_date)
    repo = MsiRepository(pg_client)
    stock_repo = MarketLimitRepository(pg_client)
    if truthy(request.params.get("create_table")):
        repo.create_table()
        stock_repo.create_table()
    stock_rows = extract_limit_stock_snapshots(today_rows, trade_date=trade_date)
    result["rows_read"] = len(today_rows) + len(prev_rows)
    result["rows_written"] = repo.save_limit_emotion(payload)
    result["serving_rows_written"] = stock_repo.upsert_daily_snapshot(payload=payload, stock_rows=stock_rows, source=str(payload.get("source") or "clickhouse.daily_raw"))
    result["stock_rows_written"] = stock_repo.replace_stock_snapshots(trade_date=trade_date, rows=stock_rows, source=str(payload.get("source") or "clickhouse.daily_raw"))
    result["payload"] = payload
    if meta_note:
        result["notes"] = [meta_note]
    return result


def _apply_history_scores(payload: dict, percentile_repo: PercentileRepository, trade_date: date) -> None:
    components = dict(payload.get("components") or {})
    up_down_score = percentile_repo.percentile_score("limit_emotion_summary_daily", "limit_up_down_ratio", trade_date, payload.get("limitUpDownRatio"))
    if up_down_score is not None:
        components["upDownScore"] = up_down_score
    break_rate = payload.get("breakBoardRate")
    if break_rate is not None:
        break_percentile = percentile_repo.percentile_score("limit_emotion_summary_daily", "break_board_rate", trade_date, break_rate)
        if break_percentile is not None:
            components["breakBoardScore"] = round(100.0 - float(break_percentile), 2)
    yesterday_return = payload.get("yesterdayLimitUpAvgReturn")
    if yesterday_return is not None:
        yesterday_score = percentile_repo.percentile_score("limit_emotion_summary_daily", "yesterday_limit_up_avg_return", trade_date, yesterday_return)
        if yesterday_score is not None:
            components["yesterdayReturnScore"] = yesterday_score
    payload["components"] = components
    composite_raw = round(
        0.4 * float(components.get("upDownScore") or 0)
        + 0.3 * float(components.get("breakBoardScore") or 0)
        + 0.3 * float(components.get("yesterdayReturnScore") or 0),
        2,
    )
    composite_score = percentile_repo.percentile_score("limit_emotion_summary_daily", "composite_score", trade_date, composite_raw)
    payload["compositeScore"] = composite_score if composite_score is not None else composite_raw
    payload["level"] = _level_for_score(payload["compositeScore"])


def _level_for_score(score: float | int | None) -> str:
    value = float(score or 0)
    if value >= 70:
        return "hot"
    if value >= 55:
        return "active"
    if value >= 40:
        return "normal"
    if value >= 30:
        return "weak"
    return "ice"


def _load_stock_meta(context: PipelineContext) -> tuple[dict[str, dict], str | None]:
    try:
        if context.hooks is None:
            return {}, "stock meta unavailable: hooks missing"
        return StockBasicRepository(context.hooks.postgres.get_client()).list_limit_meta(active_only=True), None
    except Exception as exc:  # noqa: BLE001 - fallback keeps the factor buildable when sec_stock_basic is not initialized.
        return {}, f"stock meta unavailable, used code-rule thresholds: {exc}"


def _filter_active_stock_rows(rows: list[dict], stock_meta: dict[str, dict]) -> list[dict]:
    if not stock_meta:
        return rows
    filtered = []
    for row in rows:
        code = str(row.get("code") or row.get("symbol") or "").zfill(6)
        meta = stock_meta.get(code)
        if not meta:
            continue
        row_market = str(row.get("market") or "").upper()
        meta_market = str(meta.get("market") or "").upper()
        if row_market and meta_market and row_market != meta_market:
            continue
        filtered.append(row)
    return filtered
