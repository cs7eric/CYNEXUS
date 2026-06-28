from __future__ import annotations

from datetime import date

from cynexus_data.storage.postgres.style_sector import StyleSectorRepository


class FakeSession:
    def __init__(self):
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params):
        self.calls.append((str(statement), params))


class FakeClient:
    schema = "cynexus_data"

    def __init__(self):
        self.session_obj = FakeSession()

    def session(self):
        return self.session_obj


def test_replace_snapshots_soft_deletes_and_upserts_rows() -> None:
    client = FakeClient()
    repo = StyleSectorRepository(client)

    count = repo.replace_snapshots(trade_date=date(2026, 6, 26), rows=[{"name": "昨日涨停", "change_pct": 3.0, "valid_size": 2, "sample_size": 3}])

    assert count == 1
    delete_sql, delete_params = client.session_obj.calls[0]
    insert_sql, insert_params = client.session_obj.calls[1]
    assert "UPDATE cynexus_data.market_style_sector_snapshot" in delete_sql
    assert "INSERT INTO cynexus_data.market_style_sector_snapshot" in insert_sql
    assert "ON CONFLICT (trade_date, name, source)" in insert_sql
    assert delete_params["source"] == "tencent.qt"
    assert insert_params["name"] == "昨日涨停"
