from __future__ import annotations

import json
from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market import sync_tdx_sector_quote_details
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

    def replace_snapshots(self, *, snapshot_date, rows, source="eltdx.f10.theme_market"):
        FakeRepo.calls.append({"snapshot_date": snapshot_date, "rows": rows, "source": source})
        return len(rows)


def test_sync_tdx_sector_quote_details_dry_run_uses_sample_rows() -> None:
    result = sync_tdx_sector_quote_details.run(
        PipelineContext(),
        JobRequest(job_key="sync_tdx_sector_quote_details", dry_run=True, params={"snapshot_date": "2026-06-26", "codes": "880471", "datasets": "period_stats,constituents"}),
    )

    assert result["rows_written"] == 0
    assert result["rows_read"] == 1
    assert result["sample"][0]["code6"] == "880471"
    assert set(result["sample"][0]["payload"]["sections"]) == {"period_stats", "constituents"}


def test_sync_tdx_sector_quote_details_persists_supplied_rows(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_tdx_sector_quote_details, "TdxSectorQuoteDetailRepository", FakeRepo)
    rows = [
        {
            "code6": "880471",
            "name": "银行",
            "seed_code": "sh600000",
            "sections": {"period_stats": [{"range_code": "0"}]},
        }
    ]

    result = sync_tdx_sector_quote_details.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_tdx_sector_quote_details",
            dry_run=False,
            params={"snapshot_date": "2026-06-26", "rows": json.dumps(rows), "create_table": True},
        ),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert FakeRepo.created == 1
    assert FakeRepo.calls[0]["snapshot_date"] == date(2026, 6, 26)
