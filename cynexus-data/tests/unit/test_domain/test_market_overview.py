from __future__ import annotations

from cynexus_data.domain.market.market_overview import merge_source, normalize_market_overview_fields, should_overwrite_field


def test_normalize_market_overview_fields_aliases() -> None:
    fields = normalize_market_overview_fields(
        {"tradingDate": "20260626", "totalAmount": "1234.5", "risingCount": "3000", "mainNetInflow": "-12.3"}
    )

    assert fields["trade_date"].isoformat() == "2026-06-26"
    assert fields["total_amount"] == 1234.5
    assert fields["rising_count"] == 3000
    assert fields["main_net_inflow"] == -12.3


def test_market_overview_source_rules() -> None:
    assert should_overwrite_field(source="eltdx", field="total_amount", current_value=1)
    assert not should_overwrite_field(source="eltdx", field="main_net_inflow", current_value=1)
    assert should_overwrite_field(source="manual", field="main_net_inflow", current_value=1)
    assert merge_source("akshare+eltdx", "manual") == "akshare+eltdx+manual"
