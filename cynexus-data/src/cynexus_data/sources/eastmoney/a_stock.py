from __future__ import annotations

from datetime import date, datetime, timedelta
import json
import time
import re
import uuid
from typing import Any

from cynexus_data.sources.eastmoney.http import EastMoneyHttpClient, UA, diff_rows, em_secid, parse_jsonp, plain_code


DATACENTER_URL = "https://datacenter-web.eastmoney.com/api/data/v1/get"
REPORT_API = "https://reportapi.eastmoney.com/report/list"
PDF_TPL = "https://pdf.dfcfw.com/pdf/H3_{info_code}_1.pdf"
ZTB_UT = "7eea3edcaed734bea9cbfc24409ed989"
EM_HOT_BODY = {"appId": "appId01", "globalId": "786e4c21-70dc-435a-93bb-38"}


def datacenter(
    client: EastMoneyHttpClient,
    report_name: str,
    *,
    columns: str = "ALL",
    filter_str: str = "",
    page_size: int = 50,
    sort_columns: str = "",
    sort_types: str = "-1",
    page_number: int = 1,
) -> list[dict[str, Any]]:
    response = client.get(
        DATACENTER_URL,
        params={
            "reportName": report_name,
            "columns": columns,
            "filter": filter_str,
            "pageNumber": str(page_number),
            "pageSize": str(page_size),
            "sortColumns": sort_columns,
            "sortTypes": sort_types,
            "source": "WEB",
            "client": "WEB",
        },
    )
    result = response.json().get("result") or {}
    data = result.get("data") or []
    return data if isinstance(data, list) else []


def reports(client: EastMoneyHttpClient, code: str, *, max_pages: int = 1) -> list[dict[str, Any]]:
    return _report_list(client, q_type="0", code=plain_code(code), industry_code="*", max_pages=max_pages)


def industry_reports(
    client: EastMoneyHttpClient,
    *,
    industry_code: str = "*",
    max_pages: int = 1,
    begin: str = "2024-01-01",
) -> list[dict[str, Any]]:
    return _report_list(client, q_type="1", code="", industry_code=industry_code, max_pages=max_pages, begin=begin)


def report_pdf_url(record: dict[str, Any]) -> str | None:
    info_code = str(record.get("infoCode") or "").strip()
    return PDF_TPL.format(info_code=info_code) if info_code else None


def stock_info(client: EastMoneyHttpClient, code: str) -> dict[str, Any]:
    response = client.get(
        "https://push2.eastmoney.com/api/qt/stock/get",
        params={
            "fltt": "2",
            "invt": "2",
            "fields": "f57,f58,f84,f85,f100,f127,f116,f117,f189,f43,f20,f21",
            "secid": em_secid(code),
        },
        headers={"Referer": "https://quote.eastmoney.com/"},
    )
    data = response.json().get("data") or {}
    return {
        "code": data.get("f57") or plain_code(code),
        "name": data.get("f58"),
        "industry": data.get("f127") or data.get("f100"),
        "total_shares": data.get("f84"),
        "float_shares": data.get("f85"),
        "mcap": data.get("f116") or data.get("f20"),
        "float_mcap": data.get("f117") or data.get("f21"),
        "list_date": str(data.get("f189") or ""),
        "price": data.get("f43"),
        "source": "eastmoney.push2.stock.get",
    }


def concept_blocks(client: EastMoneyHttpClient, code: str) -> dict[str, Any]:
    response = client.get(
        "https://push2.eastmoney.com/api/qt/slist/get",
        params={
            "fltt": "2",
            "invt": "2",
            "secid": em_secid(code),
            "spt": "3",
            "pi": "0",
            "pz": "200",
            "po": "1",
            "fields": "f12,f14,f3,f128",
        },
        headers={"Referer": "https://quote.eastmoney.com/"},
    )
    boards = [
        {"name": row.get("f14"), "code": row.get("f12"), "change_pct": row.get("f3"), "lead_stock": row.get("f128")}
        for row in diff_rows(response.json())
    ]
    return {"total": len(boards), "boards": boards, "concept_tags": [b["name"] for b in boards if b.get("name")]}


