from __future__ import annotations

import json
from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market import build_market_heatmap
from cynexus_data.runtime.context import PipelineContext


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    postgres = FakePostgresHook()


class FakeQuoteRepo:
    def __init__(self, client):
        self.client = client

    def get_snapshot(self, snapshot_date, *, source="eltdx"):
        return [
            {"code6": "880471", "name": "银行", "full_code": "sh880471", "amount": 100, "change_pct": -0.1},
            {"code6": "880493", "name": "软件服务", "full_code": "sh880493", "amount": 300, "change_pct": 2.2},
        ]


class FakeHeatmapRepo:
    created = 0
    calls = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeHeatmapRepo.created += 1

    def upsert_snapshot(self, *, snapshot_date, kind, source, payload):
        FakeHeatmapRepo.calls.append({"snapshot_date": snapshot_date, "kind": kind, "source": source, "payload": payload})
        return {"snapshot_date": snapshot_date, "kind": kind, "source": source, "item_count": len(payload["items"])}


def test_build_market_heatmap_dry_run_uses_rows() -> None:
    rows = [{"code6": "880493", "name": "软件服务", "amount": 300, "change_pct": 2.2}]
    result = build_market_heatmap.run(
        PipelineContext(),
        JobRequest(job_key="build_market_heatmap", dry_run=True, params={"snapshot_date": "2026-06-26", "rows": json.dumps(rows)}),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 0
    assert result["payload"]["items"][0]["name"] == "软件服务"


def test_build_market_heatmap_reads_quote_snapshot_and_persists(monkeypatch) -> None:
    FakeHeatmapRepo.created = 0
    FakeHeatmapRepo.calls = []
    monkeypatch.setattr(build_market_heatmap, "TdxIndustryQuoteRepository", FakeQuoteRepo)
    monkeypatch.setattr(build_market_heatmap, "MarketHeatmapRepository", FakeHeatmapRepo)

    result = build_market_heatmap.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(job_key="build_market_heatmap", dry_run=False, params={"snapshot_date": "2026-06-26", "create_table": True}),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 2
    assert FakeHeatmapRepo.created == 1
    call = FakeHeatmapRepo.calls[0]
    assert call["snapshot_date"] == date(2026, 6, 26)
    assert call["kind"] == "industries"
    assert call["payload"]["items"][0]["name"] == "软件服务"
