from __future__ import annotations

from datetime import date

from fastapi import APIRouter, Depends

from cynexus_data.domain.market.index_codes import TDX_INDUSTRY_56
from cynexus_data.domain.market.tdx_industry_fund_flow import build_tdx_industry_fund_flow_response
from cynexus_data.interfaces.trigger_api.auth import require_internal_token
from cynexus_data.interfaces.trigger_api.routes import _context
from cynexus_data.storage.postgres.tdx_industry_fund_flow import TdxIndustryFundFlowRepository


router = APIRouter(dependencies=[Depends(require_internal_token)])


@router.get("/internal/industry-application/tdx-fund-flow")
def tdx_fund_flow(
    symbol: str,
    target_type: str = "industry",
    trade_date: str | None = None,
    days: int = 30,
) -> dict:
    normalized_type = str(target_type or "").strip().lower()
    requested_trade_date = _parse_date(trade_date)
    normalized_symbol = str(symbol or "").strip().lower()
    code6 = _normalize_code6(normalized_symbol)
    applicable = normalized_type == "industry" and code6 in TDX_INDUSTRY_56
    if not applicable:
        return build_tdx_industry_fund_flow_response(
            target_type=normalized_type or target_type,
            symbol=normalized_symbol,
            requested_trade_date=requested_trade_date,
            days=days,
            latest_row=None,
            history_rows=[],
            applicable=False,
            error="only TDX 56 industry targets are supported",
        )
    if _context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    repo = TdxIndustryFundFlowRepository(_context.hooks.postgres.get_client())
    latest = repo.get_latest_snapshot(code6=code6, max_trade_date=requested_trade_date)
    if latest is None:
        return build_tdx_industry_fund_flow_response(
            target_type=normalized_type,
            symbol=normalized_symbol,
            requested_trade_date=requested_trade_date,
            days=days,
            latest_row=None,
            history_rows=[],
            applicable=True,
            error="no TDX industry fund-flow snapshot found",
        )
    history = repo.list_history(
        code6=code6,
        max_trade_date=latest.get("trade_date"),
        limit_days=max(1, min(days, 252)),
    )
    return build_tdx_industry_fund_flow_response(
        target_type=normalized_type,
        symbol=normalized_symbol,
        requested_trade_date=requested_trade_date,
        days=days,
        latest_row=latest,
        history_rows=history,
        applicable=True,
        error=None,
    )


def _normalize_code6(value: str) -> str:
    text = value.strip().lower()
    for prefix in ("sh", "sz", "bj"):
        if text.startswith(prefix):
            text = text[len(prefix) :]
            break
    return text


def _parse_date(value: str | None) -> date | None:
    if value is None or not value.strip():
        return None
    return date.fromisoformat(value.strip())
