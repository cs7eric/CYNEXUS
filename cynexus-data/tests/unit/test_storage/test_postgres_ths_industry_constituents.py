from __future__ import annotations

from cynexus_data.storage.postgres import ths_industry_constituents


class FakeExecuteResult:
    def __init__(self, inserted_id):
        self.inserted_id = inserted_id

    def scalar_one_or_none(self):
        return self.inserted_id


class FakeSession:
    def __init__(self):
        self.calls = []

    def execute(self, statement, params):
        self.calls.append((statement, params))
        return FakeExecuteResult(None if len(self.calls) == 1 else params["id"])


def test_generated_id_insert_retries_primary_key_collision(monkeypatch) -> None:
    ids = iter([100, 101])
    monkeypatch.setattr(ths_industry_constituents, "next_bigint_id", lambda: next(ids))
    db = FakeSession()

    inserted_id = ths_industry_constituents._execute_generated_id_insert(
        db,
        "insert statement",
        {"stock_code": "300825"},
        label="ths_industry_constituent_snapshot",
    )

    assert inserted_id == 101
    assert [call[1]["id"] for call in db.calls] == [100, 101]
