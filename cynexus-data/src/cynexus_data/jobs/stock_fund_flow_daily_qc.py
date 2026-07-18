from __future__ import annotations

from datetime import date, datetime, timezone
import random
import time
from typing import Any

from cynexus_data.jobs.contracts import JobRequest, JobResult
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.eastmoney import a_stock as eastmoney_a_stock
from cynexus_data.sources.eastmoney.http import EastMoneyHttpClient
from cynexus_data.sources.tdx.easy_tdx import fetch_history_stock_fund_flow_payload
from cynexus_data.storage.postgres.fund_flow_qc import FundFlowQualityCheckRepository
from cynexus_data.storage.postgres.market_limit import MarketLimitRepository
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository


JOB_KEY = "a_stock_fund_flow_daily_qc"
DEFAULT_SAMPLE_SIZE = 50


def run(context: PipelineContext, request: JobRequest, run_id: str) -> JobResult:
    started_at = datetime.now(timezone.utc)
    params = dict(request.params or {})
    sample_date = _date(params.get("sample_date") or params.get("trade_date")) or date.today()
    sample_size = min(_int(params.get("sample_size"), DEFAULT_SAMPLE_SIZE), DEFAULT_SAMPLE_SIZE)
    sleep_seconds = max(0.0, float(params.get("request_sleep_seconds") or 0.0))
    timeout = float(params.get("provider_timeout") or 15.0)

    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    pg = context.hooks.postgres.get_client()
    repo = FundFlowQualityCheckRepository(pg)
    if bool(params.get("create_table", True)):
        repo.create_table()

    codes = _sample_codes(context, params, sample_date=sample_date, sample_size=sample_size)
    client = EastMoneyHttpClient(timeout=timeout)
    rows: list[dict[str, Any]] = []
    errors: list[str] = []

    for index, code in enumerate(codes, start=1):
        try:
            row = _check_code(code, run_id=run_id, sample_date=sample_date, timeout=timeout, eastmoney_client=client)
        except Exception as exc:  # noqa: BLE001 - QC should continue to the next sampled code.
            message = f"{code}: {type(exc).__name__}: {exc}"
            errors.append(message)
            row = {
                "run_id": run_id,
                "sample_date": sample_date,
                "code": code,
                "error": message,
                "easy_tdx_payload": None,
                "eastmoney_payload": None,
            }
        rows.append(row)
        if not request.dry_run:
            repo.upsert_many([row])
        context.job_store.mark_progress(
            run_id,
            {
                "status": "running",
                "success": False,
                "metadata": {
                    "pipeline": "stock_fund_flow.daily_qc",
                    "sample_size": len(codes),
                    "processed": index,
                    "success_count": len([item for item in rows if not item.get("error")]),
                    "failed_count": len([item for item in rows if item.get("error")]),
                    "lastMessage": f"QC {index}/{len(codes)}: {code}",
                },
            },
        )
        if sleep_seconds > 0 and index < len(codes):
            time.sleep(sleep_seconds)

    summary = _summary(rows)
    metadata = {
        "pipeline": "stock_fund_flow.daily_qc",
        "sample_date": sample_date.isoformat(),
        "stock_scope": str(params.get("stock_scope") or params.get("scope") or "all-active"),
        "sample_size": len(codes),
        "processed": len(rows),
        "success_count": summary["successCount"],
        "failed_count": summary["failedCount"],
        "direction_match_count": summary["directionMatchCount"],
        "direction_match_rate": summary["directionMatchRate"],
        "qcSummary": summary,
        "lastMessage": (
            f"QC completed: success={summary['successCount']}/{len(codes)}, "
            f"direction_match={summary['directionMatchRate'] if summary['directionMatchRate'] is not None else 'n/a'}"
        ),
    }
    status = "success" if not errors else "partial"
    return JobResult(
        run_id=run_id,
        job_key=request.job_key,
        success=not errors,
        status=status,
        started_at=started_at,
        finished_at=datetime.now(timezone.utc),
        rows_read=len(rows),
        rows_written=0 if request.dry_run else len(rows),
        errors=errors[:20],
        metadata=metadata,
    )


