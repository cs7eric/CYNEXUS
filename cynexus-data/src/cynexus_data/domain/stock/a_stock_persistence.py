from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime, time
import hashlib
import json
from typing import Any


@dataclass(frozen=True)
class AStockBatch:
    postgres: dict[str, list[dict[str, Any]]]
    clickhouse: dict[str, list[dict[str, Any]]]

    @property
    def rows_read(self) -> int:
        return sum(len(rows) for rows in self.postgres.values()) + sum(len(rows) for rows in self.clickhouse.values())


def normalize_dataset(dataset: str, payload: Any, params: dict[str, Any]) -> AStockBatch:
    dataset = str(dataset)
    pg: dict[str, list[dict[str, Any]]] = {}
    ch: dict[str, list[dict[str, Any]]] = {}
    today = _date(params.get("snapshot_date") or params.get("trade_date")) or date.today()
    code = _plain_code(params.get("code") or params.get("symbol") or "")

    def put_pg(table: str, rows: list[dict[str, Any]]) -> None:
        if rows:
            pg.setdefault(table, []).extend(rows)

    def put_ch(table: str, rows: list[dict[str, Any]]) -> None:
        if rows:
            ch.setdefault(table, []).extend(rows)

    if dataset == "eastmoney_stock_info":
        row = dict(payload or {})
        put_pg("sec_stock_basic", [_stock_basic_row(row)])
    elif dataset == "eastmoney_reports":
        put_pg("a_stock_research_report", [_research_report_row("eastmoney.reportapi", "stock", item, code=code) for item in _items(payload)])
    elif dataset == "eastmoney_industry_reports":
        industry_code = str(params.get("industry_code") or "*")
        put_pg(
            "a_stock_research_report",
            [_research_report_row("eastmoney.reportapi", "industry", item, industry_code=industry_code) for item in _items(payload)],
        )
    elif dataset in {"iwencai_search", "iwencai_query"}:
        scope = dataset.replace("_", "-")
        put_pg("a_stock_research_report", [_semantic_report_row(f"iwencai.{scope}", scope, item, params) for item in _items(payload)])
    elif dataset == "ths_eps_forecast":
        put_pg("a_stock_research_report", [_ths_eps_row(item, code=code, index=index) for index, item in enumerate(_items(payload), start=1)])
    elif dataset == "sina_financial_report":
        report_type = str(params.get("report_type") or "lrb")
        put_pg("a_stock_financial_statement", [_financial_row(item, code=code, report_type=report_type) for item in _items(payload)])
    elif dataset == "cninfo_announcements":
        put_pg("a_stock_announcement", [_announcement_row("cninfo", item, code=code) for item in _items(payload)])
    elif dataset == "eastmoney_stock_news":
        put_pg("a_stock_news", [_news_row("eastmoney.search", item, code=code) for item in _items(payload)])
    elif dataset == "eastmoney_global_news":
        put_pg("a_stock_news", [_news_row("eastmoney.7x24", item, code="") for item in _items(payload)])
    elif dataset == "cninfo_irm":
        put_pg("a_stock_investor_interaction", [_interaction_row("cninfo.irm", item, code=code) for item in _items(payload)])
    elif dataset == "eastmoney_fund_flow_120d":
        put_pg("a_stock_fund_flow_daily", [_fund_flow_daily_row("eastmoney.push2his", item, code=code) for item in _items(payload)])
    elif dataset == "eastmoney_fund_flow_minute":
        put_ch("ch_ods_stock_fund_flow_minute", [_fund_flow_minute_row("eastmoney.push2", item, code=code, snapshot_date=today) for item in _items(payload)])
    elif dataset == "eastmoney_industry_rank":
        rows = []
        for direction in ("top", "bottom"):
            for index, item in enumerate((payload or {}).get(direction) or [], start=1):
                rows.append(_industry_rank_row(item, snapshot_date=today, direction=direction, index=index))
        put_pg("a_stock_industry_rank_snapshot", rows)
    elif dataset == "eastmoney_daily_dragon_tiger":
        trade_date = _date((payload or {}).get("date") or params.get("trade_date")) or today
        put_pg("a_stock_dragon_tiger_daily", [_dragon_tiger_daily_row(item, trade_date=trade_date) for item in (payload or {}).get("stocks") or []])
    elif dataset == "eastmoney_dragon_tiger":
        trade_date = _date(params.get("trade_date")) or today
        put_pg("a_stock_dragon_tiger_daily", [_dragon_tiger_record_row(item, code=code) for item in (payload or {}).get("records") or []])
        put_pg("a_stock_dragon_tiger_seat", _dragon_tiger_seat_rows(payload or {}, code=code, trade_date=trade_date))
    elif dataset == "eastmoney_lockup":
        items = (payload or {}).get("history") or []
        items += (payload or {}).get("upcoming") or []
        put_pg("a_stock_lockup_expiry", [_lockup_row(item, code=code) for item in items])
    elif dataset == "eastmoney_block_trade":
        put_pg("a_stock_block_trade", [_block_trade_row(item, code=code) for item in _items(payload)])
    elif dataset == "eastmoney_holders":
        put_pg("a_stock_holder_num", [_holder_row(item, code=code) for item in _items(payload)])
    elif dataset == "eastmoney_dividends":
        put_pg("a_stock_dividend", [_dividend_row(item, code=code) for item in _items(payload)])
    elif dataset in {"eastmoney_hot_rank", "ths_hot_list"}:
        source = "eastmoney.app.hot_rank" if dataset == "eastmoney_hot_rank" else "ths.hot_list"
        list_type = str(params.get("period") or params.get("list_type") or "current")
        put_pg("a_stock_hot_rank_snapshot", [_hot_rank_row(source, item, snapshot_date=today, list_type=list_type) for item in _items(payload)])
    elif dataset == "eastmoney_hot_concept":
        put_pg("a_stock_hot_concept_snapshot", [_hot_concept_row(item, code=code, snapshot_date=today) for item in _items(payload)])
    elif dataset == "eastmoney_concept_blocks":
        put_pg("a_stock_concept_membership", [_concept_membership_row(item, code=code, snapshot_date=today) for item in (payload or {}).get("boards") or []])
    elif dataset == "ths_hot_reason":
        put_pg("a_stock_hot_reason_snapshot", [_hot_reason_row(item, snapshot_date=today) for item in _items(payload)])
    elif dataset in {"eastmoney_limit_pool", "ths_limit_up_pool"}:
        source = "eastmoney.push2ex.limit_pool" if dataset == "eastmoney_limit_pool" else "ths.limit_up_pool"
        pool = str(params.get("pool") or "zt")
        put_pg("a_stock_limit_pool_snapshot", [_limit_pool_row(source, item, snapshot_date=today, pool=pool) for item in _items(payload)])
    elif dataset == "sina_option_codes":
        underlying = str(params.get("underlying") or "510050")
        option_type = "call" if _bool(params.get("call"), True) else "put"
        rows = []
        for option_month, codes in (payload or {}).items():
            rows.extend(_option_contract_row(item, snapshot_date=today, underlying=underlying, option_month=str(option_month), option_type=option_type) for item in codes or [])
        put_pg("a_stock_option_contract_snapshot", rows)
    elif dataset == "tdx_transactions":
        trade_date = _date(params.get("trade_date")) or today
        put_ch("ch_ods_stock_transaction", [_transaction_row(item, code=code, trade_date=trade_date) for item in _items(payload)])
    elif dataset == "tdx_order_book":
        put_ch("ch_ods_stock_order_book_snapshot", [_order_book_row(payload or {}, code=code)])
    elif dataset == "baidu_kline_ma":
        put_ch("ch_ods_baidu_kline_ma", _baidu_kline_rows(payload or {}, code=code))
    elif dataset == "ths_northbound":
        put_ch("ch_ods_northbound_minute", [_northbound_row(item, snapshot_date=today) for item in _items(payload)])
    elif dataset in {"sina_option_tquote", "sina_option_greeks"}:
        option_code = str(params.get("option_code") or params.get("code") or "")
        put_ch("ch_ods_option_quote_snapshot", [_option_quote_row(dataset, payload or {}, option_code=option_code)])
    else:
        raise ValueError(f"unsupported A-stock persistence dataset: {dataset}")

    return AStockBatch(postgres=pg, clickhouse=ch)