def fund_flow_minute(client: EastMoneyHttpClient, code: str) -> list[dict[str, Any]]:
    response = client.get(
        "https://push2.eastmoney.com/api/qt/stock/fflow/kline/get",
        params={"secid": em_secid(code), "klt": 1, "fields1": "f1,f2,f3,f7", "fields2": "f51,f52,f53,f54,f55,f56,f57"},
        headers={"Referer": "https://quote.eastmoney.com/", "Origin": "https://quote.eastmoney.com"},
    )
    rows = []
    for line in ((response.json().get("data") or {}).get("klines") or []):
        parts = str(line).split(",")
        if len(parts) >= 6:
            rows.append(
                {
                    "time": parts[0],
                    "main_net": _float(parts[1]),
                    "small_net": _float(parts[2]),
                    "mid_net": _float(parts[3]),
                    "large_net": _float(parts[4]),
                    "super_net": _float(parts[5]),
                }
            )
    return rows


def fund_flow_120d(client: EastMoneyHttpClient, code: str, *, mode: str = "auto") -> dict[str, Any]:
    circuit_key = "push2his.stock_fund_flow"
    params = {
        "cb": f"jQuery1123{int(time.time() * 1000)}_{int(time.time() * 1000)}",
        "secid": em_secid(code),
        "fields1": "f1,f2,f3,f7",
        "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65",
        "lmt": "120",
        "klt": "101",
        "ut": "b2884a393a59ad64002292a3e90d46a5",
        "_": int(time.time() * 1000),
    }
    headers = {
        "Accept": "*/*",
        "Accept-Language": "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7",
        "Cache-Control": "no-cache",
        "Pragma": "no-cache",
        "Referer": f"https://data.eastmoney.com/zjlx/{str(code)[-6:]}.html",
        "Sec-Fetch-Dest": "script",
        "Sec-Fetch-Mode": "no-cors",
        "Sec-Fetch-Site": "same-site",
        "User-Agent": (
            "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 "
            "(KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36"
        ),
        "sec-ch-ua": '"Google Chrome";v="149", "Chromium";v="149", "Not)A;Brand";v="24"',
        "sec-ch-ua-mobile": "?1",
        "sec-ch-ua-platform": '"Android"',
    }
    if mode == "daily":
        return _fund_flow_delayed_daily(client, params=params, headers=headers, history_error=None)
    history_error: Exception | None = None
    if client.circuit_open(circuit_key):
        history_error = RuntimeError("EastMoney Push2His circuit is cooling down")
    else:
        try:
            rows = _fetch_fund_flow_history(client, params=params, headers=headers)
            if len(rows) > 1:
                client.close_circuit(circuit_key)
                return {
                    "items": rows,
                    "source": "eastmoney.push2his",
                    "mode": "history",
                    "requested_limit": 120,
                    "history_complete": True,
                }
            history_error = RuntimeError(f"EastMoney Push2His returned incomplete history: {len(rows)} row(s)")
        except Exception as exc:  # noqa: BLE001 - a delayed endpoint is the bounded source fallback.
            history_error = exc

        try:
            client.refresh_browser_cookies()
            rows = _fetch_fund_flow_history(client, params=params, headers=headers)
            if len(rows) > 1:
                client.close_circuit(circuit_key)
                return {
                    "items": rows,
                    "source": "eastmoney.push2his",
                    "mode": "history_browser_refreshed",
                    "requested_limit": 120,
                    "history_complete": True,
                }
            history_error = RuntimeError(f"EastMoney Push2His remained incomplete after browser refresh: {len(rows)} row(s)")
        except Exception as refresh_exc:  # noqa: BLE001 - fallback is intentional after one browser refresh.
            history_error = RuntimeError(f"{history_error}; browser refresh retry failed: {refresh_exc}")
        client.open_circuit(circuit_key)

    return _fund_flow_delayed_daily(client, params=params, headers=headers, history_error=history_error)


def _fund_flow_delayed_daily(
    client: EastMoneyHttpClient,
    *,
    params: dict[str, Any],
    headers: dict[str, str],
    history_error: Exception | None,
) -> dict[str, Any]:
    response = client.get(
        "https://push2delay.eastmoney.com/api/qt/stock/fflow/daykline/get",
        params=params,
        headers=headers,
    )
    rows = _fund_flow_daily_rows(_json_or_jsonp(response))
    if not rows:
        raise RuntimeError(f"EastMoney fund-flow history and delayed fallback failed: {history_error}")
    return {
        "items": rows,
        "source": "eastmoney.push2delay",
        "mode": "daily_fallback",
        "requested_limit": 120,
        "history_complete": False,
        "history_error": str(history_error) if history_error else None,
    }


