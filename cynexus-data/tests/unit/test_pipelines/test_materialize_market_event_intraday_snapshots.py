from datetime import datetime, timedelta, timezone

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.a_stock.materialize_market_event_intraday_snapshots import run
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.market_event_intraday_snapshot import _record


def test_event_intraday_materializer_dry_run_is_side_effect_free() -> None:
    result = run(PipelineContext.default(), JobRequest(
        job_key="materialize_market_event_intraday_snapshots", dry_run=True,
        params={"code": "600000", "trade_date": "2026-08-14"},
    ))
    assert result["rows_written"] == 0
    assert result["code"] == "600000"


def test_intraday_snapshot_availability_matches_the_observed_minute() -> None:
    minute = datetime(2026, 8, 14, 10, 0, tzinfo=timezone(timedelta(hours=8)))
    record = _record("600000", "SH", "2026-08-14", minute, {"minute_at": minute, "primitive_code": "stock.close", "value": 10})
    assert record["available_at"] == minute


def test_event_intraday_materializer_persists_only_bounded_stock_primitives(monkeypatch) -> None:
    class FakeClickHouseHook:
        def get_client(self):
            return object()

    class FakePostgresHook:
        def get_client(self):
            return object()

    class FakeHooks:
        clickhouse = FakeClickHouseHook()
        postgres = FakePostgresHook()

    anchor = datetime(2026, 8, 14, 10, 0, tzinfo=timezone(timedelta(hours=8)))

    def fake_read(client, **kwargs):
        assert kwargs["code"] == "600000"
        assert kwargs["start_at"] == anchor - timedelta(minutes=120)
        assert kwargs["end_at"] == anchor + timedelta(minutes=30)
        return [{"minute_at": datetime(2026, 8, 14, 10, 0), "close": 10.2, "volume": 100.0, "amount": 1020.0}]

    captured = {}

    class FakeTarget:
        def __init__(self, client):
            pass

        def replace_event(self, **kwargs):
            captured.update(kwargs)
            return len(kwargs["records"])

    module = "cynexus_data.pipelines.a_stock.materialize_market_event_intraday_snapshots"
    monkeypatch.setattr(module + ".read_event_minute_bars", fake_read)
    monkeypatch.setattr(module + ".MarketEventIntradaySnapshotRepository", FakeTarget)
    context = PipelineContext(hooks=FakeHooks())
    result = run(context, JobRequest(job_key="materialize_market_event_intraday_snapshots", params={
        "code": "600000", "trade_date": "2026-08-14", "anchor_at": anchor.isoformat(),
    }))

    assert result["rows_read"] == 3
    assert result["rows_written"] == 3
    assert {row["primitive_code"] for row in captured["records"]} == {"stock.close", "stock.volume", "stock.amount"}
    assert all(row["minute_at"].tzinfo is not None for row in captured["records"])


def test_event_intraday_materializer_reports_missing_source_explicitly(monkeypatch) -> None:
    class FakeClickHouseHook:
        def get_client(self):
            return object()

    class FakeHooks:
        clickhouse = FakeClickHouseHook()
        postgres = None

    module = "cynexus_data.pipelines.a_stock.materialize_market_event_intraday_snapshots"
    monkeypatch.setattr(module + ".read_event_minute_bars", lambda *args, **kwargs: [])
    result = run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="materialize_market_event_intraday_snapshots", params={
        "code": "600000", "trade_date": "2026-08-14",
    }))

    assert result["success"] is False
    assert result["warnings"] == ["STOCK_1M_UNAVAILABLE"]
