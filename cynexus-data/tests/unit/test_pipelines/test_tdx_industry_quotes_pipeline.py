from __future__ import annotations

import json
from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market import sync_tdx_industry_quotes
from cynexus_data.runtime.context import PipelineContext


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    postgres = FakePostgresHook()


class FakeRepo:
    created = 0
    calls = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeRepo.created += 1

    def replace_snapshot(self, snapshot_date, rows, *, source="eltdx", batch_id=None):
        FakeRepo.calls.append({"snapshot_date": snapshot_date, "rows": rows, "source": source})
        return {"batch_id": "batch-1", "snapshot_date": snapshot_date.isoformat(), "rows_written": len(rows), "source": source}


def test_sync_tdx_industry_quotes_dry_run_uses_sample_rows() -> None:
    result = sync_tdx_industry_quotes.run(
        PipelineContext(),
        JobRequest(job_key="sync_tdx_industry_quotes", dry_run=True, params={"snapshot_date": "2026-06-26"}),
    )

    assert result["rows_written"] == 0
    assert result["rows_read"] == 56
    assert result["sample"][0]["full_code"].startswith("sh")


def test_sync_tdx_industry_quotes_persists_supplied_rows(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_tdx_industry_quotes, "TdxIndustryQuoteRepository", FakeRepo)
    rows = [{"code6": "880471", "name": "银行", "full_code": "sh880471", "change_pct": 1.2}]

    result = sync_tdx_industry_quotes.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_tdx_industry_quotes",
            dry_run=False,
            params={"snapshot_date": "2026-06-26", "rows": json.dumps(rows), "create_table": True},
        ),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert FakeRepo.created == 1
    assert FakeRepo.calls[0]["snapshot_date"] == date(2026, 6, 26)
