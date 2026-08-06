from datetime import datetime, timezone

from cynexus_data.storage.postgres.market_indicator import _snapshot_params


def test_snapshot_id_is_stable_for_same_capture_and_code():
    captured_at = datetime(2026, 7, 28, 13, 17, 15, tzinfo=timezone.utc)
    row = {"code": "000001", "captured_at": captured_at, "last_price": 10.0}

    first = _snapshot_params(row, source="eltdx.quotes")
    second = _snapshot_params(row, source="eltdx.quotes")

    assert first["id"] == second["id"]
    assert first["trade_date"].isoformat() == "2026-07-28"