def _fetch_fund_flow_history(
    client: EastMoneyHttpClient,
    *,
    params: dict[str, Any],
    headers: dict[str, str],
) -> list[dict[str, Any]]:
    response = client.get(
        "https://push2his.eastmoney.com/api/qt/stock/fflow/daykline/get",
        params={**params, "cb": f"jQuery1123{int(time.time() * 1000)}_{int(time.time() * 1000)}", "_": int(time.time() * 1000)},
        headers=headers,
    )
    return _fund_flow_daily_rows(_json_or_jsonp(response))


def _fund_flow_daily_rows(payload: dict[str, Any]) -> list[dict[str, Any]]:
    rows = []
    for line in ((payload.get("data") or {}).get("klines") or []):
        parts = str(line).split(",")
        if len(parts) >= 6:
            rows.append(
                {
                    "date": parts[0],
                    "main_net": _float(parts[1]),
                    "small_net": _float(parts[2]),
                    "mid_net": _float(parts[3]),
                    "large_net": _float(parts[4]),
                    "super_net": _float(parts[5]),
                }
            )
    return rows


def _json_or_jsonp(response) -> dict[str, Any]:
    try:
        return response.json()
    except (ValueError, json.JSONDecodeError):
        return parse_jsonp(response.text)


def industry_rank(client: EastMoneyHttpClient, *, top_n: int = 20) -> dict[str, Any]:
    response = client.get(
        "https://push2.eastmoney.com/api/qt/clist/get",
        params={
            "pn": "1",
            "pz": "100",
            "po": "1",
            "np": "1",
            "fltt": "2",
            "invt": "2",
            "fs": "m:90+t:2",
            "fields": "f2,f3,f4,f12,f13,f14,f104,f105,f128,f136,f140,f141,f207",
        },
        headers={"Referer": "https://quote.eastmoney.com/"},
    )
    rows = []
    for idx, item in enumerate(diff_rows(response.json()), start=1):
        rows.append(
            {
                "rank": idx,
                "name": item.get("f14"),
                "change_pct": item.get("f3"),
                "code": item.get("f12"),
                "up_count": item.get("f104"),
                "down_count": item.get("f105"),
                "leader": item.get("f140"),
                "leader_change": item.get("f136"),
            }
        )
    return {"top": rows[:top_n], "bottom": rows[-top_n:], "total": len(rows)}


def dragon_tiger_board(client: EastMoneyHttpClient, code: str, trade_date: str, *, look_back: int = 30) -> dict[str, Any]:
    end = datetime.strptime(trade_date, "%Y-%m-%d").date()
    start = (end - timedelta(days=look_back)).isoformat()
    code6 = plain_code(code)
    records = [
        {
            "date": str(row.get("TRADE_DATE") or "")[:10],
            "reason": row.get("EXPLANATION"),
            "net_buy": round(float(row.get("BILLBOARD_NET_AMT") or 0) / 10000, 1),
            "turnover": round(float(row.get("TURNOVERRATE") or 0), 2),
        }
        for row in datacenter(
            client,
            "RPT_DAILYBILLBOARD_DETAILSNEW",
            filter_str=f"(TRADE_DATE>='{start}')(TRADE_DATE<='{trade_date}')(SECURITY_CODE=\"{code6}\")",
            page_size=50,
            sort_columns="TRADE_DATE",
            sort_types="-1",
        )
    ]
    latest = records[0]["date"] if records else trade_date
    buy_rows = datacenter(client, "RPT_BILLBOARD_DAILYDETAILSBUY", filter_str=f"(TRADE_DATE='{latest}')(SECURITY_CODE=\"{code6}\")", page_size=10, sort_columns="BUY")
    sell_rows = datacenter(client, "RPT_BILLBOARD_DAILYDETAILSSELL", filter_str=f"(TRADE_DATE='{latest}')(SECURITY_CODE=\"{code6}\")", page_size=10, sort_columns="SELL")
    return {
        "records": records,
        "seats": {"buy": [_seat(row) for row in buy_rows[:5]], "sell": [_seat(row) for row in sell_rows[:5]]},
        "institution": _institution_stats(buy_rows, sell_rows),
    }


