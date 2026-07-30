from datetime import date
from decimal import Decimal

from cynexus_data.domain.futures.position import normalize_position_rows


def test_normalize_position_rows_keeps_three_independent_rankings() -> None:
    rows = normalize_position_rows([
        {
            "symbol": "IF2609",
            "variety": "IF",
            "rank": 1,
            "vol_party_name": "成交会员",
            "vol": "1,000",
            "vol_chg": "10",
            "long_party_name": "多头会员",
            "long_open_interest": 200,
            "long_open_interest_chg": 2,
            "short_party_name": "空头会员",
            "short_open_interest": 180,
            "short_open_interest_chg": -3,
        },
        {"symbol": "IF2609", "variety": "IF", "rank": 999, "vol": 1, "long_open_interest": 1, "short_open_interest": 1},
    ], trade_date=date(2026, 7, 30), exchange_code="CFFEX", selected_source="akshare.cffex")

    assert [(row.rank_type, row.member_name, row.rank_value) for row in rows] == [
        ("VOLUME", "成交会员", Decimal("1000")),
        ("LONG", "多头会员", Decimal("200")),
        ("SHORT", "空头会员", Decimal("180")),
    ]
    assert all(row.contract_code == "IF2609" for row in rows)


def test_normalize_position_rows_supports_variety_scope_without_contract_identity() -> None:
    rows = normalize_position_rows([
        {"symbol": "M2609", "variety": "M", "rank": 1, "vol": 100, "long_open_interest": 90, "short_open_interest": 110},
    ], trade_date=date(2026, 7, 30), exchange_code="DCE", selected_source="official.dce", scope_type="VARIETY")

    assert rows[0].instrument_code == "M"
    assert rows[0].contract_code is None