def _stock_basic_row(item: dict[str, Any]) -> dict[str, Any]:
    code = _plain_code(item.get("code"))
    return {
        "code": code,
        "name": item.get("name") or "",
        "market": _market(code),
        "board": "unknown",
        "is_st": False,
        "limit_up_threshold": 0.10,
        "is_active": True,
        "source": item.get("source") or "eastmoney.push2.stock.get",
        "industry": item.get("industry"),
        "total_market_cap": _num(item.get("mcap")),
        "float_market_cap": _num(item.get("float_mcap")),
        "total_shares": _num(item.get("total_shares")),
        "float_shares": _num(item.get("float_shares")),
        "list_date": _date(item.get("list_date")),
        "eastmoney_price": _num(item.get("price")),
        "eastmoney_float_market_cap": _num(item.get("float_mcap")),
        "eastmoney_updated_at": datetime.now(),
        "updated_at": datetime.now(),
    }


def _research_report_row(source: str, scope: str, item: dict[str, Any], *, code: str = "", industry_code: str = "") -> dict[str, Any]:
    report_id = str(item.get("infoCode") or item.get("INFO_CODE") or item.get("reportId") or _hash(item))
    return {
        "source": source,
        "scope": scope,
        "code": code or _plain_code(item.get("stockCode") or item.get("SECURITY_CODE") or item.get("code") or ""),
        "industry_code": industry_code,
        "industry_name": item.get("industryName") or item.get("INDUSTRY_NAME"),
        "report_id": report_id,
        "title": item.get("title") or item.get("TITLE") or item.get("reportTitle") or "",
        "org_name": item.get("orgName") or item.get("ORG_NAME"),
        "author": item.get("researcher") or item.get("author") or item.get("ANALYST_NAME"),
        "publish_date": _date(item.get("publishDate") or item.get("PUBLISH_DATE") or item.get("date")),
        "rating": item.get("rating") or item.get("RATING_NAME"),
        "rating_change": item.get("ratingChange") or item.get("RATING_CHANGE"),
        "pdf_url": item.get("pdf_url") or item.get("attachUrl"),
        "url": item.get("url"),
        "metrics": _jsonish(item),
        "raw_payload": _jsonish(item),
    }


