from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.market_heatmap import build_industry_heatmap_payload


def test_build_industry_heatmap_payload_sorts_by_amount_and_limits() -> None:
    payload = build_industry_heatmap_payload(
        [
            {"code6": "880471", "name": "银行", "amount": 100, "change_pct": -0.2},
            {"code6": "880493", "name": "软件服务", "amount": 300, "change_pct": 2.1},
        ],
        snapshot_date=date(2026, 6, 26),
        top_n=1,
    )

    assert payload["kind"] == "industries"
    assert payload["itemsTotal"] == 2
    assert payload["items"][0]["name"] == "软件服务"
    assert payload["items"][0]["sectorCode"] == "sh880493"
    assert payload["items"][0]["risingCount"] == 1


def test_build_industry_heatmap_payload_skips_incomplete_rows() -> None:
    payload = build_industry_heatmap_payload(
        [{"name": ""}, {"code6": "880493", "name": "软件服务", "amount": 1}],
        snapshot_date=date(2026, 6, 26),
    )

    assert payload["totalItems"] == 1
    assert payload["items"][0]["kindLabel"] == "行业 (通达信)"
