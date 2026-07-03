from __future__ import annotations

from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any, Callable

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.jobs.contracts import JobRequest, JobResult
from cynexus_data.pipelines.fund_flow import sync_sector_flow
from cynexus_data.pipelines.kline import sync_adjusted_daily, sync_daily, sync_tdx_hsjday
from cynexus_data.pipelines.market import build_index_returns, build_market_heatmap, build_style_sector
from cynexus_data.pipelines.market_overview import build_market_overview
from cynexus_data.pipelines.msi import (
    build_limit_emotion,
    build_ma_count,
    build_msi,
    build_profit_effect,
    build_risk_appetite,
    build_sector_breadth,
    build_style_risk,
    build_turnover_activity,
    build_volatility_sentiment,
)
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.runtime.progress import mark_parent_step_progress
from cynexus_data.storage.clickhouse.readers import get_latest_table_trade_date


StepCallable = Callable[[PipelineContext, JobRequest], dict[str, Any]]
DEFAULT_HSJDAY_ROOT = PROJECT_ROOT / "var" / "tdx" / "day" / "hsjday"

CHAIN_STEPS: list[tuple[str, StepCallable]] = [
    ("tdx_hsjday_download", sync_tdx_hsjday.run),
    ("initial_backfill_refresh", sync_daily.run),
    ("qfq_reconciliation_refresh", sync_adjusted_daily.run),
    ("build_style_sector", build_style_sector.run),
    ("build_index_returns", build_index_returns.run),
    ("limit_emotion_refresh", build_limit_emotion.run),
    ("risk_appetite_refresh", build_risk_appetite.run),
    ("ma_count_refresh", build_ma_count.run),
    ("volatility_sentiment_refresh", build_volatility_sentiment.run),
    ("style_risk_appetite_refresh", build_style_risk.run),
    ("profit_effect_refresh", build_profit_effect.run),
    ("market_overview_daily", build_market_overview.run),
    ("turnover_activity_refresh", build_turnover_activity.run),
    ("ths_industry_fund_flow_daily", sync_sector_flow.run),
    ("build_market_heatmap", build_market_heatmap.run),
    ("sector_breadth_refresh", build_sector_breadth.run),
    ("market_sentiment_index_refresh", build_msi.run),
]