def _semantic_report_row(source: str, scope: str, item: dict[str, Any], params: dict[str, Any]) -> dict[str, Any]:
    return {
        "source": source,
        "scope": scope,
        "code": _plain_code(item.get("code") or item.get("股票代码") or ""),
        "industry_code": "",
        "industry_name": item.get("industry") or item.get("行业"),
        "report_id": str(item.get("id") or item.get("url") or _hash({"item": item, "query": params.get("query")})),
        "title": item.get("title") or item.get("标题") or str(params.get("query") or "iwencai"),
        "org_name": item.get("source") or item.get("机构"),
        "author": item.get("author") or item.get("作者"),
        "publish_date": _date(item.get("date") or item.get("发布日期")),
        "rating": item.get("rating"),
        "rating_change": item.get("rating_change"),
        "pdf_url": item.get("pdf_url"),
        "url": item.get("url"),
        "metrics": _jsonish(item),
        "raw_payload": _jsonish(item),
    }


def _ths_eps_row(item: dict[str, Any], *, code: str, index: int) -> dict[str, Any]:
    title = "THS EPS consensus"
    period = item.get("预测年度") or item.get("年度") or item.get("year") or item.get("报告期")
    return {
        "source": "ths.basic.eps_forecast",
        "scope": "eps_forecast",
        "code": code,
        "industry_code": "",
        "report_id": _hash({"code": code, "index": index, "item": item}),
        "title": f"{title} {period or index}",
        "publish_date": date.today(),
        "metrics": _jsonish(item),
        "raw_payload": _jsonish(item),
    }


def _financial_row(item: dict[str, Any], *, code: str, report_type: str) -> dict[str, Any]:
    report_date = _date(item.get("report_date") or item.get("date")) or date.today()
    return {"source": "sina.finance", "code": code, "report_type": report_type, "report_date": report_date, "fields": _jsonish(item), "raw_payload": _jsonish(item)}


def _announcement_row(source: str, item: dict[str, Any], *, code: str) -> dict[str, Any]:
    announcement_id = str(item.get("announcement_id") or item.get("id") or item.get("url") or _hash(item))
    return {
        "source": source,
        "code": code,
        "announcement_id": announcement_id,
        "title": item.get("title") or "",
        "category": item.get("type") or item.get("category"),
        "publish_date": _date(item.get("date") or item.get("time")),
        "url": item.get("url"),
        "raw_payload": _jsonish(item),
    }


