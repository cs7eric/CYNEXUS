from __future__ import annotations

import json
from datetime import date
from pathlib import Path

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.domain.market.style_sector import STYLE_CATEGORY_RAW, build_style_sector_snapshots
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_object, parse_rows, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import read_daily_change_pct_by_codes
from cynexus_data.storage.postgres.stock_sector import StockSectorRepository
from cynexus_data.storage.postgres.style_sector import StyleSectorRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = parse_date(request.params.get("trade_date")) or date.today()
    sectors = parse_rows(request.params.get("sectors"))
    quotes_by_code = parse_object(request.params.get("quotes_by_code")) or {}
    result = {
        "pipeline": "market.build_style_sector",
        "dry_run": request.dry_run,
        "trade_date": trade_date.isoformat(),
        "rows_read": 0,
        "rows_written": 0,
    }
    if request.dry_run:
        snapshots = build_style_sector_snapshots(sectors or [], quotes_by_code, trade_date=trade_date)
        return {**result, "rows_read": len(sectors or []), "planned_rows": len(snapshots), "payload": {"items": snapshots}, "notes": ["dry_run skipped Postgres and Tencent access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    pg_client = context.hooks.postgres.get_client()
    sector_repo = StockSectorRepository(pg_client)
    style_repo = StyleSectorRepository(pg_client)
    if truthy(request.params.get("create_table")):
        sector_repo.create_table()
        style_repo.create_table()
    if sectors is None:
        sectors = sector_repo.list_sectors_by_category(STYLE_CATEGORY_RAW)
    if not sectors:
        sectors = _load_style_universe(request.params.get("style_universe_path"))
    codes = _unique_codes(sectors)
    quote_source = str(request.params.get("quote_source") or request.params.get("source") or "clickhouse.daily_qfq")
    if not quotes_by_code:
        if quote_source in {"clickhouse.daily_qfq", "daily_qfq", "clickhouse"}:
            source_table = str(request.params.get("source_table") or "daily_qfq")
            quotes_by_code = read_daily_change_pct_by_codes(context.hooks.clickhouse.get_client(), codes, trade_date, table=source_table)
            quote_source = f"clickhouse.{source_table}+stock-universe.styles"
        else:
            batch_size = int(request.params.get("batch_size") or 500)
            batch_sleep = float(request.params.get("batch_sleep") or 0.22)
            quotes_by_symbol = context.hooks.tencent.get_client().fetch_realtime_snapshots_batch(codes, batch_size=batch_size, batch_sleep=batch_sleep)
            quotes_by_code = _index_quotes_by_code(quotes_by_symbol)
    snapshots = build_style_sector_snapshots(sectors, quotes_by_code, trade_date=trade_date)
    rows_written = style_repo.replace_snapshots(trade_date=trade_date, rows=snapshots, source=quote_source)
    return {
        **result,
        "rows_read": len(sectors),
        "quote_count": len(quotes_by_code),
        "planned_rows": len(snapshots),
        "rows_written": rows_written,
        "payload": {"items": snapshots},
    }


def _load_style_universe(path_value) -> list[dict]:
    path = Path(str(path_value)) if path_value else PROJECT_ROOT / "var" / "stock-universe" / "sectors" / "sectors_styles_4.json"
    if not path.exists():
        return []
    payload = json.loads(path.read_text(encoding="utf-8"))
    sectors = payload.get("sectors") if isinstance(payload, dict) else payload
    if not isinstance(sectors, list):
        return []
    out: list[dict] = []
    for sector in sectors:
        if not isinstance(sector, dict):
            continue
        codes = [str(code)[-6:].zfill(6) for code in sector.get("stock_codes") or sector.get("codes") or [] if str(code or "").strip()]
        out.append(
            {
                "name": sector.get("name"),
                "stock_codes": codes,
                "stock_count": len(codes),
                "source": str(path),
            }
        )
    return out


def _unique_codes(sectors: list[dict]) -> list[str]:
    seen: set[str] = set()
    out: list[str] = []
    for sector in sectors:
        for raw_code in sector.get("stock_codes") or sector.get("codes") or []:
            code = str(raw_code or "").zfill(6)
            if not code or code == "000000" or code in seen:
                continue
            seen.add(code)
            out.append(code)
    return out


def _index_quotes_by_code(quotes_by_symbol: dict[str, dict]) -> dict[str, dict]:
    out: dict[str, dict] = {}
    for symbol, row in quotes_by_symbol.items():
        code = str(row.get("code") or symbol)[-6:].zfill(6)
        out[code] = row
        out[str(symbol)] = row
    return out
