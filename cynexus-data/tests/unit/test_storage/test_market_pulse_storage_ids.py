from datetime import date, datetime, timezone

from cynexus_data.storage.postgres.ids import stable_bigint_id
from cynexus_data.storage.postgres.market_pulse import SNAPSHOT_TABLE, _row_params


def test_market_pulse_snapshot_id_is_stable_for_retry() -> None:
    row = {"trade_date": date(2026, 7, 27), "sector_name": "医疗服务", "extra": {"source": "test"}}
    captured_at = datetime(2026, 7, 27, tzinfo=timezone.utc)

    first = _row_params(row, batch_id=1, source_kind="live_capture", source_name="test", captured_at=captured_at)
    second = _row_params(row, batch_id=2, source_kind="live_capture", source_name="test", captured_at=captured_at)

    assert first["id"] == second["id"]
    assert first["id"] == stable_bigint_id(SNAPSHOT_TABLE, row["trade_date"], row["sector_name"])


def test_market_pulse_snapshot_upsert_revives_soft_deleted_row() -> None:
    class FakeResult:
        def scalar_one(self):
            return 1001

    class FakeSession:
        def __init__(self):
            self.calls = []

        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

        def execute(self, statement, params=None):
            self.calls.append((str(statement), params))
            return FakeResult()

    class FakeClient:
        schema = "cynexus_appl_market"

        def __init__(self):
            self.session_obj = FakeSession()

        def session(self):
            return self.session_obj

    from cynexus_data.storage.postgres.market_pulse import MarketPulseRepository

    client = FakeClient()
    MarketPulseRepository(client).replace_snapshot(
        trade_date=date(2026, 7, 27),
        rows=[{"trade_date": date(2026, 7, 27), "sector_name": "医疗服务"}],
    )

    batch_sql = client.session_obj.calls[3][0]
    snapshot_sql = client.session_obj.calls[4][0]
    assert "ON CONFLICT (id) DO UPDATE SET" in batch_sql
    assert "deleted_at = NULL" in batch_sql
    assert "ON CONFLICT (id) DO UPDATE SET" in snapshot_sql
    assert "deleted_at = NULL" in snapshot_sql