def _check_code(
    code: str,
    *,
    run_id: str,
    sample_date: date,
    timeout: float,
    eastmoney_client: EastMoneyHttpClient,
) -> dict[str, Any]:
    easy_payload = fetch_history_stock_fund_flow_payload(code, count=1, timeout=timeout)
    eastmoney_payload = eastmoney_a_stock.fund_flow_120d(eastmoney_client, code, mode="daily")
    easy_row = _latest_row(easy_payload.get("items") or [])
    eastmoney_row = _latest_row(eastmoney_payload.get("items") or [])
    if not easy_row:
        raise RuntimeError("easy_tdx returned no daily fund-flow row")
    if not eastmoney_row:
        raise RuntimeError("EastMoney returned no daily fund-flow row")

    easy_main = _float(easy_row.get("main_net"))
    eastmoney_main = _float(eastmoney_row.get("main_net"))
    diff = easy_main - eastmoney_main
    ratio = abs(diff) / max(abs(eastmoney_main), 1.0)
    trade_date = _date(easy_row.get("trade_date") or easy_row.get("date")) or _date(eastmoney_row.get("trade_date") or eastmoney_row.get("date"))
    return {
        "run_id": run_id,
        "sample_date": sample_date,
        "code": code,
        "trade_date": trade_date,
        "easy_tdx_source": easy_payload.get("source"),
        "eastmoney_source": eastmoney_payload.get("source"),
        "easy_tdx_main_net": easy_main,
        "eastmoney_main_net": eastmoney_main,
        "main_net_diff": diff,
        "main_net_diff_ratio": ratio,
        "direction_match": _direction(easy_main) == _direction(eastmoney_main),
        "easy_tdx_payload": easy_row,
        "eastmoney_payload": eastmoney_row,
        "error": None,
    }


def _sample_codes(context: PipelineContext, params: dict[str, Any], *, sample_date: date, sample_size: int) -> list[str]:
    explicit = params.get("codes") or params.get("symbols") or params.get("code")
    if explicit:
        values = explicit if isinstance(explicit, list) else str(explicit).split(",")
        codes = [str(value).strip().zfill(6) for value in values if str(value).strip()]
    else:
        pg = context.hooks.postgres.get_client()  # type: ignore[union-attr]
        active = _dedupe_codes(StockBasicRepository(pg).list_codes(active_only=True))
        scope = str(params.get("stock_scope") or params.get("scope") or "all-active")
        if scope in {"limit-up-history", "historical-limit-up", "limit_up_history"}:
            codes = _dedupe_codes(MarketLimitRepository(pg).list_historical_limit_up_codes())
        elif scope in {"remaining-active", "non-limit-up-active", "active-minus-limit-up"}:
            limit_codes = set(_dedupe_codes(MarketLimitRepository(pg).list_historical_limit_up_codes()))
            codes = [code for code in active if code not in limit_codes]
        else:
            codes = active
    rng = random.Random(f"{sample_date.isoformat()}:{params.get('sample_seed') or datetime.now(timezone.utc).isoformat()}")
    shuffled = list(dict.fromkeys(codes))
    rng.shuffle(shuffled)
    return shuffled[:sample_size]


def _dedupe_codes(values: list[str]) -> list[str]:
    seen: set[str] = set()
    output: list[str] = []
    for value in values:
        code = str(value).strip()[-6:].zfill(6)
        if not code or code in seen:
            continue
        seen.add(code)
        output.append(code)
    return output


def _summary(rows: list[dict[str, Any]]) -> dict[str, Any]:
    success = [row for row in rows if not row.get("error") and row.get("direction_match") is not None]
    matched = [row for row in success if row.get("direction_match") is True]
    ratios = [float(row["main_net_diff_ratio"]) for row in success if row.get("main_net_diff_ratio") is not None]
    return {
        "sampleSize": len(rows),
        "successCount": len(success),
        "failedCount": len(rows) - len(success),
        "directionMatchCount": len(matched),
        "directionMatchRate": round(len(matched) / len(success), 4) if success else None,
        "avgAbsDiffRatio": round(sum(ratios) / len(ratios), 4) if ratios else None,
        "samples": [
            {
                "code": row.get("code"),
                "tradeDate": (_date(row.get("trade_date")).isoformat() if _date(row.get("trade_date")) else None),
                "easyTdxMainNet": row.get("easy_tdx_main_net"),
                "eastmoneyMainNet": row.get("eastmoney_main_net"),
                "mainNetDiff": row.get("main_net_diff"),
                "mainNetDiffRatio": row.get("main_net_diff_ratio"),
                "directionMatch": row.get("direction_match"),
                "error": row.get("error"),
            }
            for row in rows[:8]
        ],
    }


def _latest_row(rows: list[dict[str, Any]]) -> dict[str, Any] | None:
    return rows[-1] if rows else None


def _direction(value: float) -> int:
    if value > 0:
        return 1
    if value < 0:
        return -1
    return 0


def _float(value: Any) -> float:
    if value in (None, "", "--", "-"):
        return 0.0
    return float(value)


def _date(value: Any) -> date | None:
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    if value in (None, ""):
        return None
    return datetime.fromisoformat(str(value)[:10]).date()


def _int(value: Any, default: int) -> int:
    try:
        return max(1, int(value))
    except (TypeError, ValueError):
        return default


def main() -> None:
    from cynexus_data.jobs._pipeline_job import main_for_job

    main_for_job(JOB_KEY, "Run sequential same-day EastMoney vs easy_tdx stock fund-flow QC.")


if __name__ == "__main__":
    main()
