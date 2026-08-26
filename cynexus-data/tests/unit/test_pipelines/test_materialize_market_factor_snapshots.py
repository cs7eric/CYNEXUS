from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.a_stock.materialize_market_factor_snapshots import run
from cynexus_data.runtime.context import PipelineContext
import json
import pytest

from cynexus_data.storage.postgres.market_factor_snapshot import _record


def test_factor_snapshot_record_is_versioned_and_point_in_time() -> None:
    record = _record({"market": "SZ", "code": "000001", "trade_date": "2026-08-14", "close_price": 12.3})

    assert record["feature_version"] == "v1"
    assert record["source"] == '{"name":"clickhouse.daily_raw","source_trade_date":"2026-08-14"}'
    assert record["coverage"] == '{"daily_raw":"AVAILABLE"}'
    assert record["available_at"].isoformat() == "2026-08-14T15:00:00+08:00"


def test_factor_snapshot_keeps_raw_candlestick_primitives_without_declaring_a_factor() -> None:
    record = _record({"market": "SZ", "code": "000001", "trade_date": "2026-08-14", "open_price": 10,
                      "high_price": 12, "low_price": 9, "close_price": 11, "volume": 100, "amount": 1100,
                      "return_5d": 8.2, "ma20": 10, "volume_vs_ma20": 1.5})
    payload = json.loads(record["feature_payload"])

    assert payload["stock.candlestick.body_pct"] == 1 / 3
    assert payload["stock.candlestick.upper_shadow_pct"] == 1 / 3
    assert payload["stock.candlestick.lower_shadow_pct"] == 1 / 3
    assert payload["stock.return_5d"] == 8.2
    assert payload["stock.close_vs_ma20"] == pytest.approx(0.1)
    assert payload["stock.volume_vs_ma20"] == 1.5
    assert "factor" not in payload


def test_factor_snapshot_dry_run_has_no_storage_side_effect() -> None:
    result = run(
        PipelineContext.default(),
        JobRequest(job_key="materialize_market_factor_snapshots", dry_run=True, params={"trade_date": "2026-08-14"}),
    )

    assert result["rows_read"] == 0
    assert result["rows_written"] == 0
    assert result["feature_version"] == "v1"


def test_factor_snapshot_pipeline_reconciles_source_coverage(monkeypatch) -> None:
    class FakeClickHouseHook:
        def get_client(self):
            return object()

    class FakePostgresHook:
        def get_client(self):
            return object()

    class FakeHooks:
        clickhouse = FakeClickHouseHook()
        postgres = FakePostgresHook()

    class FakeRepository:
        VERSION = "v1"

        def __init__(self, client) -> None:
            self.client = client

        def replace_range(self, rows, **kwargs):
            return len(rows)

        def count_rows(self, **kwargs):
            return 2

    rows = [
        {"market": "SH", "code": "600000", "trade_date": "2026-08-14", "close_price": 10.0},
        {"market": "SZ", "code": "000001", "trade_date": "2026-08-14", "close_price": 12.0},
    ]
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_market_factor_snapshots.read_stock_screening_fact_rows",
        lambda *args, **kwargs: rows,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_market_factor_snapshots.count_stock_screening_fact_source_rows",
        lambda *args, **kwargs: 2,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_market_factor_snapshots.MarketFactorSnapshotRepository",
        FakeRepository,
    )

    context = PipelineContext(hooks=FakeHooks())
    result = run(
        context,
        JobRequest(job_key="materialize_market_factor_snapshots", params={"trade_date": "2026-08-14"}),
    )

    assert result.get("success", True)
    assert result["watermark"]["coverage_pct"] == 100.0
    assert context.checkpoints.get("a_stock.market_factor_snapshot.watermark")["materialized_daily_rows"] == 2


def test_factor_snapshot_pipeline_passes_a_bounded_upsert_batch_size(monkeypatch) -> None:
    class FakeClickHouseHook:
        def get_client(self):
            return object()

    class FakePostgresHook:
        def get_client(self):
            return object()

    class FakeHooks:
        clickhouse = FakeClickHouseHook()
        postgres = FakePostgresHook()

    captured: dict[str, object] = {}

    class FakeRepository:
        VERSION = "v1"

        def __init__(self, client) -> None:
            pass

        def replace_range(self, rows, **kwargs):
            captured.update(kwargs)
            return len(rows)

        def count_rows(self, **kwargs):
            return 1

    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_market_factor_snapshots.read_stock_screening_fact_rows",
        lambda *args, **kwargs: [{"market": "SH", "code": "600000", "trade_date": "2026-08-14", "close_price": 10.0}],
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_market_factor_snapshots.count_stock_screening_fact_source_rows",
        lambda *args, **kwargs: 1,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_market_factor_snapshots.MarketFactorSnapshotRepository",
        FakeRepository,
    )

    run(PipelineContext(hooks=FakeHooks()), JobRequest(
        job_key="materialize_market_factor_snapshots", params={"trade_date": "2026-08-14", "batch_size": 321},
    ))

    assert captured["batch_size"] == 321


def test_factor_snapshot_pipeline_fails_on_coverage_gap(monkeypatch) -> None:
    class FakeClickHouseHook:
        def get_client(self):
            return object()

    class FakePostgresHook:
        def get_client(self):
            return object()

    class FakeHooks:
        clickhouse = FakeClickHouseHook()
        postgres = FakePostgresHook()

    class FakeRepository:
        VERSION = "v1"

        def __init__(self, client) -> None:
            pass

        def replace_range(self, rows, **kwargs):
            return len(rows)

        def count_rows(self, **kwargs):
            return 1

    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_market_factor_snapshots.read_stock_screening_fact_rows",
        lambda *args, **kwargs: [{"market": "SH", "code": "600000", "trade_date": "2026-08-14", "close_price": 10.0}],
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_market_factor_snapshots.count_stock_screening_fact_source_rows",
        lambda *args, **kwargs: 2,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_market_factor_snapshots.MarketFactorSnapshotRepository",
        FakeRepository,
    )

    result = run(PipelineContext(hooks=FakeHooks()), JobRequest(
        job_key="materialize_market_factor_snapshots", params={"trade_date": "2026-08-14"},
    ))

    assert result["success"] is False
    assert result["watermark"]["coverage_pct"] == 50.0