def _news_row(source: str, item: dict[str, Any], *, code: str) -> dict[str, Any]:
    return {
        "source": source,
        "code": code,
        "news_id": str(item.get("id") or item.get("url") or _hash(item)),
        "title": item.get("title") or "",
        "summary": item.get("summary") or item.get("content"),
        "media_name": item.get("source") or item.get("mediaName"),
        "published_at": _datetime(item.get("time") or item.get("date") or item.get("showTime")),
        "url": item.get("url"),
        "raw_payload": _jsonish(item),
    }


def _interaction_row(source: str, item: dict[str, Any], *, code: str) -> dict[str, Any]:
    return {
        "source": source,
        "code": code or _plain_code(item.get("code") or item.get("stockCode")),
        "interaction_id": str(item.get("id") or _hash(item)),
        "company_name": item.get("company"),
        "question": item.get("question") or "",
        "answer": item.get("answer"),
        "answerer": item.get("answerer"),
        "asked_at": _datetime(item.get("ask_time") or item.get("date")),
        "raw_payload": _jsonish(item),
    }


def _fund_flow_daily_row(source: str, item: dict[str, Any], *, code: str) -> dict[str, Any]:
    return {
        "source": source,
        "code": code,
        "trade_date": _date(item.get("date")) or date.today(),
        "main_net": _num(item.get("main_net")),
        "small_net": _num(item.get("small_net")),
        "mid_net": _num(item.get("mid_net")),
        "large_net": _num(item.get("large_net")),
        "super_net": _num(item.get("super_net")),
        "raw_payload": _jsonish(item),
    }


def _fund_flow_minute_row(source: str, item: dict[str, Any], *, code: str, snapshot_date: date) -> dict[str, Any]:
    trade_time = _datetime(item.get("time"))
    if trade_time is None:
        minute = str(item.get("time") or "00:00")[-5:]
        parsed_time = _time(minute) or time(0, 0)
        trade_time = datetime.combine(snapshot_date, parsed_time)
    return {
        "source": source,
        "code": code,
        "trade_date": trade_time.date(),
        "trade_time": trade_time,
        "main_net": _num(item.get("main_net")),
        "small_net": _num(item.get("small_net")),
        "mid_net": _num(item.get("mid_net")),
        "large_net": _num(item.get("large_net")),
        "super_net": _num(item.get("super_net")),
        "raw_payload": _json_dumps(item),
        "ingested_at": datetime.now(),
    }


def _industry_rank_row(item: dict[str, Any], *, snapshot_date: date, direction: str, index: int) -> dict[str, Any]:
    return {
        "source": "eastmoney.push2.industry_rank",
        "snapshot_date": snapshot_date,
        "rank_direction": direction,
        "rank_no": int(item.get("rank") or index),
        "industry_code": str(item.get("code") or ""),
        "industry_name": item.get("name") or "",
        "change_pct": _num(item.get("change_pct")),
        "up_count": _int(item.get("up_count")),
        "down_count": _int(item.get("down_count")),
        "leader_name": item.get("leader"),
        "leader_change_pct": _num(item.get("leader_change")),
        "raw_payload": _jsonish(item),
    }


def _dragon_tiger_daily_row(item: dict[str, Any], *, trade_date: date) -> dict[str, Any]:
    return {
        "source": "eastmoney.datacenter.dailybillboard",
        "trade_date": trade_date,
        "code": _plain_code(item.get("code")),
        "name": item.get("name"),
        "reason": item.get("reason") or "",
        "close_price": _num(item.get("close")),
        "change_pct": _num(item.get("change_pct")),
        "net_buy_wan": _num(item.get("net_buy_wan")),
        "buy_wan": _num(item.get("buy_wan")),
        "sell_wan": _num(item.get("sell_wan")),
        "turnover_pct": _num(item.get("turnover_pct")),
        "raw_payload": _jsonish(item),
    }


def _dragon_tiger_record_row(item: dict[str, Any], *, code: str) -> dict[str, Any]:
    return {
        "source": "eastmoney.datacenter.dailybillboard",
        "trade_date": _date(item.get("date")) or date.today(),
        "code": code,
        "reason": item.get("reason") or "",
        "net_buy_wan": _num(item.get("net_buy")),
        "turnover_pct": _num(item.get("turnover")),
        "raw_payload": _jsonish(item),
    }