def run(context: PipelineContext, request: JobRequest, run_id: str) -> JobResult:
    started_at = datetime.now(timezone.utc)
    base_params = dict(request.params or {})
    target_date = _resolve_target_date(context, base_params)
    hsjday_root = _resolve_hsjday_root(base_params)
    completed_steps: list[str] = []
    step_results: list[dict[str, Any]] = []
    rows_read = 0
    rows_written = 0

    metadata: dict[str, Any] = {
        "pipeline": "scheduler.market_sentiment_chain_refresh",
        "dry_run": request.dry_run,
        "target_date": target_date,
        "hsjday_root": str(hsjday_root),
        "planned_steps": len(CHAIN_STEPS),
        "lastStep": None,
        "lastCompletedSteps": completed_steps,
        "lastStepResults": step_results,
        "lastMessage": "market_sentiment_chain starting",
        "rows_read": rows_read,
        "rows_written": rows_written,
    }
    _mark_progress(context, run_id, metadata, rows_read=rows_read, rows_written=rows_written)

    for step_code, step_runner in CHAIN_STEPS:
        step_index = len(completed_steps)
        metadata["lastStep"] = step_code
        metadata["currentStepProgress"] = {
            "step": step_code,
            "unitLabel": "阶段",
            "processedItems": 0,
            "totalItems": 1,
            "rowsRead": 0,
            "rowsWritten": 0,
        }
        metadata["lastMessage"] = f"running {step_code}"
        _mark_progress(context, run_id, metadata, rows_read=rows_read, rows_written=rows_written)
        try:
            child_params = dict(base_params)
            if target_date:
                child_params.setdefault("trade_date", target_date)
                child_params.setdefault("date", target_date)
                child_params.setdefault("snapshot_date", target_date)
            if step_code == "market_sentiment_index_refresh":
                child_params.setdefault("force", True)
                child_params.setdefault("require_full", True)
            _apply_step_path_defaults(step_code, child_params, hsjday_root)
            _apply_step_progress_defaults(step_code, child_params, run_id, metadata, rows_read, rows_written)
            mark_parent_step_progress(
                context,
                child_params,
                processed=0,
                total=1,
                rows_read=0,
                rows_written=0,
                message=f"running {step_code} ({step_index + 1}/{len(CHAIN_STEPS)})",
                unit_label="阶段",
                force=True,
            )
            child_request = JobRequest(
                job_key=step_code,
                trigger_request_id=request.trigger_request_id,
                triggered_by=request.triggered_by,
                dry_run=request.dry_run,
                params=child_params,
            )
            payload = step_runner(context, child_request)
            step_rows_read = int(payload.get("rows_read") or 0)
            step_rows_written = int(payload.get("rows_written") or 0)
            rows_read += step_rows_read
            rows_written += step_rows_written
            summary = {
                "jobId": step_code,
                "ok": True,
                "lastStatus": "success",
                "lastRunError": None,
                "lastMessage": _step_message(payload, step_rows_read, step_rows_written),
                "rowsRead": step_rows_read,
                "rowsWritten": step_rows_written,
                "pipeline": payload.get("pipeline"),
            }
        except Exception as exc:  # noqa: BLE001
            summary = {
                "jobId": step_code,
                "ok": False,
                "lastStatus": "failed",
                "lastRunError": f"{type(exc).__name__}: {exc}",
                "lastMessage": f"{step_code} failed",
                "rowsRead": 0,
                "rowsWritten": 0,
            }
            step_results.append(summary)
            metadata.update(
                {
                    "lastStep": step_code,
                    "lastCompletedSteps": completed_steps,
                    "lastStepResults": step_results,
                    "lastMessage": f"stopped_at={step_code}",
                    "currentStepProgress": {
                        "step": step_code,
                        "unitLabel": "阶段",
                        "processedItems": 0,
                        "totalItems": 1,
                        "rowsRead": 0,
                        "rowsWritten": 0,
                    },
                    "rows_read": rows_read,
                    "rows_written": rows_written,
                }
            )
            _mark_progress(context, run_id, metadata, rows_read=rows_read, rows_written=rows_written)
            return JobResult(
                run_id=run_id,
                job_key=request.job_key,
                success=False,
                status="failed",
                started_at=started_at,
                finished_at=datetime.now(timezone.utc),
                rows_read=rows_read,
                rows_written=rows_written,
                errors=[summary["lastRunError"]],
                metadata=metadata,
            )

        completed_steps.append(step_code)
        step_results.append(summary)
        metadata.update(
            {
                "lastCompletedSteps": completed_steps.copy(),
                "lastStepResults": step_results.copy(),
                "lastMessage": f"completed {len(completed_steps)}/{len(CHAIN_STEPS)} steps",
                "currentStepProgress": {
                    "step": step_code,
                    "unitLabel": "阶段",
                    "processedItems": 1,
                    "totalItems": 1,
                    "rowsRead": step_rows_read,
                    "rowsWritten": step_rows_written,
                },
                "rows_read": rows_read,
                "rows_written": rows_written,
            }
        )
        _mark_progress(context, run_id, metadata, rows_read=rows_read, rows_written=rows_written)

    metadata["lastStep"] = None
    metadata["lastMessage"] = f"completed {len(completed_steps)} market sentiment steps"
    return JobResult(
        run_id=run_id,
        job_key=request.job_key,
        success=True,
        status="success",
        started_at=started_at,
        finished_at=datetime.now(timezone.utc),
        rows_read=rows_read,
        rows_written=rows_written,
        metadata=metadata,
    )


