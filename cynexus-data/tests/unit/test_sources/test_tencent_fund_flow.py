from __future__ import annotations

from cynexus_data.sources.tencent.parser import parse_qt_stock_fund_flow_text


def test_parse_qt_stock_fund_flow_text() -> None:
    parts = [""] * 50
    parts[1] = "浦发银行"
    parts[2] = "600000"
    parts[3] = "10.00"
    parts[4] = "9.80"
    parts[5] = "9.90"
    parts[6] = "1000"
    parts[7] = "600"
    parts[8] = "400"
    parts[31] = "0.20"
    parts[32] = "2.04"
    parts[33] = "10.10"
    parts[34] = "9.70"
    parts[37] = "1000"
    parts[38] = "1.2"
    parts[43] = "4.1"
    parts[44] = "2000"
    parts[45] = "3000"
    parts[46] = "0.8"
    body = "\n".join(
        [
            f'v_sh600000="{"~".join(parts)}";',
            'v_s_pksh600000="0.12~0.38~0.20~0.30";',
        ]
    )

    parsed = parse_qt_stock_fund_flow_text(body)

    row = parsed["sh600000"]
    assert row["code"] == "600000"
    assert row["name"] == "浦发银行"
    assert row["active_net_lots"] == 200.0
    assert row["active_net_amount_wan"] == 0.2
    assert row["active_buy_ratio"] == 0.6
    assert row["disk"]["buy_big_ratio"] == 0.12
    assert row["disk"]["sell_small_ratio"] == 0.30
    assert row["pb"] == 0.8
