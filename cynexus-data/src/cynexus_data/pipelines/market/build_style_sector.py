from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.style_sector import STYLE_CATEGORY_RAW, build_style_sector_snapshots
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_object, parse_rows, truthy
from cynexus_data.runtime.context import PipelineContext
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
    codes = _unique_codes(sectors)
    batch_size = int(request.params.get("batch_size") or 500)
    batch_sleep = float(request.params.get("batch_sleep") or 0.22)
    if not quotes_by_code:
        quotes_by_symbol = context.hooks.tencent.get_client().fetch_realtime_snapshots_batch(codes, batch_size=batch_size, batch_sleep=batch_sleep)
        quotes_by_code = _index_quotes_by_code(quotes_by_symbol)
    snapshots = build_style_sector_snapshots(sectors, quotes_by_code, trade_date=trade_date)
    rows_written = style_repo.replace_snapshots(trade_date=trade_date, rows=snapshots, source="tencent.qt")
    return {
        **result,
        "rows_read": len(sectors),
        "quote_count": len(quotes_by_code),
        "planned_rows": len(snapshots),
        "rows_written": rows_written,
        "payload": {"items": snapshots},
    }


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