def _dragon_tiger_seat_rows(payload: dict[str, Any], *, code: str, trade_date: date) -> list[dict[str, Any]]:
    out = []
    seats = payload.get("seats") or {}
    for side in ("buy", "sell"):
        for index, item in enumerate(seats.get(side) or [], start=1):
            out.append(
                {
                    "source": "eastmoney.datacenter.dailybillboard",
                    "trade_date": trade_date,
                    "code": code,
                    "side": side,
                    "seat_rank": index,
                    "seat_name": item.get("name") or "",
                    "buy_wan": _num(item.get("buy_amt")),
                    "sell_wan": _num(item.get("sell_amt")),
                    "net_wan": _num(item.get("net")),
                    "raw_payload": _jsonish(item),
                }
            )
    return out


def _lockup_row(item: dict[str, Any], *, code: str) -> dict[str, Any]:
    return {"source": "eastmoney.datacenter.lockup", "code": code, "free_date": _date(item.get("date")) or date.today(), "lockup_type": item.get("type") or "", "shares": _num(item.get("shares")), "ratio": _num(item.get("ratio")), "raw_payload": _jsonish(item)}


def _block_trade_row(item: dict[str, Any], *, code: str) -> dict[str, Any]:
    return {"source": "eastmoney.datacenter.block_trade", "code": code, "trade_date": _date(item.get("date")) or date.today(), "price": _num(item.get("price")), "close_price": _num(item.get("close")), "premium_pct": _num(item.get("premium_pct")), "volume": _num(item.get("vol")), "amount": _num(item.get("amount")), "buyer": item.get("buyer") or "", "seller": item.get("seller") or "", "raw_payload": _jsonish(item)}


def _holder_row(item: dict[str, Any], *, code: str) -> dict[str, Any]:
    return {"source": "eastmoney.datacenter.holder_num", "code": code, "end_date": _date(item.get("date")) or date.today(), "holder_num": _int(item.get("holder_num")), "change_num": _int(item.get("change_num")), "change_ratio": _num(item.get("change_ratio")), "avg_shares": _num(item.get("avg_shares")), "raw_payload": _jsonish(item)}


def _dividend_row(item: dict[str, Any], *, code: str) -> dict[str, Any]:
    return {"source": "eastmoney.datacenter.dividend", "code": code, "ex_dividend_date": _date(item.get("date")) or date.today(), "bonus_rmb": _num(item.get("bonus_rmb")), "transfer_ratio": _num(item.get("transfer_ratio")), "bonus_ratio": _num(item.get("bonus_ratio")), "plan": item.get("plan"), "raw_payload": _jsonish(item)}


def _hot_rank_row(source: str, item: dict[str, Any], *, snapshot_date: date, list_type: str) -> dict[str, Any]:
    return {"source": source, "snapshot_date": snapshot_date, "list_type": list_type, "rank_no": _int(item.get("rank")) or 0, "code": _plain_code(item.get("code")), "name": item.get("name"), "heat": _num(item.get("heat")), "price": _num(item.get("price")), "pct": _num(item.get("pct")), "rank_change": _num(item.get("rank_chg")), "concepts": _jsonish(item.get("concepts") or []), "raw_payload": _jsonish(item)}


def _hot_concept_row(item: dict[str, Any], *, code: str, snapshot_date: date) -> dict[str, Any]:
    return {"source": "eastmoney.app.hot_concept", "snapshot_date": snapshot_date, "code": code, "concept_code": str(item.get("bk") or ""), "concept_name": item.get("concept") or "", "hit_count": _int(item.get("hit")), "raw_payload": _jsonish(item)}


def _concept_membership_row(item: dict[str, Any], *, code: str, snapshot_date: date) -> dict[str, Any]:
    return {"source": "eastmoney.push2.slist", "snapshot_date": snapshot_date, "code": code, "concept_code": str(item.get("code") or ""), "concept_name": item.get("name") or "", "change_pct": _num(item.get("change_pct")), "lead_stock": item.get("lead_stock"), "raw_payload": _jsonish(item)}


