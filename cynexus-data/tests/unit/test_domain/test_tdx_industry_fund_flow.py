from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.tdx_industry_fund_flow import (
    build_tdx_industry_fund_flow_snapshot,
    select_fund_flow_history_item,
)


def test_select_history_item_prefers_exact_trade_date() -> None:
    history = [
        {"trade_date": date(2026, 7, 1), "main_net": 1.0},
        {"trade_date": date(2026, 7, 3), "main_net": 3.0},
    ]

    selected, mode = select_fund_flow_history_item(history, date(2026, 7, 3))

    assert mode == "exact"
    assert selected["main_net"] == 3.0


def test_build_snapshot_marks_name_mismatch_and_previous_fallback() -> None:
    snapshot = build_tdx_industry_fund_flow_snapshot(
        {
            "code6": "880471",
            "name": "银行",
            "seed_code": "sh601398",
            "sections": {
                "main_capital_flow": [
                    {
                        "sector_name": "全国性银行",
                        "date": "20260702",
                        "main_net": 12.5,
                        "large_order_net": 10.0,
                        "medium_order_net": 2.0,
                        "small_order_net": 0.5,
                    }
                ]
            },
        },
        snapshot_date=date(2026, 7, 3),
    )

    assert snapshot["selection_mode"] == "fallback_previous"
    assert snapshot["name_match_type"] == "mismatch"
    assert snapshot["flow_sector_name"] == "全国性银行"
    assert snapshot["main_net"] == 12.5
