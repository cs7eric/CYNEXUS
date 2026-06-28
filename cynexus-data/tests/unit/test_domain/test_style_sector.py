from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.style_sector import build_style_sector_snapshots


def test_build_style_sector_snapshots_uses_equal_weight_change_pct() -> None:
    sectors = [
        {"name": "昨日涨停", "stock_codes": ["000001", "000002", "000003"]},
        {"name": "不在白名单", "stock_codes": ["000001"]},
    ]
    quotes = {
        "000001": {"change_pct": 10},
        "000002": {"last_price": 11, "pre_close": 10},
        "000003": {"change_pct": None},
    }

    rows = build_style_sector_snapshots(sectors, quotes, trade_date=date(2026, 6, 26))

    assert len(rows) == 1
    assert rows[0]["name"] == "昨日涨停"
    assert rows[0]["change_pct"] == 10
    assert rows[0]["valid_size"] == 2
    assert rows[0]["sample_size"] == 3
