from __future__ import annotations

from datetime import date

from cynexus_data.storage.postgres.market_overview import MarketOverviewRepository


class FakeResult:
    def __init__(self, first_row=None, rows=None):
        self.first_row = first_row
        self.rows = rows or []

    def first(self):
        return self.first_row

    def all(self):
        return self.rows


class FakeMappings:
    def __init__(self, result):
        self.result = result

    def first(self):
        return self.result.first_row

    def all(self):
        return self.result.rows


class FakeExecuteResult:
    def __init__(self, result):
        self.result = result

    def mappings(self):
        return FakeMappings(self.result)


class FakeSession:
    def __init__(self, result):
        self.result = result
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params=None):
        self.calls.append((str(statement), params))
        return FakeExecuteResult(self.result)


class FakeClient:
    schema = "cynexus_appl_market"

    def __init__(self, result):
        self.session_obj = FakeSession(result)

    def session(self):
        return self.session_obj


def test_list_snapshots_queries_market_overview_snapshot() -> None:
    result = FakeResult(rows=[{"trade_date": date(2026, 6, 25)}, {"trade_date": date(2026, 6, 26)}])
    client = FakeClient(result)
    rows = MarketOverviewRepository(client).list_snapshots(end_date=date(2026, 6, 26), limit=2)

    sql, params = client.session_obj.calls[0]
    assert len(rows) == 2
    assert "cynexus_appl_market.market_overview_snapshots" in sql
    assert params["limit"] == 2


def test_upsert_detail_writes_json_payload_to_detail_table() -> None:
    result = FakeResult(first_row=None)
    client = FakeClient(result)
    repo = MarketOverviewRepository(client)
    row = repo.upsert_detail(
        trade_date=date(2026, 6, 26),
        benchmark_code="999999",
        source_table="daily_raw",
        payload={"regimeBucket": "trend_up"},
        source="test",
    )

    sql, params = client.session_obj.calls[0]
    assert "cynexus_appl_market.mkt_overview_detail_snapshot" in sql
    assert "ON CONFLICT (trade_date, benchmark_code, source_table)" in sql
    assert params["benchmark_code"] == "999999"
    assert params["source_table"] == "daily_raw"
    assert '"regimeBucket": "trend_up"' in params["payload"]
    assert row["payload"]["regimeBucket"] == "trend_up"


def test_record_source_failure_uses_latest_successful_flow_value() -> None:
    class FailureSession:
        def __init__(self):
            self.calls = []

        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

        def execute(self, statement, params=None):
            sql = str(statement)
            self.calls.append((sql, params))
            if "main_net_inflow IS NOT NULL" in sql:
                return FakeExecuteResult(FakeResult(first_row={"trade_date": date(2026, 7, 8), "extra": {}}))
            return FakeExecuteResult(FakeResult(first_row=None))

    class FailureClient:
        schema = "cynexus_appl_market"

        def __init__(self):
            self.session_obj = FailureSession()

        def session(self):
            return self.session_obj

    client = FailureClient()
    last_successful = MarketOverviewRepository(client).record_source_failure(
        source="eastmoney.market_fund_flow",
        error="upstream unavailable",
        attempted_at="2026-07-16T09:00:00+00:00",
    )

    assert last_successful == "2026-07-08"
    update_sql, update_params = client.session_obj.calls[-1]
    assert "SET extra = CAST(:extra AS jsonb)" in update_sql
    assert '"last_successful_trade_date": "2026-07-08"' in update_params["extra"]
