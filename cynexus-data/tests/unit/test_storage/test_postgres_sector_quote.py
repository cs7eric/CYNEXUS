from __future__ import annotations

from datetime import date

from cynexus_data.storage.postgres.sector_quote import TdxSectorQuoteDetailRepository


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
    def __init__(self):
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params):
        self.calls.append((str(statement), params))
        return FakeExecuteResult()


class FakeClient:
    schema = "cynexus_data"

    def __init__(self):
        self.session_obj = FakeSession()

    def session(self):
        return self.session_obj


def test_replace_snapshots_soft_deletes_requested_codes_and_upserts() -> None:
    client = FakeClient()
    repo = TdxSectorQuoteDetailRepository(client)
    count = repo.replace_snapshots(
        snapshot_date=date(2026, 6, 26),
        rows=[
            {
                "code6": "880471",
                "name": "银行",
                "seed_code": "sh600000",
                "payload": {"sections": {"period_stats": []}},
                "section_counts": {"period_stats": 0},
            }
        ],
    )

    sqls = [call[0] for call in client.session_obj.calls]
    assert count == 1
    assert "UPDATE cynexus_data.tdx_sector_quote_detail_snapshot" in sqls[0]
    assert "INSERT INTO cynexus_data.tdx_sector_quote_detail_snapshot" in sqls[1]
    assert client.session_obj.calls[1][1]["code6"] == "880471"
