from __future__ import annotations

from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.a_stock.materialize_market_factor_membership_snapshots import run
from cynexus_data.runtime.context import PipelineContext


def test_membership_materializer_dry_run_is_side_effect_free() -> None:
    result = run(PipelineContext.default(), JobRequest(
        job_key="materialize_market_factor_membership_snapshots", dry_run=True, params={"trade_date": "2026-08-14"},
    ))

    assert result["rows_written"] == 0
    assert result["membership"] == "ths"


def test_membership_materializer_uses_only_as_of_source_records(monkeypatch) -> None:
    class FakePostgresHook:
        def get_client(self):
            return object()

    class FakeHooks:
        postgres = FakePostgresHook()

    class FakeSourceRepository:
        def __init__(self, client) -> None:
            pass

        def list_records_as_of(self, *, snapshot_date: date):
            assert snapshot_date == date(2026, 8, 14)
            return [{"source_snapshot_date": date(2026, 8, 13), "industry_code": "881001", "stock_code": "600000"}]

    class FakeTargetRepository:
        def __init__(self, client) -> None:
            pass

        def replace_effective_date(self, *, effective_trade_date: date, records):
            assert effective_trade_date == date(2026, 8, 14)
            assert records[0]["source_snapshot_date"] == date(2026, 8, 13)
            return len(records)

    module = "cynexus_data.pipelines.a_stock.materialize_market_factor_membership_snapshots"
    monkeypatch.setattr(module + ".ThsIndustryConstituentRepository", FakeSourceRepository)
    monkeypatch.setattr(module + ".MarketFactorMembershipSnapshotRepository", FakeTargetRepository)

    context = PipelineContext(hooks=FakeHooks())
    result = run(context, JobRequest(job_key="materialize_market_factor_membership_snapshots", params={"trade_date": "2026-08-14"}))

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert context.checkpoints.get("a_stock.market_factor_membership.watermark")["coverage_pct"] == 100.0
