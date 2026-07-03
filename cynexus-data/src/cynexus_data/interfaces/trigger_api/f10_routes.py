from __future__ import annotations

from fastapi import APIRouter, Depends

from cynexus_data.interfaces.trigger_api.auth import require_internal_token
from cynexus_data.sources.tdx.f10 import (
    create_eltdx_f10_client,
    fetch_stock_announcements,
    fetch_stock_business_composition,
    fetch_stock_company_news,
    fetch_stock_finance_report,
    fetch_stock_news,
    fetch_stock_profit_forecast,
    fetch_stock_roadshows,
    fetch_stock_valuation,
)


router = APIRouter(dependencies=[Depends(require_internal_token)])


@router.get("/internal/f10/valuation")
def valuation(symbol: str, req_id: str = "200191", timeout_seconds: float = 8.0, pool_size: int = 4) -> dict:
    with create_eltdx_f10_client(pool_size=pool_size, timeout=timeout_seconds) as client:
        return fetch_stock_valuation(client, symbol, req_id=req_id)


@router.get("/internal/f10/finance-report")
def finance_report(symbol: str, report_type: str = "zcfzb", timeout_seconds: float = 8.0, pool_size: int = 4) -> dict:
    with create_eltdx_f10_client(pool_size=pool_size, timeout=timeout_seconds) as client:
        return fetch_stock_finance_report(client, symbol, report_type=report_type)


@router.get("/internal/f10/business-composition")
def business_composition(
    symbol: str,
    report_date: str | None = None,
    timeout_seconds: float = 8.0,
    pool_size: int = 4,
) -> dict:
    with create_eltdx_f10_client(pool_size=pool_size, timeout=timeout_seconds) as client:
        return fetch_stock_business_composition(client, symbol, report_date=report_date)


@router.get("/internal/f10/profit-forecast")
def profit_forecast(symbol: str, timeout_seconds: float = 8.0, pool_size: int = 4) -> dict:
    with create_eltdx_f10_client(pool_size=pool_size, timeout=timeout_seconds) as client:
        return fetch_stock_profit_forecast(client, symbol)


@router.get("/internal/f10/announcements")
def announcements(symbol: str, timeout_seconds: float = 8.0, pool_size: int = 4) -> dict:
    with create_eltdx_f10_client(pool_size=pool_size, timeout=timeout_seconds) as client:
        return fetch_stock_announcements(client, symbol)


@router.get("/internal/f10/news")
def news(symbol: str, timeout_seconds: float = 8.0, pool_size: int = 4) -> dict:
    with create_eltdx_f10_client(pool_size=pool_size, timeout=timeout_seconds) as client:
        return fetch_stock_news(client, symbol)


@router.get("/internal/f10/roadshows")
def roadshows(symbol: str, timeout_seconds: float = 8.0, pool_size: int = 4) -> dict:
    with create_eltdx_f10_client(pool_size=pool_size, timeout=timeout_seconds) as client:
        return fetch_stock_roadshows(client, symbol)


@router.get("/internal/f10/company-news")
def company_news(symbol: str, section: str = "gsyj", timeout_seconds: float = 8.0, pool_size: int = 4) -> dict:
    with create_eltdx_f10_client(pool_size=pool_size, timeout=timeout_seconds) as client:
        return fetch_stock_company_news(client, symbol, section=section)
