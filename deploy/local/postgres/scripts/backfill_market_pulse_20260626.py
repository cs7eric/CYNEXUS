from __future__ import annotations

import argparse
import json
import math
import re
import time
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any

import clickhouse_connect


ROOT = Path(r"F:\dev-repo\mp4-to-word-new\reference")
TRADE_DATE = "2026-06-26"
STYLE_SECTOR_NAMES = [
    "近期新低", "微小盘股", "微盘股", "近期弱势", "昨日较弱",
    "最近情绪指数", "百元股", "近期新高", "大盘股", "低价股",
    "最近异动", "昨日弱势", "自由现金流", "行业龙头", "历史新低",
    "近期强势", "昨高换手", "昨日涨停", "机构吸筹", "最近多板",
    "昨日跌停", "昨日首板", "昨曾跌停", "历史新高", "昨日较强",
    "昨曾涨停", "昨日强势", "昨日连板", "昨日断板",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Backfill Market Pulse PG tables from local 2026-06-26 reference files.")
    parser.add_argument("--reference-root", default=str(ROOT))
    parser.add_argument("--trade-date", default=TRADE_DATE)
    parser.add_argument("--output", default=r"E:\project-repo\cynexus\deploy\local\postgres\scripts\market_pulse_20260626_backfill.sql")
    parser.add_argument("--clickhouse-host", default="127.0.0.1")
    parser.add_argument("--clickhouse-port", type=int, default=28123)
    parser.add_argument("--clickhouse-database", default="cynexus")
    parser.add_argument("--clickhouse-user", default="cynexus")
    parser.add_argument("--clickhouse-password", default="C020611.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    reference_root = Path(args.reference_root)
    quotes = load_daily_quotes(args)
    statements: list[str] = [
        "begin;",
        "set search_path to cynexus_appl_market, public;",
    ]

    limit_sql, limit_counts = build_limit_sql(reference_root, args.trade_date)
    statements.extend(limit_sql)

    industries_payload = build_industries_payload(reference_root, args.trade_date)
    statements.extend(upsert_heatmap_sql(args.trade_date, "industries", industries_payload))

    concepts_payload = build_sector_payload(
        reference_root / "stock-universe" / "sectors" / "sectors_concepts_2.json",
        args.trade_date,
        "concepts",
        "概念",
        quotes,
    )
    statements.extend(upsert_heatmap_sql(args.trade_date, "concepts", concepts_payload))

    style_rows = build_style_rows(reference_root, args.trade_date, quotes)
    statements.extend(upsert_style_sql(args.trade_date, style_rows))

    statements.append("commit;")
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(statements) + "\n", encoding="utf-8")
    print(f"[done] wrote {output}")
    print(f"[limit] {limit_counts}")
    print(f"[heatmap] industries={len(industries_payload['items'])} concepts={len(concepts_payload['items'])}")
    print(f"[style] rows={len(style_rows)}")


def load_daily_quotes(args: argparse.Namespace) -> dict[str, dict[str, float]]:
    client = clickhouse_connect.get_client(
        host=args.clickhouse_host,
        port=args.clickhouse_port,
        username=args.clickhouse_user,
        password=args.clickhouse_password,
        database=args.clickhouse_database,
        http_proxy="",
        https_proxy="",
    )
    sql = """
        select
            cur.code,
            toFloat64(cur.open),
            toFloat64(cur.high),
            toFloat64(cur.low),
            toFloat64(cur.close),
            toFloat64(cur.volume),
            toFloat64(cur.amount),
            toFloat64(prev.close) as pre_close
        from daily_qfq cur
        left join daily_qfq prev
          on prev.code = cur.code
         and prev.trade_date = (
             select max(trade_date) from daily_qfq where trade_date < {trade_date:Date}
         )
        where cur.trade_date = {trade_date:Date}
    """
    rows = client.query(sql, parameters={"trade_date": args.trade_date}).result_rows
    quotes: dict[str, dict[str, float]] = {}
    for code, open_, high, low, close, volume, amount, pre_close in rows:
        pct = None
        if pre_close and pre_close > 0:
            pct = (float(close) - float(pre_close)) / float(pre_close) * 100.0
        quotes[str(code).zfill(6)] = {
            "open": float(open_ or 0),
            "high": float(high or 0),
            "low": float(low or 0),
            "close": float(close or 0),
            "volume": float(volume or 0),
            "amount_yuan": float(amount or 0),
            "change_pct": pct,
        }
    return quotes


def build_limit_sql(reference_root: Path, trade_date: str) -> tuple[list[str], dict[str, Any]]:
    blob = read_json(reference_root / "market-limit" / "daily" / f"{trade_date}.json")
    stocks = [item for item in blob.get("stocks", []) if isinstance(item, dict)]
    limit_up = [item for item in stocks if item.get("isLimitUp") is True]
    touched = [item for item in stocks if item.get("isTouchedLimitUp") is True]
    broken = [item for item in stocks if item.get("isBrokenLimitUp") is True]
    limit_down = [item for item in stocks if is_limit_down(item)]
    max_streak = max([int(item.get("limitUpStreak") or 0) for item in stocks], default=0)
    promoted = [item for item in stocks if item.get("isPromoted") is True]
    previous_streak = [item for item in stocks if int(item.get("previousLimitUpStreak") or 0) > 0]
    break_rate = round(len(broken) / len(touched), 4) if touched else 0.0
    promotion_rate = round(len(promoted) / len(previous_streak), 4) if previous_streak else 0.0

    distribution = [
        {"height": streak, "count": count}
        for streak, count in sorted(Counter(int(item.get("limitUpStreak") or 0) for item in limit_up).items(), reverse=True)
        if streak > 0
    ]
    leaders = sorted(limit_up, key=lambda item: (int(item.get("limitUpStreak") or 0), float(item.get("changePct") or 0)), reverse=True)[:20]
    sentiment_level = sentiment(len(limit_up), break_rate)
    payload = {
        "ok": True,
        "tradeDate": trade_date,
        "updateTime": normalize_datetime(blob.get("updateTime")),
        "marketStatus": blob.get("marketStatus") or "closed",
        "dataStatus": "normal",
        "source": blob.get("source") or "reference.market-limit.daily",
        "limitUp": {"count": len(limit_up), "label": "涨停", "stocks": [limit_stock(item) for item in limit_up[:100]]},
        "limitDown": {"count": len(limit_down), "label": "跌停", "stocks": [limit_stock(item) for item in limit_down[:100]]},
        "breakBoard": {
            "touchedCount": len(touched),
            "brokenCount": len(broken),
            "rate": break_rate,
            "status": "ready",
            "label": "炸板率",
            "brokenStocks": [limit_stock(item) for item in broken[:100]],
        },
        "streak": {
            "maxHeight": max_streak,
            "label": "连板高度",
            "leaders": [limit_stock(item) for item in leaders],
            "distribution": distribution,
            "promotion": {"overallRate": promotion_rate, "levels": []},
            "broken": {
                "count": len([item for item in stocks if item.get("isBrokenStreak") is True]),
                "highStreakBrokenCount": len([item for item in stocks if item.get("isBrokenStreak") is True and int(item.get("previousLimitUpStreak") or 0) >= 2]),
                "stocks": [limit_stock(item) for item in stocks if item.get("isBrokenStreak") is True][:50],
            },
            "sentiment": {"level": sentiment_level, "text": sentiment_text(sentiment_level, len(limit_up), break_rate)},
        },
        "_meta": {
            "backfill": "deploy/local/postgres/scripts/backfill_market_pulse_20260626.py",
            "rawFile": str(reference_root / "market-limit" / "daily" / f"{trade_date}.json"),
            "stockCount": blob.get("stockCount"),
        },
    }
    update_time = normalize_datetime(blob.get("updateTime"))
    source = str(blob.get("source") or "reference.market-limit.daily")[:64]
    sql = [
        f"delete from market_limit_daily_stocks where trade_date = date '{trade_date}' and deleted_at is null;",
        f"delete from market_limit_daily_snapshots where trade_date = date '{trade_date}' and deleted_at is null;",
        f"""
insert into market_limit_daily_snapshots (
    id, trade_date, limit_up_count, limit_down_count, touched_count, broken_count,
    break_board_rate, max_streak_height, promotion_overall_rate, sentiment_level,
    sentiment_text, stock_count, market_status, data_status, source, extra, created_at, updated_at
) values (
    {next_id()}, date '{trade_date}', {len(limit_up)}, {len(limit_down)}, {len(touched)}, {len(broken)},
    {break_rate}, {max_streak}, {promotion_rate}, {sql_str(sentiment_level)},
    {sql_str(sentiment_text(sentiment_level, len(limit_up), break_rate))}, {int(blob.get("stockCount") or len(stocks))},
    {sql_str(blob.get("marketStatus") or "closed")}, 'normal', {sql_str(source)}, {sql_json(payload)}, {sql_ts(update_time)}, {sql_ts(update_time)}
);
""".strip(),
    ]
    stock_rows = []
    seen: set[str] = set()
    for category, items in [("limit_up", limit_up), ("limit_down", limit_down), ("broken", broken)]:
        for item in items:
            code = str(item.get("code") or "").zfill(6)
            if not code or code in seen:
                continue
            seen.add(code)
            stock_rows.append(
                f"({next_id()}, date '{trade_date}', {sql_str(code)}, {sql_str(item.get('name') or code)}, "
                f"{sql_str(category)}, {int(item.get('limitUpStreak') or 0)}, {sql_num(item.get('changePct'))}, "
                f"{sql_num(item.get('limitUpPrice'))}, {sql_num(item.get('limitDownPrice'))}, {sql_ts(update_time)}, {sql_ts(update_time)})"
            )
    if stock_rows:
        sql.append(
            "insert into market_limit_daily_stocks "
            "(id, trade_date, code, name, category, streak, change_pct, limit_up_price, limit_down_price, created_at, updated_at) values\n"
            + ",\n".join(stock_rows)
            + ";"
        )
    return sql, {"limit_up": len(limit_up), "limit_down": len(limit_down), "touched": len(touched), "broken": len(broken)}


def build_industries_payload(reference_root: Path, trade_date: str) -> dict[str, Any]:
    industry_list = read_json(reference_root / "ths-industry" / "industry_list.json")
    by_code = industry_list.get("byCode") or {}
    items: list[dict[str, Any]] = []
    fetched_values: list[str] = []
    for code, info in by_code.items():
        blob_path = reference_root / "ths-industry" / "constituents" / f"{code}.json"
        if not blob_path.exists():
            continue
        blob = read_json(blob_path)
        rows = [row for row in blob.get("rows", []) if isinstance(row, dict)]
        if blob.get("fetchedAt"):
            fetched_values.append(str(blob.get("fetchedAt")))
        amount_sum = 0.0
        weighted_pct = 0.0
        mcap_sum = 0.0
        valid = 0
        for row in rows:
            amount = parse_amount_yi(row.get("成交额"))
            pct = parse_float(row.get("涨跌幅(%)"))
            if amount is None or pct is None:
                continue
            amount_sum += amount
            weighted_pct += amount * pct
            valid += 1
            mcap = parse_amount_yi(row.get("流通市值"))
            if mcap is not None:
                mcap_sum += mcap
        change_pct = round(weighted_pct / amount_sum, 4) if amount_sum > 0 else None
        items.append({
            "name": (info or {}).get("name") or code,
            "sectorCode": code,
            "fullCode": code,
            "kind": "industries",
            "kindLabel": "行业 (同花顺)",
            "topicId": [code],
            "value": round(amount_sum, 4),
            "changePercent": change_pct,
            "amount": round(amount_sum, 4),
            "circulatingMarketCap": round(mcap_sum, 4),
            "stockCount": len(rows),
            "risingCount": 0,
            "fallingCount": 0,
            "flatCount": 0,
            "mainNetInflow": 0,
            "turnoverRateAvg": None,
            "speedAvg": None,
            "limitUpCount": 0,
            "limitStreakCount": 0,
            "conceptTags": [],
            "children": [],
            "snapshotFetchedAt": blob.get("fetchedAt"),
            "validSize": valid,
        })
    items.sort(key=lambda item: -(item.get("amount") or 0))
    return {
        "ok": True,
        "kind": "industries",
        "kinds": ["行业 (同花顺)"],
        "items": items,
        "totalItems": len(items),
        "itemsTotal": len(items),
        "topN": 200,
        "totalStocks": sum(int(item.get("stockCount") or 0) for item in items),
        "hiddenStocks": 0,
        "hiddenNoQuote": 0,
        "hiddenEmptySectors": 0,
        "fetchedAt": min(fetched_values) if fetched_values else f"{trade_date}T15:30:00",
        "source": "reference/ths-industry snapshot (industry_list.json + constituents/{code}.json)",
    }


def build_sector_payload(path: Path, trade_date: str, kind: str, label: str, quotes: dict[str, dict[str, float]]) -> dict[str, Any]:
    blob = read_json(path)
    items: list[dict[str, Any]] = []
    all_codes: set[str] = set()
    for sector in blob.get("sectors", []):
        if not isinstance(sector, dict):
            continue
        code_values = [normalize_code(code) for code in sector.get("stock_codes", [])]
        code_values = [code for code in code_values if code]
        all_codes.update(code_values)
        valid_quotes = [(code, quotes[code]) for code in code_values if code in quotes and quotes[code].get("change_pct") is not None]
        if not valid_quotes:
            continue
        amount_yi = sum(row.get("amount_yuan", 0.0) for _, row in valid_quotes) / 1e8
        pcts = [row["change_pct"] for _, row in valid_quotes if row.get("change_pct") is not None]
        change_pct = round(sum(pcts) / len(pcts), 4) if pcts else None
        rising = sum(1 for pct in pcts if pct > 0)
        falling = sum(1 for pct in pcts if pct < 0)
        flat = len(pcts) - rising - falling
        limit_up = sum(1 for pct in pcts if pct >= 9.7)
        items.append({
            "name": sector.get("name"),
            "sectorCode": sector.get("name"),
            "kind": kind,
            "kindLabel": label,
            "topicId": [sector.get("topic_id")] if sector.get("topic_id") else [],
            "value": round(amount_yi, 4),
            "changePercent": change_pct,
            "amount": round(amount_yi, 4),
            "circulatingMarketCap": 0,
            "stockCount": len(valid_quotes),
            "risingCount": rising,
            "fallingCount": falling,
            "flatCount": flat,
            "mainNetInflow": 0,
            "turnoverRateAvg": None,
            "speedAvg": None,
            "limitUpCount": limit_up,
            "limitStreakCount": 0,
            "conceptTags": [],
            "children": [],
        })
    items.sort(key=lambda item: -(item.get("value") or 0))
    items = items[:200]
    visible = {normalize_code(code) for item in items for code in item.get("topicId", [])}
    return {
        "ok": True,
        "kind": kind,
        "kinds": [label],
        "items": items,
        "totalItems": len(items),
        "itemsTotal": len(items),
        "topN": 200,
        "totalStocks": len(all_codes),
        "hiddenStocks": max(0, len(all_codes) - len(visible)),
        "hiddenNoQuote": len([code for code in all_codes if code not in quotes]),
        "hiddenEmptySectors": 0,
        "fetchedAt": f"{trade_date}T15:30:00",
        "source": f"{path.name} + clickhouse.daily_qfq",
    }


def build_style_rows(reference_root: Path, trade_date: str, quotes: dict[str, dict[str, float]]) -> list[dict[str, Any]]:
    blob = read_json(reference_root / "stock-universe" / "sectors" / "sectors_styles_4.json")
    by_name = {sector.get("name"): sector for sector in blob.get("sectors", []) if isinstance(sector, dict)}
    rows: list[dict[str, Any]] = []
    for name in STYLE_SECTOR_NAMES:
        sector = by_name.get(name) or {}
        codes = [normalize_code(code) for code in sector.get("stock_codes", [])]
        codes = [code for code in codes if code]
        valid = [(code, quotes[code]) for code in codes if code in quotes and quotes[code].get("change_pct") is not None]
        pcts = [row["change_pct"] for _, row in valid]
        avg = round(sum(pcts) / len(pcts), 4) if pcts else None
        constituents = [
            {"code": code, "name": code, "change_pct": round(row["change_pct"], 4), "amount": round(row.get("amount_yuan", 0.0) / 1e8, 4)}
            for code, row in sorted(valid, key=lambda pair: pair[1].get("change_pct") or -999, reverse=True)[:300]
        ]
        rows.append({
            "name": name,
            "change_pct": avg,
            "valid_size": len(valid),
            "sample_size": len(codes),
            "payload": {
                "name": name,
                "tradeDate": trade_date,
                "change_pct": avg,
                "valid_size": len(valid),
                "sample_size": len(codes),
                "constituents": constituents,
                "source": "sectors_styles_4.json + clickhouse.daily_qfq",
            },
        })
    rows.sort(key=lambda row: row["change_pct"] if row["change_pct"] is not None else -999, reverse=True)
    for index, row in enumerate(rows, start=1):
        row["rank"] = index
    return rows


def upsert_heatmap_sql(trade_date: str, kind: str, payload: dict[str, Any]) -> list[str]:
    source = payload.get("source") or f"reference.{kind}"
    return [
        f"delete from mkt_heatmap_snapshot where snapshot_date = date '{trade_date}' and kind = {sql_str(kind)} and deleted_at is null;",
        "insert into mkt_heatmap_snapshot (snapshot_date, kind, source, payload, item_count, generated_at, created_at, updated_at) values "
        f"(date '{trade_date}', {sql_str(kind)}, {sql_str(source)}, {sql_json(payload)}, {len(payload.get('items') or [])}, now(), now(), now());",
    ]


def upsert_style_sql(trade_date: str, rows: list[dict[str, Any]]) -> list[str]:
    sql = [f"delete from mkt_style_sector_snapshot where trade_date = date '{trade_date}' and deleted_at is null;"]
    values = []
    for row in rows:
        values.append(
            f"(date '{trade_date}', {sql_str(row['name'])}, {row['rank']}, {sql_num(row['change_pct'])}, "
            f"{row['valid_size']}, {row['sample_size']}, 'clickhouse.daily_qfq+stock-universe.styles', {sql_json(row['payload'])}, now(), now())"
        )
    if values:
        sql.append(
            "insert into mkt_style_sector_snapshot "
            "(trade_date, name, rank_by_change, change_pct, valid_size, sample_size, source, payload, ingested_at, updated_at) values\n"
            + ",\n".join(values)
            + ";"
        )
    return sql


def read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def next_id() -> int:
    next_id.counter += 1
    return int(time.time() * 1000) * 1000 + next_id.counter


next_id.counter = 0


def normalize_code(raw: Any) -> str:
    value = str(raw or "").strip().lower()
    match = re.search(r"(\d{6})", value)
    return match.group(1) if match else ""


def is_limit_down(item: dict[str, Any]) -> bool:
    latest = parse_float(item.get("latestPrice"))
    limit_down = parse_float(item.get("limitDownPrice"))
    return latest is not None and limit_down is not None and latest <= limit_down * 1.001


def limit_stock(item: dict[str, Any]) -> dict[str, Any]:
    code = str(item.get("code") or "").zfill(6)
    return {
        "code": code,
        "name": item.get("name") or code,
        "streak": int(item.get("limitUpStreak") or 0),
        "previousStreak": int(item.get("previousLimitUpStreak") or 0),
        "changePct": parse_float(item.get("changePct")),
        "latestPrice": parse_float(item.get("latestPrice")),
        "limitUpPrice": parse_float(item.get("limitUpPrice")),
        "limitDownPrice": parse_float(item.get("limitDownPrice")),
    }


def sentiment(limit_up_count: int, break_rate: float) -> str:
    if limit_up_count >= 80 and break_rate < 0.35:
        return "hot"
    if limit_up_count >= 50 and break_rate < 0.5:
        return "warm"
    if limit_up_count <= 30 or break_rate >= 0.65:
        return "cold"
    return "normal"


def sentiment_text(level: str, limit_up_count: int, break_rate: float) -> str:
    return {
        "hot": f"涨停 {limit_up_count} 家，炸板率 {break_rate:.2%}，短线情绪偏热。",
        "warm": f"涨停 {limit_up_count} 家，炸板率 {break_rate:.2%}，短线情绪正常偏强。",
        "cold": f"涨停 {limit_up_count} 家，炸板率 {break_rate:.2%}，短线情绪偏弱。",
        "normal": f"涨停 {limit_up_count} 家，炸板率 {break_rate:.2%}，短线情绪中性。",
    }[level]


def parse_float(raw: Any) -> float | None:
    if raw is None or raw == "" or raw == "--":
        return None
    try:
        value = float(str(raw).replace("%", "").strip())
    except ValueError:
        return None
    return value if math.isfinite(value) else None


def parse_amount_yi(raw: Any) -> float | None:
    if raw is None or raw == "" or raw == "--":
        return None
    if isinstance(raw, (int, float)):
        return float(raw) / 1e8 if abs(float(raw)) > 1e6 else float(raw)
    text = str(raw).strip()
    match = re.match(r"^([-+]?\d+(?:\.\d+)?)([亿万千]?)$", text)
    if not match:
        return parse_float(text)
    value = float(match.group(1))
    unit = match.group(2)
    if unit == "亿":
        return value
    if unit == "万":
        return value / 10000.0
    if unit == "千":
        return value / 100000.0
    return value / 1e8 if abs(value) > 1e6 else value


def normalize_datetime(raw: Any) -> str:
    value = str(raw or "").strip()
    if not value:
        return f"{TRADE_DATE} 15:30:00"
    value = value.replace("/", "-")
    try:
        return datetime.fromisoformat(value).strftime("%Y-%m-%d %H:%M:%S")
    except ValueError:
        return value


def sql_str(value: Any) -> str:
    return "'" + str(value).replace("'", "''") + "'"


def sql_ts(value: Any) -> str:
    return sql_str(value) + "::timestamptz"


def sql_num(value: Any) -> str:
    parsed = parse_float(value)
    return "null" if parsed is None else str(round(parsed, 6))


def sql_json(value: Any) -> str:
    return sql_str(json.dumps(value, ensure_ascii=False, separators=(",", ":"))) + "::jsonb"


if __name__ == "__main__":
    main()
