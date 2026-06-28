from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.market_pulse import build_market_pulse_rows, build_strong_and_flow_payload


def test_build_market_pulse_rows_sorts_by_change_pct() -> None:
    rows = [
        {"sector_name": "银行", "change_pct": "-0.2", "net": "-3"},
        {"sector_name": "软件服务", "change_pct": "1.2%", "net": "6"},
    ]

    out = build_market_pulse_rows(rows, trade_date=date(2026, 6, 26))

    assert [row["sector_name"] for row in out] == ["软件服务", "银行"]
    assert out[0]["rank_by_change"] == 1
    assert out[0]["main_net"] == 6.0


def test_build_strong_and_flow_payload() -> None:
    rows = [
        {"sector_name": "A", "change_pct": 2, "net": 10},
        {"sector_name": "B", "change_pct": 1, "net": -4},
        {"sector_name": "C", "change_pct": -1, "net": 3},
    ]

    payload = build_strong_and_flow_payload(rows, trade_date=date(2026, 6, 26), top_n=2, flow_top_n=2)

    assert [row["sector_name"] for row in payload["strong"]["top"]] == ["A", "B"]
    assert [row["sector_name"] for row in payload["strong"]["bottom"]] == ["C", "B"]
    assert [row["sector_name"] for row in payload["flow"]["inflow"]] == ["A", "C"]
    assert [row["sector_name"] for row in payload["flow"]["outflow"]] == ["B"]
