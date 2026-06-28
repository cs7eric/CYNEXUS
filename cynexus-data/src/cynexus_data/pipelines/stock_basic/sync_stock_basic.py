from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.domain.stock.stock_meta import normalize_stock_basic_record
from cynexus_data.sources.akshare.client import AkshareClient
from cynexus_data.sources.akshare.stock_basic import fetch_stock_code_name
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    providers = _normalize_providers(request.params.get("providers") or ["akshare"])
    create_table = _truthy(request.params.get("create_table"))
    limit = request.params.get("limit")
    codes = _normalize_codes(request.params.get("codes") or request.params.get("symbols"))
    result = {
        "pipeline": "stock_basic.sync_stock_basic",
        "dry_run": request.dry_run,
        "providers": providers,
        "planned_codes": len(codes or []),
        "rows_read": 0,
        "rows_written": 0,
        "provider_results": [],
    }
    if request.dry_run:
        result["notes"] = ["dry_run skipped provider and Postgres access"]
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    repo = StockBasicRepository(context.hooks.postgres.get_client())
    if create_table:
        repo.create_table()
    total_records: list[dict] = []
    warnings: list[str] = []

    if "akshare" in providers:
        records = fetch_stock_code_name(AkshareClient())
        if limit is not None:
            records = records[: int(limit)]
        total_records.extend(records)
        result["provider_results"].append({"provider": "akshare", "rows_read": len(records)})

    if "eastmoney" in providers:
        eastmoney_codes = list(codes or [])
        if not eastmoney_codes:
            eastmoney_codes = repo.list_codes(active_only=True)
            result["code_source"] = "postgres.sec_stock_basic"
        if limit is not None:
            eastmoney_codes = eastmoney_codes[: int(limit)]
        if not eastmoney_codes:
            raise ValueError("codes/symbols are required for eastmoney stock meta or sec_stock_basic must contain active codes")
        client = context.hooks.eastmoney.get_client()
        eastmoney_records: list[dict] = []
        for code in eastmoney_codes:
            try:
                eastmoney_records.append(normalize_stock_basic_record(client.fetch_stock_meta(code)))
            except Exception as exc:
                warnings.append(f"eastmoney {code}: {exc}")
        total_records.extend(eastmoney_records)
        result["planned_codes"] = len(eastmoney_codes)
        result["provider_results"].append({"provider": "eastmoney", "rows_read": len(eastmoney_records), "requested_codes": len(eastmoney_codes)})

    unsupported = [provider for provider in providers if provider not in {"akshare", "eastmoney"}]
    if unsupported:
        warnings.append(f"unsupported stock_basic providers ignored: {','.join(unsupported)}")

    written = repo.upsert_many(total_records)
    result["rows_read"] = len(total_records)
    result["rows_written"] = written
    if warnings:
        result["warnings"] = warnings
    return result


def _normalize_providers(value) -> list[str]:
    if isinstance(value, str):
        return [item.strip().lower() for item in value.split(",") if item.strip()]
    return [str(item).strip().lower() for item in value if str(item).strip()]


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
