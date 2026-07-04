from __future__ import annotations

from datetime import date

import pandas as pd

from cynexus_data.sources.akshare.margin import (
    normalize_margin_account_stats,
    normalize_margin_sse_detail,
    normalize_margin_sse_summary,
    normalize_margin_szse_detail,
    normalize_margin_szse_summary,
)


def test_normalize_margin_summary_records() -> None:
    sse = pd.DataFrame(
        [
            {
                "信用交易日期": "20260704",
                "融资余额": "100",
                "融资买入额": "10",
                "融券余量": "2",
                "融券余量金额": "20",
                "融券卖出量": "3",
                "融资融券余额": "120",
            }
        ]
    )
    szse = pd.DataFrame(
        [
            {
                "融资买入额": "11",
                "融资余额": "101",
                "融券卖出量": "4",
                "融券余量": "5",
                "融券余额": "21",
                "融资融券余额": "122",
            }
        ]
    )

    rows = normalize_margin_sse_summary(sse, fallback_trade_date=date(2026, 7, 4))
    rows += normalize_margin_szse_summary(szse, trade_date=date(2026, 7, 4))

    assert rows[0]["trade_date"] == date(2026, 7, 4)
    assert rows[0]["exchange"] == "SSE"
    assert rows[0]["margin_balance"] == 120.0
    assert rows[1]["exchange"] == "SZSE"
    assert rows[1]["securities_lending_balance_amount"] == 21.0


def test_normalize_margin_detail_records() -> None:
    sse = pd.DataFrame(
        [
            {
                "信用交易日期": "20260704",
                "标的证券代码": "600000",
                "标的证券简称": "浦发银行",
                "融资余额": "100",
                "融资买入额": "10",
                "融资偿还额": "8",
                "融券余量": "2",
                "融券卖出量": "3",
                "融券偿还量": "1",
            }
        ]
    )
    szse = pd.DataFrame(
        [
            {
                "证券代码": 1,
                "证券简称": "平安银行",
                "融资买入额": "11",
                "融资余额": "101",
                "融券卖出量": "4",
                "融券余量": "5",
                "融券余额": "21",
                "融资融券余额": "122",
            }
        ]
    )

    rows = normalize_margin_sse_detail(sse, fallback_trade_date=date(2026, 7, 4))
    rows += normalize_margin_szse_detail(szse, trade_date=date(2026, 7, 4))

    assert rows[0]["stock_code"] == "600000"
    assert rows[0]["financing_repay_amount"] == 8.0
    assert rows[1]["stock_code"] == "000001"
    assert rows[1]["exchange"] == "SZSE"


def test_normalize_margin_account_stats_filters_trade_date() -> None:
    frame = pd.DataFrame(
        [
            {"日期": date(2026, 7, 3), "融资余额": 1, "融券余额": 2},
            {"日期": date(2026, 7, 4), "融资余额": 3, "融券余额": 4, "证券公司数量": 10},
        ]
    )

    rows = normalize_margin_account_stats(frame, trade_date=date(2026, 7, 4))

    assert len(rows) == 1
    assert rows[0]["trade_date"] == date(2026, 7, 4)
    assert rows[0]["broker_count"] == 10
