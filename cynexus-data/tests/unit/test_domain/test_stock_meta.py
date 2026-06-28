from __future__ import annotations

from cynexus_data.domain.stock.stock_meta import (
    board_from_code,
    cap_style_from_market_cap,
    is_st_name,
    limit_up_threshold,
    market_from_code,
    normalize_stock_basic_record,
)


def test_stock_market_board_and_threshold_rules() -> None:
    assert market_from_code("600000") == "SH"
    assert market_from_code("000001") == "SZ"
    assert market_from_code("430001") == "BJ"
    assert board_from_code("688001") == "star"
    assert board_from_code("300001") == "chinext"
    assert limit_up_threshold("300001") == 0.20
    assert limit_up_threshold("430001") == 0.30
    assert limit_up_threshold("600000", "ST测试") == 0.05
    assert is_st_name("*ST测试") is True


def test_normalize_stock_basic_record_keeps_eastmoney_meta() -> None:
    record = normalize_stock_basic_record(
        {
            "code": "600000",
            "name": "浦发银行",
            "industry": "银行",
            "total_market_cap": 400_000_000_000,
            "float_market_cap": 300_000_000_000,
            "source": "eastmoney",
        }
    )

    assert cap_style_from_market_cap(300_000_000_000) == "large"
    assert record["industry"] == "银行"
    assert record["total_market_cap"] == 400_000_000_000
    assert record["float_market_cap"] == 300_000_000_000
    assert record["cap_style"] == "large"
