from __future__ import annotations

from datetime import date, datetime, time, timedelta, timezone
from typing import Any

from cynexus_data.domain.market.transmission import evaluate_episode_outcome, to_jsonable
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.market_indicator import MarketIndicatorRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    repo = MarketIndicatorRepository(context.hooks.postgres.get_client())
    trade_date = _trade_date(request.params.get("trade_date"))
    if request.dry_run:
        return {
            "pipeline": "market.market_indicator_outcome_finalize",
            "dry_run": True,
            "success": True,
            "trade_date": trade_date.isoformat(),
            "notes": ["dry_run skipped PostgreSQL reads/writes"],
        }
    episodes = repo.list_transmission_episode_payloads(trade_date, limit=int(request.params.get("limit") or 500))
    if not episodes:
        return {
            "pipeline": "market.market_indicator_outcome_finalize",
            "success": True,
            "trade_date": trade_date.isoformat(),
            "episodes": 0,
            "outcomes_written": 0,
            "warnings": ["没有可评价的 Episode"],
        }
    start_at = datetime.combine(trade_date, time.min, tzinfo=timezone.utc)
    end_at = datetime.combine(trade_date, time.max, tzinfo=timezone.utc)
    captures = repo.list_capture_rows(trade_date, start_at=start_at, end_at=end_at)
    if not captures:
        return {
            "pipeline": "market.market_indicator_outcome_finalize",
            "success": True,
            "trade_date": trade_date.isoformat(),
            "episodes": len(episodes),
            "outcomes_written": 0,
            "warnings": ["没有可评价的历史报价捕获"],
        }
    evaluated_at = datetime.now(timezone.utc)
    outcomes = [to_jsonable(evaluate_episode_outcome(episode, captures, evaluated_at=evaluated_at)) for episode in episodes]
    written = repo.save_outcomes(outcomes)
    return {
        "pipeline": "market.market_indicator_outcome_finalize",
        "success": True,
        "trade_date": trade_date.isoformat(),
        "episodes": len(episodes),
        "captures": len(captures),
        "outcomes_written": written,
        "evaluated_at": evaluated_at.isoformat(),
    }


def _trade_date(value: object) -> date:
    if value in (None, ""):
        return datetime.now(timezone.utc).date()
    text = str(value).replace("/", "-")
    return date.fromisoformat(text[:10])
