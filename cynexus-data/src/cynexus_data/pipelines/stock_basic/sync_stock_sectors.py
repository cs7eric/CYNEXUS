from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.stock_topics import create_eltdx_client, fetch_stock_topics, stock_topics_to_sector_mappings
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository
from cynexus_data.storage.postgres.stock_sector import StockSectorRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    codes = _normalize_codes(request.params.get("codes") or request.params.get("symbols"))
    limit_codes = request.params.get("limit_codes")
    create_table = _truthy(request.params.get("create_table"))
    pool_size = int(request.params.get("pool_size") or 8)
    timeout = float(request.params.get("timeout") or 8)
    if codes and limit_codes is not None:
        codes = codes[: int(limit_codes)]
    result = {
        "pipeline": "stock_basic.sync_stock_sectors",
        "dry_run": request.dry_run,
        "provider": "eltdx",
        "planned_codes": len(codes or []),
        "rows_read": 0,
        "rows_written": 0,
        "code_results": [],
    }
    if request.dry_run:
        result["notes"] = ["dry_run skipped eltdx and Postgres access"]
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    pg = context.hooks.postgres.get_client()
    if not codes:
        codes = StockBasicRepository(pg).list_codes(active_only=True)
        if limit_codes is not None:
            codes = codes[: int(limit_codes)]
        result["planned_codes"] = len(codes)
        result["code_source"] = "postgres.sec_stock_basic"
    if not codes:
        raise ValueError("codes/symbols are required or sec_stock_basic must contain active codes")

    repo = StockSectorRepository(pg)
    if create_table:
        repo.create_table()
    client = create_eltdx_client(pool_size=pool_size, timeout=timeout)
    total_topics = 0
    total_written = 0
    try:
        for code in codes:
            topics = fetch_stock_topics(client, code)
            mappings = stock_topics_to_sector_mappings(code, topics)
            written = repo.replace_stock_mappings(code, mappings)
            total_topics += len(topics)
            total_written += written
            result["code_results"].append({"code": code, "topics": len(topics), "mappings_written": written})
    finally:
        close = getattr(client, "close", None)
        if callable(close):
            close()
    result["rows_read"] = total_topics
    result["rows_written"] = total_written
    return {**result, "code_results": result["code_results"][:50], "code_results_truncated": len(result["code_results"]) > 50}


def _normalize_codes(value) -> list[str] | None:
    if value in (None, "", []):
        return None
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
