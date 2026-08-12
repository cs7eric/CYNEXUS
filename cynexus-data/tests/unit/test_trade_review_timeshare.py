from datetime import date

from cynexus_data.core.market_symbol import normalize_market_symbol
from cynexus_data.domain.market.trade_review_timeshare import (
    enrich_with_minute_amount,
    normalize_timeshare_rows,
    select_top_bottom_industry_targets,
)


def test_normalize_timeshare_uses_fixed_session_slots_and_drops_lunch():
    rows = normalize_timeshare_rows(
        [
            {"datetime": "2026-08-07 09:30:00", "price": 10, "volume": 2},
            {"datetime": "2026-08-07 11:30:00", "price": 11, "volume": 3},
            {"datetime": "2026-08-07 13:00:00", "price": 12, "volume": 4},
        ],
        trade_date=date(2026, 8, 7), symbol="600519", market="SH", source="easy_tdx",
    )
    assert [row["slot"] for row in rows] == [0, 120]
    assert rows[1]["time_label"] == "13:00"


def test_enrich_amount_and_vwap_from_aligned_minute_kline():
    rows = normalize_timeshare_rows(
        [{"datetime": "2026-08-07 09:30:00", "price": 10, "volume": 100}],
        trade_date=date(2026, 8, 7), symbol="600519", market="SH", source="easy_tdx",
    )
    enriched = enrich_with_minute_amount(rows, {0: {"volume": 10_000, "amount": 100_000}})
    assert enriched[0]["turnover"] == 100_000
    assert enriched[0]["avg_price"] == 10


def test_industry_selection_excludes_limits_and_keeps_gainers_and_decliners():
    rows = [
        {"industry_code": "A", "stock_code": f"600{index:03d}", "change_pct": value}
        for index, value in enumerate([-12, -8, -3, 2, 8, 12])
    ]
    selected = select_top_bottom_industry_targets(rows, limit_symbols={"600000"}, top_n=2, bottom_n=2)
    assert [item[0] for item in selected] == ["600001", "600002", "600005", "600004"]


def test_new_beijing_920_series_uses_bj_market():
    assert normalize_market_symbol("920403") == "bj920403"