def daily_dragon_tiger(client: EastMoneyHttpClient, *, trade_date: str | None = None, min_net_buy: float | None = None) -> dict[str, Any]:
    day = trade_date or date.today().isoformat()
    rows = datacenter(
        client,
        "RPT_DAILYBILLBOARD_DETAILSNEW",
        filter_str=f"(TRADE_DATE>='{day}')(TRADE_DATE<='{day}')",
        page_size=500,
        sort_columns="BILLBOARD_NET_AMT",
        sort_types="-1",
    )
    out = []
    for row in rows:
        net_buy = float(row.get("BILLBOARD_NET_AMT") or 0) / 10000
        if min_net_buy is not None and net_buy < min_net_buy:
            continue
        out.append(
            {
                "code": row.get("SECURITY_CODE"),
                "name": row.get("SECURITY_NAME_ABBR"),
                "reason": row.get("EXPLANATION"),
                "close": row.get("CLOSE_PRICE"),
                "change_pct": row.get("CHANGE_RATE"),
                "net_buy_wan": round(net_buy, 1),
                "buy_wan": round(float(row.get("BILLBOARD_BUY_AMT") or 0) / 10000, 1),
                "sell_wan": round(float(row.get("BILLBOARD_SELL_AMT") or 0) / 10000, 1),
                "turnover_pct": row.get("TURNOVERRATE"),
            }
        )
    return {"date": str(rows[0].get("TRADE_DATE"))[:10] if rows else day, "total_records": len(out), "stocks": out}


def lockup_expiry(client: EastMoneyHttpClient, code: str, trade_date: str, *, forward_days: int = 90) -> dict[str, Any]:
    code6 = plain_code(code)
    end = (datetime.strptime(trade_date, "%Y-%m-%d").date() + timedelta(days=forward_days)).isoformat()
    history = [_lockup(row) for row in datacenter(client, "RPT_LIFT_STAGE", filter_str=f"(SECURITY_CODE=\"{code6}\")", page_size=15, sort_columns="FREE_DATE")]
    upcoming = [
        _lockup(row)
        for row in datacenter(
            client,
            "RPT_LIFT_STAGE",
            filter_str=f"(SECURITY_CODE=\"{code6}\")(FREE_DATE>='{trade_date}')(FREE_DATE<='{end}')",
            page_size=20,
            sort_columns="FREE_DATE",
            sort_types="1",
        )
    ]
    return {"history": history, "upcoming": upcoming}


def margin_trading(client: EastMoneyHttpClient, code: str, *, page_size: int = 30) -> list[dict[str, Any]]:
    return [
        {
            "date": str(row.get("DATE") or "")[:10],
            "rzye": row.get("RZYE"),
            "rzmre": row.get("RZMRE"),
            "rzche": row.get("RZCHE"),
            "rqye": row.get("RQYE"),
            "rqmcl": row.get("RQMCL"),
            "rqchl": row.get("RQCHL"),
            "rzrqye": row.get("RZRQYE"),
        }
        for row in datacenter(client, "RPTA_WEB_RZRQ_GGMX", filter_str=f'(SCODE="{plain_code(code)}")', page_size=page_size, sort_columns="DATE")
    ]


def block_trade(client: EastMoneyHttpClient, code: str, *, page_size: int = 20) -> list[dict[str, Any]]:
    out = []
    for row in datacenter(client, "RPT_DATA_BLOCKTRADE", filter_str=f'(SECURITY_CODE="{plain_code(code)}")', page_size=page_size, sort_columns="TRADE_DATE"):
        close = float(row.get("CLOSE_PRICE") or 0)
        price = float(row.get("DEAL_PRICE") or 0)
        out.append(
            {
                "date": str(row.get("TRADE_DATE") or "")[:10],
                "price": price,
                "close": close,
                "premium_pct": round((price / close - 1) * 100, 2) if close else None,
                "vol": row.get("DEAL_VOLUME"),
                "amount": row.get("DEAL_AMT"),
                "buyer": row.get("BUYER_NAME"),
                "seller": row.get("SELLER_NAME"),
            }
        )
    return out


