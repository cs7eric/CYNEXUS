from __future__ import annotations

from datetime import date

from cynexus_data.storage.postgres.tdx_industry_quotes import TdxIndustryQuoteRepository


class FakeExecuteResult:
    def __init__(self, rows=None):
        self.rows = rows or []

    def all(self):
        return self.rows

    def scalar_one(self):
        return 1001


class FakeSession:
    def __init__(self, rows=None):
        self.rows = rows or []
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params):
        self.calls.append((str(statement), params))
        return FakeExecuteResult(self.rows)


class FakeClient:
    schema = "cynexus_appl_market"

    def __init__(self, rows=None):
        self.session_obj = FakeSession(rows)

    def session(self):
        return self.session_obj


def test_replace_snapshot_soft_deletes_and_upserts_rows() -> None:
    client = FakeClient()
    repo = TdxIndustryQuoteRepository(client)
    result = repo.replace_snapshot(
        date(2026, 6, 26),
        [{"code6": "880471", "name": "银行", "full_code": "sh880471", "exchange": "sh", "change_pct": 1.2}],
        batch_id="batch-1",
    )

    sqls = [call[0] for call in client.session_obj.calls]
    assert result["rows_written"] == 1
    assert "UPDATE cynexus_appl_market.tdx_industry_quote_snapshot" in sqls[0]
    assert "INSERT INTO cynexus_appl_market.tdx_industry_quote_batch" in sqls[1]
    assert "INSERT INTO cynexus_appl_market.tdx_industry_quote_snapshot" in sqls[2]
    assert client.session_obj.calls[2][1]["code6"] == "880471"


def test_get_snapshot_maps_rows() -> None:
    client = FakeClient(rows=[("880471", "银行", "sh880471", "sh", 10, 9, 9.5, 10.2, 9.1, 1, 1.2, 1000, 100, 10, "eltdx")])
    rows = TdxIndustryQuoteRepository(client).get_snapshot(date(2026, 6, 26))

    assert rows[0]["code6"] == "880471"
    assert rows[0]["change_pct"] == 1.2
    assert rows[0]["source"] == "eltdx"
