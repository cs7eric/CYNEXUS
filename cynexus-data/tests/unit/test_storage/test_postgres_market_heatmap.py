from __future__ import annotations

from datetime import date

from cynexus_data.storage.postgres.market_heatmap import MarketHeatmapRepository


class FakeMappings:
    def __init__(self, row=None):
        self.row = row

    def first(self):
        return self.row


class FakeExecuteResult:
    def __init__(self, row=None):
        self.row = row

    def mappings(self):
        return FakeMappings(self.row)


class FakeSession:
    def __init__(self, row=None):
        self.row = row
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params):
        self.calls.append((str(statement), params))
        return FakeExecuteResult(self.row)


class FakeClient:
    schema = "cynexus_appl_market"

    def __init__(self, row=None):
        self.session_obj = FakeSession(row)

    def session(self):
        return self.session_obj


def test_upsert_snapshot_writes_json_payload() -> None:
    client = FakeClient()
    repo = MarketHeatmapRepository(client)
    row = repo.upsert_snapshot(
        snapshot_date=date(2026, 6, 26),
        kind="industries",
        source="test",
        payload={"items": [{"name": "软件服务"}]},
    )

    sql, params = client.session_obj.calls[0]
    assert "cynexus_appl_market.mkt_heatmap_snapshot" in sql
    assert "ON CONFLICT (snapshot_date, kind, source)" in sql
    assert params["item_count"] == 1
    assert '"name": "软件服务"' in params["payload"]
    assert row["item_count"] == 1
