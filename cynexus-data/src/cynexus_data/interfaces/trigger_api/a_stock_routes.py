from __future__ import annotations

from fastapi import APIRouter, Depends

from cynexus_data.interfaces.trigger_api.auth import require_internal_token
from cynexus_data.sources.baidu.client import BaiduStockClient
from cynexus_data.sources.cninfo.client import CninfoClient
from cynexus_data.sources.eastmoney import a_stock as em
from cynexus_data.sources.eastmoney.http import EastMoneyHttpClient
from cynexus_data.sources.iwencai.client import IwencaiClient, dedup_articles
from cynexus_data.sources.sina.a_stock import SinaAStockClient
from cynexus_data.sources.tdx.mootdx_market import fetch_order_book, fetch_quotes, fetch_transactions
from cynexus_data.sources.ths.a_stock import ThsAStockClient


router = APIRouter(prefix="/internal/a-stock", dependencies=[Depends(require_internal_token)])

_em_client = EastMoneyHttpClient()


@router.get("/eastmoney/reports")
def eastmoney_reports(code: str, max_pages: int = 1) -> dict:
    rows = em.reports(_em_client, code, max_pages=max_pages)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/industry-reports")
def eastmoney_industry_reports(industry_code: str = "*", max_pages: int = 1, begin: str = "2024-01-01") -> dict:
    rows = em.industry_reports(_em_client, industry_code=industry_code, max_pages=max_pages, begin=begin)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/stock-info")
def eastmoney_stock_info(code: str) -> dict:
    return em.stock_info(_em_client, code)


@router.get("/eastmoney/concept-blocks")
def eastmoney_concept_blocks(code: str) -> dict:
    return em.concept_blocks(_em_client, code)


@router.get("/eastmoney/fund-flow-minute")
def eastmoney_fund_flow_minute(code: str) -> dict:
    rows = em.fund_flow_minute(_em_client, code)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/fund-flow-120d")
def eastmoney_fund_flow_120d(code: str) -> dict:
    rows = em.fund_flow_120d(_em_client, code)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/industry-rank")
def eastmoney_industry_rank(top_n: int = 20) -> dict:
    return em.industry_rank(_em_client, top_n=top_n)


@router.get("/eastmoney/dragon-tiger")
def eastmoney_dragon_tiger(code: str, trade_date: str, look_back: int = 30) -> dict:
    return em.dragon_tiger_board(_em_client, code, trade_date, look_back=look_back)


@router.get("/eastmoney/daily-dragon-tiger")
def eastmoney_daily_dragon_tiger(trade_date: str | None = None, min_net_buy: float | None = None) -> dict:
    return em.daily_dragon_tiger(_em_client, trade_date=trade_date, min_net_buy=min_net_buy)


@router.get("/eastmoney/lockup")
def eastmoney_lockup(code: str, trade_date: str, forward_days: int = 90) -> dict:
    return em.lockup_expiry(_em_client, code, trade_date, forward_days=forward_days)


@router.get("/eastmoney/margin")
def eastmoney_margin(code: str, page_size: int = 30) -> dict:
    rows = em.margin_trading(_em_client, code, page_size=page_size)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/block-trade")
def eastmoney_block_trade(code: str, page_size: int = 20) -> dict:
    rows = em.block_trade(_em_client, code, page_size=page_size)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/holders")
def eastmoney_holders(code: str, page_size: int = 10) -> dict:
    rows = em.holder_num_change(_em_client, code, page_size=page_size)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/dividends")
def eastmoney_dividends(code: str, page_size: int = 20) -> dict:
    rows = em.dividend_history(_em_client, code, page_size=page_size)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/stock-news")
def eastmoney_stock_news(code: str, page_size: int = 20) -> dict:
    rows = em.stock_news(_em_client, code, page_size=page_size)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/global-news")
def eastmoney_global_news(page_size: int = 50) -> dict:
    rows = em.global_news(_em_client, page_size=page_size)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/limit-pool")
def eastmoney_limit_pool(pool: str = "zt", trade_date: str = "") -> dict:
    rows = em.limit_pool(_em_client, pool, trade_date)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/limit-sentiment")
