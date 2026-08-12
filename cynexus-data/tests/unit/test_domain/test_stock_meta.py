from __future__ import annotations

from cynexus_data.domain.stock.stock_meta import (
    board_from_code,
    cap_style_from_market_cap,
    effective_limit_up_threshold,
    is_st_name,
    limit_up_threshold,
    market_from_code,
    normalize_stock_basic_record,
)


def test_stock_market_board_and_threshold_rules() -> None:
    assert market_from_code("600000") == "SH"
    assert market_from_code("000001") == "SZ"
    assert market_from_code("430001") == "BJ"
    assert market_from_code("920001") == "BJ"
    assert board_from_code("688001") == "star"
    assert board_from_code("300001") == "chinext"
    assert limit_up_threshold("300001") == 0.20
    assert limit_up_threshold("300001", "ST测试") == 0.20
    assert limit_up_threshold("688001", "*ST测试") == 0.20
    assert limit_up_threshold("430001") == 0.30
    assert limit_up_threshold("600000", "ST测试") == 0.10
    assert limit_up_threshold("000001", "ST测试") == 0.10
    assert is_st_name("*ST测试") is True


def test_effective_threshold_ignores_stale_st_metadata_on_non_main_boards() -> None:
    assert effective_limit_up_threshold("300001", {"is_st": True, "limit_up_threshold": 0.05}) == 0.20
    assert effective_limit_up_threshold("688001", {"name": "ST测试", "limit_up_threshold": 0.05}) == 0.20
    assert effective_limit_up_threshold("430001", {"is_st": True, "limit_up_threshold": 0.05}) == 0.30
    assert effective_limit_up_threshold("600000", {"is_st": True, "limit_up_threshold": 0.05}) == 0.10


def test_effective_threshold_keeps_positive_explicit_non_st_metadata() -> None:
    assert effective_limit_up_threshold("600000", {"is_st": False, "limit_up_threshold": 0.15}) == 0.15


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