def holder_num_change(client: EastMoneyHttpClient, code: str, *, page_size: int = 10) -> list[dict[str, Any]]:
    return [
        {
            "date": str(row.get("END_DATE") or "")[:10],
            "holder_num": row.get("HOLDER_NUM"),
            "change_num": row.get("HOLDER_NUM_CHANGE"),
            "change_ratio": row.get("HOLDER_NUM_RATIO"),
            "avg_shares": row.get("AVG_FREE_SHARES"),
        }
        for row in datacenter(client, "RPT_HOLDERNUMLATEST", filter_str=f'(SECURITY_CODE="{plain_code(code)}")', page_size=page_size, sort_columns="END_DATE")
    ]


def dividend_history(client: EastMoneyHttpClient, code: str, *, page_size: int = 20) -> list[dict[str, Any]]:
    return [
        {
            "date": str(row.get("EX_DIVIDEND_DATE") or "")[:10],
            "bonus_rmb": row.get("PRETAX_BONUS_RMB"),
            "transfer_ratio": row.get("TRANSFER_RATIO"),
            "bonus_ratio": row.get("BONUS_RATIO"),
            "plan": row.get("ASSIGN_PROGRESS"),
        }
        for row in datacenter(client, "RPT_SHAREBONUS_DET", filter_str=f'(SECURITY_CODE="{plain_code(code)}")', page_size=page_size, sort_columns="EX_DIVIDEND_DATE")
    ]


def stock_news(client: EastMoneyHttpClient, code: str, *, page_size: int = 20) -> list[dict[str, Any]]:
    inner = json.dumps(
        {
            "uid": "",
            "keyword": plain_code(code),
            "type": ["cmsArticleWebOld"],
            "client": "web",
            "clientType": "web",
            "clientVersion": "curr",
            "param": {"cmsArticleWebOld": {"searchScope": "default", "sort": "default", "pageIndex": 1, "pageSize": page_size, "preTag": "", "postTag": ""}},
        },
        separators=(",", ":"),
    )
    response = client.get("https://search-api-web.eastmoney.com/search/jsonp", params={"cb": "jQuery_news", "param": inner}, headers={"Referer": "https://so.eastmoney.com/"})
    payload = parse_jsonp(response.text)
    articles = ((payload.get("result") or {}).get("cmsArticleWebOld") or [])
    return [
        {
            "title": _strip_html(item.get("title")),
            "content": _strip_html(item.get("content"))[:200],
            "time": item.get("date"),
            "source": item.get("mediaName"),
            "url": item.get("url"),
        }
        for item in articles
        if isinstance(item, dict)
    ]


def global_news(client: EastMoneyHttpClient, *, page_size: int = 50) -> list[dict[str, Any]]:
    target = min(max(page_size, 1), 1000)
    cursor = ""
    seen: set[str] = set()
    result: list[dict[str, Any]] = []
    while len(result) < target:
        batch_size = min(50, target - len(result))
        response = client.get(
            "https://np-weblist.eastmoney.com/comm/web/getFastNewsList",
            params={"client": "web", "biz": "web_724", "fastColumn": "102", "sortEnd": cursor, "pageSize": str(batch_size), "req_trace": str(uuid.uuid4())},
            headers={"Referer": "https://kuaixun.eastmoney.com/"},
        )
        data = response.json().get("data") or {}
        rows = [row for row in data.get("fastNewsList") or [] if isinstance(row, dict)]
        if not rows:
            break
        for row in rows:
            item_id = str(row.get("code") or row.get("realSort") or "")
            if not item_id or item_id in seen:
                continue
            seen.add(item_id)
            result.append(
                {
                    "id": item_id,
                    "title": row.get("title"),
                    "summary": str(row.get("summary") or "")[:500],
                    "time": row.get("showTime"),
                    "stock_list": row.get("stockList") or [],
                    "raw_payload": row,
                }
            )
            if len(result) >= target:
                break
        next_cursor = str(data.get("sortEnd") or rows[-1].get("realSort") or "")
        if len(rows) < batch_size or not next_cursor or next_cursor == cursor:
            break
        cursor = next_cursor
    return result


