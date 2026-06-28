from __future__ import annotations

from datetime import date

from cynexus_data.domain.market.sector_quote import build_sector_quote_detail_snapshot


def test_build_sector_quote_detail_snapshot_wraps_sections() -> None:
    snapshot = build_sector_quote_detail_snapshot(
        {
            "code6": "880471",
            "name": "银行",
            "seed_code": "sh600000",
            "sections": {"period_stats": [{"range_code": "0"}], "constituents": [{"code": "600000"}]},
        },
        snapshot_date=date(2026, 6, 26),
    )

    assert snapshot is not None
    assert snapshot["code6"] == "880471"
    assert snapshot["payload"]["fullCode"] == "sh880471"
    assert snapshot["section_counts"]["period_stats"] == 1
    assert snapshot["section_counts"]["constituents"] == 1


def test_build_sector_quote_detail_snapshot_skips_missing_identity() -> None:
    assert build_sector_quote_detail_snapshot({"name": "银行"}, snapshot_date=date(2026, 6, 26)) is None
