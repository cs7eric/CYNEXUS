from __future__ import annotations

from cynexus_data.domain.market.index_returns import calculate_index_return_item, calculate_index_returns, normalize_index_code


def test_calculate_index_return_item_builds_daily_and_cumulative_return() -> None:
    target = {"name": "沪深300", "code": "000300", "fullCode": "sh000300"}
    bars = [
        {"trade_date": "2026-06-24", "close": 100},
        {"trade_date": "2026-06-25", "close": 103},
        {"trade_date": "2026-06-26", "close": 106},
    ]

    item = calculate_index_return_item(target, bars, window_days=3)

    assert item["available"] is True
    assert item["returnPct"] == 6.0
    assert item["daily"][1]["dailyReturnPct"] == 3.0
    assert item["daily"][2]["dailyReturnPct"] == 2.9126


def test_calculate_index_returns_handles_missing_rows() -> None:
    rows = {"sh000001": [{"trade_date": "2026-06-26", "close": 10}]}
    targets = [{"name": "上证指数", "code": "000001", "fullCode": "sh000001"}, {"name": "沪深300", "code": "000300", "fullCode": "sh000300"}]

    items = calculate_index_returns(rows, window_days=5, targets=targets)

    assert items[0]["available"] is True
    assert items[1]["available"] is False


def test_normalize_index_code_defaults_to_sh_prefix() -> None:
    assert normalize_index_code("000300") == "sh000300"
    assert normalize_index_code("sz399001") == "sz399001"