def limit_pool(client: EastMoneyHttpClient, pool: str, trade_date: str) -> list[dict[str, Any]]:
    endpoint_by_pool = {"zt": "getTopicZTPool", "zb": "getTopicZBPool", "dt": "getTopicDTPool", "yzt": "getYesterdayZTPool"}
    sort_by_pool = {"zt": "fbt:asc", "zb": "fbt:asc", "dt": "fund:asc", "yzt": "zs:desc"}
    key = str(pool or "zt").lower()
    endpoint = endpoint_by_pool.get(key)
    if endpoint is None:
        raise ValueError("pool must be one of zt, zb, dt, yzt")
    response = client.get(
        f"https://push2ex.eastmoney.com/{endpoint}",
        params={"ut": ZTB_UT, "dpt": "wz.ztzt", "Pageindex": 0, "pagesize": 10000, "sort": sort_by_pool[key], "date": trade_date},
        headers={"Referer": "https://quote.eastmoney.com/"},
    )
    raw_rows = (response.json().get("data") or {}).get("pool") or []
    return [_limit_row(key, row) for row in raw_rows if isinstance(row, dict)]


def limit_sentiment(client: EastMoneyHttpClient, trade_date: str) -> dict[str, Any]:
    zt = limit_pool(client, "zt", trade_date)
    zb = limit_pool(client, "zb", trade_date)
    dt = limit_pool(client, "dt", trade_date)
    ladder: dict[int, int] = {}
    for row in zt:
        days = int(row.get("limit_days") or 0)
        ladder[days] = ladder.get(days, 0) + 1
    return {
        "date": trade_date,
        "zt_count": len(zt),
        "zb_count": len(zb),
        "dt_count": len(dt),
        "break_rate": round(len(zb) / (len(zt) + len(zb)) * 100, 1) if zt or zb else 0,
        "max_height": max(ladder.keys(), default=0),
        "ladder": dict(sorted(ladder.items())),
    }


def hot_rank(client: EastMoneyHttpClient, *, top: int = 50) -> list[dict[str, Any]]:
    response = client.post("https://emappdata.eastmoney.com/stockrank/getAllCurrentList", json_body={**EM_HOT_BODY, "marketType": "", "pageNo": 1, "pageSize": top})
    data = response.json().get("data") or []
    if not data:
        return []
    secids = [("0." if str(item.get("sc")).startswith("SZ") else "1.") + str(item.get("sc"))[2:] for item in data]
    quote = client.get(
        "https://push2.eastmoney.com/api/qt/ulist.np/get",
        params={"ut": "f057cbcbce2a86e2866ab8877db1d059", "fltt": 2, "invt": 2, "fields": "f14,f3,f12,f2", "secids": ",".join(secids)},
        headers={"Referer": "https://quote.eastmoney.com/"},
    )
    names = {row.get("f12"): (row.get("f14"), row.get("f2"), row.get("f3")) for row in diff_rows(quote.json())}
    out = []
    for item in data:
        code = str(item.get("sc") or "")[2:]
        name, price, pct = names.get(code, ("", None, None))
        out.append({"rank": item.get("rk"), "code": code, "name": name, "price": price, "pct": pct, "rank_chg": item.get("hisRc")})
    return out


def hot_concept(client: EastMoneyHttpClient, code: str) -> list[dict[str, Any]]:
    code6 = plain_code(code)
    prefix = "SH" if code6.startswith("6") else "SZ"
    response = client.post("https://emappdata.eastmoney.com/stockrank/getHotStockRankList", json_body={**EM_HOT_BODY, "srcSecurityCode": prefix + code6})
    return [{"concept": item.get("conceptName"), "bk": item.get("conceptId"), "hit": item.get("hitCount")} for item in response.json().get("data") or []]


