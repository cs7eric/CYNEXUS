from __future__ import annotations

from pathlib import Path

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.domain.stock.kline_records import minute_provider_rows_to_frame
from cynexus_data.sources.provider_fallback import fetch_minute_kline_with_fallback, provider_plan
from cynexus_data.sources.tdx.local_minute import discover_1m_files, iter_tdx_1m_frames
from cynexus_data.storage.clickhouse.writers import ClickHouseKlineSink


def run(context: PipelineContext, request: JobRequest) -> dict:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    root = Path(str(request.params.get("tdx_root") or context.hooks.tdx.get_root_path()))
    codes = _normalize_codes(request.params.get("codes") or request.params.get("symbols"))
    providers = provider_plan("kline_minute", requested=request.params.get("providers"))
    limit_files = request.params.get("limit_files")
    batch_size = int(request.params.get("batch_size") or 50_000)
    create_table = _truthy(request.params.get("create_table"))
    replace_existing = _truthy(request.params.get("replace_existing"))
    period = int(request.params.get("period") or 1)
    files = discover_1m_files(root) if "tdx" in providers and str(root) else []
    if limit_files is not None:
        files = files[: int(limit_files)]
    result = {
        "pipeline": "kline.sync_minute",
        "dry_run": request.dry_run,
        "providers": providers,
        "tdx_root": str(root),
        "planned_codes": len(codes or []),
        "discovered_files": len(files),
        "rows_read": 0,
        "rows_written": 0,
        "file_results": [],
        "provider_results": [],
        "replace_existing": replace_existing,
    }
    if request.dry_run:
        if codes and any(provider != "tdx" for provider in providers):
            result["network_provider_plan"] = [provider for provider in providers if provider != "tdx"]
        return result

    sink = ClickHouseKlineSink(context.hooks.clickhouse.get_config())
    if create_table:
        sink.create_table()
    total_rows = 0
    total_written = 0
    for file, frame in iter_tdx_1m_frames(files):
        rows = len(frame)
        written = sink.insert_1m(frame, batch_size=batch_size, replace_existing=replace_existing)
        total_rows += rows
        total_written += written
        result["file_results"].append(
            {
                "path": str(file.path),
                "market": file.market,
                "symbol": file.symbol,
                "rows_read": rows,
                "rows_written": written,
            }
        )
    if codes and any(provider != "tdx" for provider in providers) and ("tdx" not in providers or not files):
        for code in codes:
            fetched = fetch_minute_kline_with_fallback(
                context.hooks,
                symbol=code,
                providers=providers,
                period=period,
                limit=int(request.params.get("limit") or 500),
            )
            frame = minute_provider_rows_to_frame(code, fetched.items, source=fetched.source, period=period)
            written = sink.insert_1m(frame, batch_size=batch_size, replace_existing=replace_existing)
            total_rows += len(frame)
            total_written += written
            result["provider_results"].append(
                {
                    "symbol": code,
                    "rows_read": len(fetched.items),
                    "rows_written": written,
                    **fetched.meta(),
                }
            )
    result["rows_read"] = total_rows
    result["rows_written"] = total_written
    return {
        **result,
        "file_results": result["file_results"][:20],
        "file_results_truncated": len(result["file_results"]) > 20,
        "provider_results": result["provider_results"][:20],
        "provider_results_truncated": len(result["provider_results"]) > 20,
    }


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