def _mark_progress(context: PipelineContext, run_id: str, metadata: dict[str, Any], *, rows_read: int, rows_written: int) -> None:
    context.job_store.mark_progress(
        run_id,
        {
            "status": "running",
            "success": False,
            "rows_read": rows_read,
            "rows_written": rows_written,
            "metadata": metadata,
        },
    )


def _step_message(payload: dict[str, Any], rows_read: int, rows_written: int) -> str:
    pipeline = payload.get("pipeline") or "pipeline"
    if payload.get("skipped"):
        return f"{pipeline} skipped"
    return f"{pipeline} rows_read={rows_read}, rows_written={rows_written}"


def _resolve_hsjday_root(params: dict[str, Any]) -> Path:
    root = (
        params.get("hsjday_root")
        or params.get("target_root")
        or params.get("day_root")
        or DEFAULT_HSJDAY_ROOT
    )
    return Path(str(root))


def _resolve_target_date(context: PipelineContext, params: dict[str, Any]) -> str | None:
    raw = params.get("target_date") or params.get("trade_date") or params.get("snapshot_date") or params.get("date")
    if raw:
        if isinstance(raw, date):
            return raw.isoformat()
        text = str(raw)
        if len(text) == 8 and text.isdigit():
            return f"{text[:4]}-{text[4:6]}-{text[6:8]}"
        return text[:10]
    if context.hooks is None:
        return None
    try:
        latest = get_latest_table_trade_date(context.hooks.clickhouse.get_client(), "daily_raw")
    except Exception:
        return None
    return latest.isoformat() if latest else None


def _apply_step_path_defaults(step_code: str, params: dict[str, Any], hsjday_root: Path) -> None:
    if step_code == "tdx_hsjday_download":
        params.setdefault("target_root", str(hsjday_root))
        return
    if step_code == "initial_backfill_refresh":
        params.setdefault("day_root", str(hsjday_root))
        params.setdefault("tdx_root", str(hsjday_root))
        params.setdefault("flush_rows", 100_000)
        params.setdefault("replace_scope", "date")
        return
    if step_code == "qfq_reconciliation_refresh":
        params.setdefault("adjust", "qfq")
        params.setdefault("flush_rows", 100_000)
        params.setdefault("replace_scope", "date")
        params.setdefault("workers", 4)
        return
    if step_code == "build_style_sector":
        params.setdefault("quote_source", "tencent")
        params.setdefault("create_table", True)
        return
    if step_code == "build_index_returns":
        params.setdefault("source_table", "daily_raw")
        params.setdefault("create_table", True)
        return
    if step_code == "build_market_heatmap":
        params.setdefault("create_table", True)
        return
    if step_code == "market_overview_daily":
        params.setdefault("create_table", True)


def _apply_step_progress_defaults(
    step_code: str,
    params: dict[str, Any],
    run_id: str,
    metadata: dict[str, Any],
    rows_read: int,
    rows_written: int,
) -> None:
    params.setdefault("progress_run_id", run_id)
    params.setdefault("progress_step_code", step_code)
    params.setdefault("progress_base_metadata", metadata.copy())
    params.setdefault("progress_parent_rows_read", rows_read)
    params.setdefault("progress_parent_rows_written", rows_written)
    if step_code == "initial_backfill_refresh":
        params.setdefault("progress_update_every_items", 100)
        params.setdefault("progress_unit_label", "文件")
    elif step_code == "qfq_reconciliation_refresh":
        params.setdefault("progress_update_every_items", 50)
        params.setdefault("progress_unit_label", "标的")
    elif step_code == "ths_industry_fund_flow_daily":
        params.setdefault("progress_update_every_items", 1)
        params.setdefault("progress_unit_label", "快照")
    else:
        params.setdefault("progress_update_every_items", 1)
        params.setdefault("progress_unit_label", "阶段")