def _report_list(
    client: EastMoneyHttpClient,
    *,
    q_type: str,
    code: str,
    industry_code: str,
    max_pages: int,
    begin: str = "2000-01-01",
) -> list[dict[str, Any]]:
    all_rows: list[dict[str, Any]] = []
    for page in range(1, max_pages + 1):
        response = client.get(
            REPORT_API,
            params={
                "industryCode": industry_code,
                "pageSize": "100",
                "industry": "*",
                "rating": "*",
                "ratingChange": "*",
                "beginTime": begin,
                "endTime": "2030-01-01",
                "pageNo": str(page),
                "fields": "",
                "qType": q_type,
                "orgCode": "",
                "code": code,
                "rcode": "",
                "p": str(page),
                "pageNum": str(page),
                "pageNumber": str(page),
            },
            headers={"Referer": "https://data.eastmoney.com/"},
            timeout=30,
        )
        payload = response.json()
        rows = payload.get("data") or []
        if not rows:
            break
        all_rows.extend(rows)
        if page >= int(payload.get("TotalPage") or 1):
            break
    return all_rows


def _seat(row: dict[str, Any]) -> dict[str, Any]:
    return {"name": row.get("OPERATEDEPT_NAME"), "buy_amt": round(float(row.get("BUY") or 0) / 10000, 1), "sell_amt": round(float(row.get("SELL") or 0) / 10000, 1), "net": round(float(row.get("NET") or 0) / 10000, 1)}


def _institution_stats(buy_rows: list[dict[str, Any]], sell_rows: list[dict[str, Any]]) -> dict[str, float]:
    buy = sum(float(row.get("BUY") or 0) for row in buy_rows if str(row.get("OPERATEDEPT_CODE") or "") == "0")
    sell = sum(float(row.get("SELL") or 0) for row in sell_rows if str(row.get("OPERATEDEPT_CODE") or "") == "0")
    return {"buy_amt": round(buy / 10000, 1), "sell_amt": round(sell / 10000, 1), "net_amt": round((buy - sell) / 10000, 1)}


def _lockup(row: dict[str, Any]) -> dict[str, Any]:
    return {"date": str(row.get("FREE_DATE") or "")[:10], "type": row.get("LIMITED_STOCK_TYPE"), "shares": row.get("FREE_SHARES_NUM"), "ratio": row.get("FREE_RATIO")}


def _limit_row(pool: str, row: dict[str, Any]) -> dict[str, Any]:
    common = {"code": row.get("c"), "name": row.get("n"), "price": _scaled_price(row.get("p")), "pct": row.get("zdp"), "turnover": row.get("hs"), "industry": row.get("hybk")}
    if pool == "zt":
        return {**common, "amount": row.get("amount"), "float_cap": row.get("ltsz"), "limit_days": row.get("lbc"), "first_seal": _zt_time(row.get("fbt")), "last_seal": _zt_time(row.get("lbt")), "seal_fund": row.get("fund"), "break_times": row.get("zbc"), "zt_stat": _zt_stat(row)}
    if pool == "zb":
        return {**common, "limit_price": _scaled_price(row.get("ztp")), "first_seal": _zt_time(row.get("fbt")), "break_times": row.get("zbc"), "amplitude": row.get("zf"), "speed": row.get("zs"), "zt_stat": _zt_stat(row)}
    if pool == "dt":
        return {**common, "pe": row.get("pe"), "seal_fund": row.get("fund"), "last_seal": _zt_time(row.get("lbt")), "board_amount": row.get("fba"), "dt_days": row.get("days"), "open_times": row.get("oc")}
    return {**common, "amplitude": row.get("zf"), "speed": row.get("zs"), "y_first_seal": _zt_time(row.get("yfbt")), "y_limit_days": row.get("ylbc"), "zt_stat": _zt_stat(row)}


def _zt_stat(row: dict[str, Any]) -> str:
    stat = row.get("zttj") or {}
    return f"{stat.get('days', '?')}天{stat.get('ct', '?')}板"


def _zt_time(value: Any) -> str:
    if value in (None, "", "-"):
        return ""
    text = str(value).zfill(6)
    return f"{text[:2]}:{text[2:4]}:{text[4:6]}"


def _scaled_price(value: Any) -> float | None:
    parsed = _float(value)
    return parsed / 1000 if parsed is not None else None


def _float(value: Any) -> float | None:
    if value in (None, "", "-", "--"):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except (TypeError, ValueError):
        return None


def _strip_html(value: Any) -> str:
    return re.sub(r"<[^>]+>", "", str(value or ""))