def eastmoney_limit_sentiment(trade_date: str) -> dict:
    return em.limit_sentiment(_em_client, trade_date)


@router.get("/eastmoney/hot-rank")
def eastmoney_hot_rank(top: int = 50) -> dict:
    rows = em.hot_rank(_em_client, top=top)
    return {"items": rows, "count": len(rows)}


@router.get("/eastmoney/hot-concept")
def eastmoney_hot_concept(code: str) -> dict:
    rows = em.hot_concept(_em_client, code)
    return {"items": rows, "count": len(rows)}


@router.get("/baidu/kline-ma")
def baidu_kline_ma(code: str, start_time: str = "") -> dict:
    return BaiduStockClient().kline_with_ma(code, start_time=start_time)


@router.get("/ths/eps-forecast")
def ths_eps_forecast(code: str) -> dict:
    rows = ThsAStockClient().eps_forecast(code)
    return {"items": rows, "count": len(rows)}


@router.get("/ths/hot-reason")
def ths_hot_reason(trade_date: str | None = None) -> dict:
    rows = ThsAStockClient().hot_reason(trade_date)
    return {"items": rows, "count": len(rows)}


@router.get("/ths/northbound")
def ths_northbound() -> dict:
    rows = ThsAStockClient().northbound_realtime()
    return {"items": rows, "count": len(rows)}


@router.get("/ths/limit-up-pool")
def ths_limit_up_pool(trade_date: str) -> dict:
    rows = ThsAStockClient().limit_up_pool(trade_date)
    return {"items": rows, "count": len(rows)}


@router.get("/ths/hot-list")
def ths_hot_list(period: str = "hour") -> dict:
    rows = ThsAStockClient().hot_list(period)
    return {"items": rows, "count": len(rows)}


@router.get("/cninfo/announcements")
def cninfo_announcements(code: str, page_size: int = 30) -> dict:
    rows = CninfoClient().announcements(code, page_size=page_size)
    return {"items": rows, "count": len(rows)}


@router.get("/cninfo/irm")
def cninfo_irm(code: str, page_size: int = 30, page_num: int = 1) -> dict:
    rows = CninfoClient().irm(code, page_size=page_size, page_num=page_num)
    return {"items": rows, "count": len(rows)}


@router.get("/sina/financial-report")
def sina_financial_report(code: str, report_type: str = "lrb", num: int = 8) -> dict:
    rows = SinaAStockClient().financial_report(code, report_type=report_type, num=num)
    return {"items": rows, "count": len(rows)}


@router.get("/sina/options/codes")
def sina_option_codes(underlying: str = "510050", call: bool = True) -> dict:
    return SinaAStockClient().option_codes(underlying=underlying, call=call)


@router.get("/sina/options/tquote")
def sina_option_tquote(code: str) -> dict:
    return SinaAStockClient().option_tquote(code)


@router.get("/sina/options/greeks")
def sina_option_greeks(code: str) -> dict:
    return SinaAStockClient().option_greeks(code)


@router.get("/iwencai/search")
def iwencai_search(query: str, channel: str = "report", size: int = 50, dedup: bool = True) -> dict:
    rows = IwencaiClient().search(query, channel=channel, size=size)
    if dedup:
        rows = dedup_articles(rows)
    return {"items": rows, "count": len(rows)}


@router.get("/iwencai/query")
def iwencai_query(query: str, page: int = 1, limit: int = 50) -> dict:
    rows = IwencaiClient().query2data(query, page=page, limit=limit)
    return {"items": rows, "count": len(rows)}


@router.get("/tdx/quotes")
def tdx_quotes(symbols: str) -> dict:
    rows = fetch_quotes([item.strip() for item in symbols.split(",") if item.strip()])
    return {"items": rows, "count": len(rows)}


@router.get("/tdx/order-book")
def tdx_order_book(symbol: str) -> dict:
    return fetch_order_book(symbol)


@router.get("/tdx/transactions")
def tdx_transactions(symbol: str, trade_date: str | None = None) -> dict:
    rows = fetch_transactions(symbol, trade_date=trade_date)
    return {"items": rows, "count": len(rows)}
