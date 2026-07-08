from __future__ import annotations

from datetime import datetime
import json
import os
from pathlib import Path
import traceback
from typing import Any, Callable

from cynexus_data.sources.baidu.client import BaiduStockClient
from cynexus_data.sources.cninfo.client import CninfoClient
from cynexus_data.sources.eastmoney import a_stock as em
from cynexus_data.sources.eastmoney.http import EastMoneyHttpClient
from cynexus_data.sources.iwencai.client import IwencaiClient
from cynexus_data.sources.sina.a_stock import SinaAStockClient
from cynexus_data.sources.tdx.mootdx_market import fetch_order_book
from cynexus_data.sources.ths.a_stock import ThsAStockClient


def main() -> None:
    em_client = EastMoneyHttpClient(min_interval=float(os.getenv("A_STOCK_EM_MIN_INTERVAL", "1.0")))
    ths = ThsAStockClient()
    cninfo = CninfoClient()
    sina = SinaAStockClient()
    baidu = BaiduStockClient()

    probes: list[tuple[str, Callable[[], Any]]] = [
        ("eastmoney_reports", lambda: em.reports(em_client, "600519", max_pages=1)),
        ("eastmoney_industry_reports", lambda: em.industry_reports(em_client, max_pages=1, begin="2026-01-01")),
        ("eastmoney_stock_info", lambda: em.stock_info(em_client, "600519")),
        ("eastmoney_concept_blocks", lambda: em.concept_blocks(em_client, "600519")),
        ("eastmoney_fund_flow_minute", lambda: em.fund_flow_minute(em_client, "600519")),
        ("eastmoney_fund_flow_120d", lambda: em.fund_flow_120d(em_client, "600519")),
        ("eastmoney_industry_rank", lambda: em.industry_rank(em_client, top_n=5)),
        ("eastmoney_daily_dragon_tiger", lambda: em.daily_dragon_tiger(em_client, trade_date="2026-06-26")),
        ("eastmoney_dragon_tiger", lambda: em.dragon_tiger_board(em_client, "002475", "2026-06-26")),
        ("eastmoney_lockup", lambda: em.lockup_expiry(em_client, "002475", "2026-06-26")),
        ("eastmoney_margin", lambda: em.margin_trading(em_client, "600519", page_size=5)),
        ("eastmoney_block_trade", lambda: em.block_trade(em_client, "600519", page_size=5)),
        ("eastmoney_holders", lambda: em.holder_num_change(em_client, "600519", page_size=5)),
        ("eastmoney_dividends", lambda: em.dividend_history(em_client, "600519", page_size=5)),
        ("eastmoney_stock_news", lambda: em.stock_news(em_client, "600519", page_size=5)),
        ("eastmoney_global_news", lambda: em.global_news(em_client, page_size=5)),
        ("eastmoney_limit_pool", lambda: em.limit_pool(em_client, "zt", "20260626")),
        ("eastmoney_hot_rank", lambda: em.hot_rank(em_client, top=10)),
        ("eastmoney_hot_concept", lambda: em.hot_concept(em_client, "600519")),
        ("baidu_kline_ma", lambda: baidu.kline_with_ma("600519")),
        ("ths_eps_forecast", lambda: ths.eps_forecast("600519")),
        ("ths_hot_reason", lambda: ths.hot_reason("2026-06-26")),
        ("ths_northbound", lambda: ths.northbound_realtime()),
        ("ths_limit_up_pool", lambda: ths.limit_up_pool("20260626")),
        ("ths_hot_list", lambda: ths.hot_list("hour")),
        ("cninfo_announcements", lambda: cninfo.announcements("600519", page_size=5)),
        ("cninfo_irm", lambda: cninfo.irm("002594", page_size=5)),
        ("sina_financial_report", lambda: sina.financial_report("600519", report_type="lrb", num=2)),
        ("sina_option_codes", lambda: sina.option_codes(underlying="510050", call=True)),
    ]
    if os.getenv("IWENCAI_API_KEY"):
        probes.append(("iwencai_search", lambda: IwencaiClient().search("人形机器人 研报", size=5)))
    else:
        probes.append(("iwencai_search", lambda: _skip("IWENCAI_API_KEY is not set")))
    probes.append(("mootdx_order_book", lambda: fetch_order_book("600519")))

    results = [_run_probe(name, func) for name, func in probes]
    output_dir = Path("var/reports/a-stock-data-smoke") / datetime.now().strftime("%Y%m%d-%H%M%S")
    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "results.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    (output_dir / "summary.md").write_text(_summary(results), encoding="utf-8")
    print(json.dumps({"output": str(output_dir), "ok": sum(1 for r in results if r["status"] == "ok"), "failed": sum(1 for r in results if r["status"] == "failed"), "skipped": sum(1 for r in results if r["status"] == "skipped")}, ensure_ascii=False))


def _run_probe(name: str, func: Callable[[], Any]) -> dict[str, Any]:
    try:
        value = func()
        if isinstance(value, dict) and value.get("status") == "skipped":
            return {"name": name, "status": "skipped", "reason": value.get("reason")}
        count = _count(value)
        return {"name": name, "status": "ok", "count": count, "sample": _sample(value)}
    except Exception as exc:  # noqa: BLE001 - smoke results need exact failure capture.
        return {"name": name, "status": "failed", "error": str(exc), "trace": traceback.format_exc(limit=2)}


def _skip(reason: str) -> dict[str, str]:
    return {"status": "skipped", "reason": reason}


def _count(value: Any) -> int | None:
    if isinstance(value, list):
        return len(value)
    if isinstance(value, dict):
        for key in ("items", "rows", "top", "boards", "stocks"):
            item = value.get(key)
            if isinstance(item, list):
                return len(item)
        return len(value) if value else 0
    return None


def _sample(value: Any) -> Any:
    if isinstance(value, list):
        return value[:2]
    if isinstance(value, dict):
        return {key: value[key] for key in list(value.keys())[:8]}
    return value


def _summary(results: list[dict[str, Any]]) -> str:
    lines = ["# A Stock Data Live Smoke Summary", "", "| Probe | Status | Count | Error/Reason |", "| --- | --- | ---: | --- |"]
    for row in results:
        reason = row.get("error") or row.get("reason") or ""
        lines.append(f"| {row['name']} | {row['status']} | {row.get('count', '')} | {str(reason).replace('|', '/')} |")
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    main()