def _hot_reason_row(item: dict[str, Any], *, snapshot_date: date) -> dict[str, Any]:
    reason = item.get("reason") or item.get("reason_type") or item.get("Reason") or item.get("ztReason") or ""
    return {"source": "ths.hot_reason", "snapshot_date": snapshot_date, "code": _plain_code(item.get("code") or item.get("stockcode")), "name": item.get("name") or item.get("stockname"), "reason_key": _hash(reason), "reason": reason, "subject": item.get("subject") or item.get("event"), "raw_payload": _jsonish(item)}


def _limit_pool_row(source: str, item: dict[str, Any], *, snapshot_date: date, pool: str) -> dict[str, Any]:
    return {"source": source, "snapshot_date": snapshot_date, "pool": pool, "code": _plain_code(item.get("code")), "name": item.get("name"), "price": _num(item.get("price")), "pct": _num(item.get("pct")), "reason": item.get("reason"), "board_type": item.get("board_type"), "limit_days": str(item.get("limit_days") or item.get("high_days") or ""), "first_time": item.get("first_seal") or item.get("first_time") or "", "last_time": item.get("last_seal") or "", "break_times": _int(item.get("break_times")), "seal_amount": _num(item.get("seal_fund") or item.get("seal_amount")), "raw_payload": _jsonish(item)}


def _option_contract_row(option_code: Any, *, snapshot_date: date, underlying: str, option_month: str, option_type: str) -> dict[str, Any]:
    return {"source": "sina.option", "snapshot_date": snapshot_date, "underlying": underlying, "option_month": option_month, "option_type": option_type, "option_code": str(option_code), "raw_payload": {"option_code": str(option_code)}}


def _transaction_row(item: dict[str, Any], *, code: str, trade_date: date) -> dict[str, Any]:
    return {"source": "mootdx.transaction", "code": code, "trade_date": trade_date, "trade_time": str(item.get("time") or item.get("datetime") or item.get("date") or ""), "price": _num(item.get("price")), "volume": _int(item.get("vol") or item.get("volume")), "amount": _num(item.get("amount")), "buy_sell": str(item.get("buyorsell") or item.get("bs") or ""), "raw_payload": _json_dumps(item), "ingested_at": datetime.now()}


def _order_book_row(item: dict[str, Any], *, code: str) -> dict[str, Any]:
    now = datetime.now()
    bids = [item.get(key) for key in ("bid1", "bid2", "bid3", "bid4", "bid5") if key in item]
    asks = [item.get(key) for key in ("ask1", "ask2", "ask3", "ask4", "ask5") if key in item]
    bid_vols = [item.get(key) for key in ("bid_vol1", "bid_vol2", "bid_vol3", "bid_vol4", "bid_vol5") if key in item]
    ask_vols = [item.get(key) for key in ("ask_vol1", "ask_vol2", "ask_vol3", "ask_vol4", "ask_vol5") if key in item]
    return {"source": "mootdx.quote", "code": code, "quote_time": now, "trade_date": now.date(), "price": _num(item.get("price")), "open": _num(item.get("open")), "high": _num(item.get("high")), "low": _num(item.get("low")), "pre_close": _num(item.get("last_close") or item.get("pre_close")), "volume": _int(item.get("vol") or item.get("volume")), "amount": _num(item.get("amount")), "bid_prices": _json_dumps(bids), "ask_prices": _json_dumps(asks), "bid_volumes": _json_dumps(bid_vols), "ask_volumes": _json_dumps(ask_vols), "raw_payload": _json_dumps(item), "ingested_at": now}


def _baidu_kline_rows(payload: dict[str, Any], *, code: str) -> list[dict[str, Any]]:
    keys = [str(item) for item in payload.get("keys") or []]
    rows = []
    for line in payload.get("rows") or []:
        values = str(line).split(",")
        data = {keys[index]: values[index] for index in range(min(len(keys), len(values)))} if keys else {}
        if not data and len(values) >= 5:
            data = {"date": values[0], "open": values[1], "high": values[2], "low": values[3], "close": values[4]}
        trade_date = _date(data.get("date") or data.get("time") or values[0])
        if trade_date is None:
            continue
        rows.append(
            {
                "source": payload.get("source") or "baidu.selfselect.getstockquotation",
                "code": code,
                "trade_date": trade_date,
                "open": _num(data.get("open")),
                "high": _num(data.get("high")),
                "low": _num(data.get("low")),
                "close": _num(data.get("close")),
                "volume": _int(data.get("volume") or data.get("vol")),
                "amount": _num(data.get("amount")),
                "ma5": _num(data.get("ma5") or data.get("MA5")),
                "ma10": _num(data.get("ma10") or data.get("MA10")),
                "ma20": _num(data.get("ma20") or data.get("MA20")),
                "raw_payload": _json_dumps({"keys": keys, "line": line, "data": data}),
                "ingested_at": datetime.now(),
            }
        )
    return rows


def _northbound_row(item: dict[str, Any], *, snapshot_date: date) -> dict[str, Any]:
    return {"source": "ths.hexin.northbound", "trade_date": snapshot_date, "minute_time": str(item.get("time") or ""), "hgt_yi": _num(item.get("hgt_yi")), "sgt_yi": _num(item.get("sgt_yi")), "raw_payload": _json_dumps(item), "ingested_at": datetime.now()}


def _option_quote_row(dataset: str, item: dict[str, Any], *, option_code: str) -> dict[str, Any]:
    now = datetime.now()
    return {"source": "sina.option", "option_code": option_code, "quote_time": now, "trade_date": now.date(), "name": str(item.get("name") or ""), "last": _num(item.get("last")), "bid": _num(item.get("bid")), "ask": _num(item.get("ask")), "strike": _num(item.get("strike")), "volume": _int(item.get("volume")), "amount": _num(item.get("amount")), "open_interest": _int(item.get("open_interest")), "delta": _num(item.get("delta")), "gamma": _num(item.get("gamma")), "theta": _num(item.get("theta")), "vega": _num(item.get("vega")), "iv": _num(item.get("iv")), "raw_payload": _json_dumps({"dataset": dataset, "payload": item}), "ingested_at": now}


def _items(payload: Any) -> list[dict[str, Any]]:
    if payload is None:
        return []
    if isinstance(payload, dict) and isinstance(payload.get("items"), list):
        return [item for item in payload["items"] if isinstance(item, dict)]
    if isinstance(payload, list):
        return [item for item in payload if isinstance(item, dict)]
    return [payload] if isinstance(payload, dict) else []


def _plain_code(value: Any) -> str:
    text = str(value or "").strip().lower()
    if text.startswith(("sh", "sz", "bj")):
        text = text[2:]
    if "." in text:
        text = text.split(".", 1)[0]
    return text[-6:].zfill(6) if text else ""


def _market(code: str) -> str:
    if code.startswith("6"):
        return "SH"
    if code.startswith(("4", "8")):
        return "BJ"
    return "SZ" if code else ""


def _date(value: Any) -> date | None:
    if isinstance(value, date) and not isinstance(value, datetime):
        return value
    if isinstance(value, datetime):
        return value.date()
    text = str(value or "").strip()
    if not text:
        return None
    text = text.replace("/", "-")
    if len(text) == 8 and text.isdigit():
        text = f"{text[:4]}-{text[4:6]}-{text[6:8]}"
    try:
        return datetime.fromisoformat(text[:10]).date()
    except ValueError:
        return None


def _datetime(value: Any) -> datetime | None:
    if isinstance(value, datetime):
        return value
    text = str(value or "").strip().replace("/", "-")
    if not text:
        return None
    try:
        return datetime.fromisoformat(text)
    except ValueError:
        try:
            return datetime.fromisoformat(text[:19])
        except ValueError:
            return None


def _time(value: Any) -> time | None:
    text = str(value or "").strip()
    for fmt in ("%H:%M:%S", "%H:%M"):
        try:
            return datetime.strptime(text, fmt).time()
        except ValueError:
            continue
    return None


def _num(value: Any) -> float | None:
    if value in (None, "", "-", "--"):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", ""))
    except (TypeError, ValueError):
        return None


def _int(value: Any) -> int | None:
    parsed = _num(value)
    return int(parsed) if parsed is not None else None


def _bool(value: Any, default: bool) -> bool:
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "y", "call"}


def _hash(value: Any) -> str:
    return hashlib.sha1(_json_dumps(value).encode("utf-8")).hexdigest()


def _jsonish(value: Any) -> Any:
    return json.loads(_json_dumps(value))


def _json_dumps(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, default=str, separators=(",", ":"))
