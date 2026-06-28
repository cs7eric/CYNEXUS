from __future__ import annotations

import json
from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market import build_style_sector
from cynexus_data.runtime.context import PipelineContext


class FakeTencentClient:
    def fetch_realtime_snapshots_batch(self, codes, *, batch_size=500, batch_sleep=0.22):
        return {
            "sz000001": {"code": "000001", "change_pct": 2.0},
            "sz000002": {"code": "000002", "change_pct": 4.0},
        }


class FakeTencentHook:
    def get_client(self):
        return FakeTencentClient()


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    postgres = FakePostgresHook()
    tencent = FakeTencentHook()


class FakeSectorRepo:
    def __init__(self, client):
        self.client = client

    def create_table(self):
        pass

    def list_sectors_by_category(self, category_raw):
        assert category_raw == 4
        return [{"name": "昨日涨停", "stock_codes": ["000001", "000002"]}]


class FakeStyleRepo:
    saved = None

    def __init__(self, client):
        self.client = client

    def create_table(self):
        pass

    def replace_snapshots(self, *, trade_date, rows, source="tencent.qt"):
        FakeStyleRepo.saved = {"trade_date": trade_date, "rows": rows, "source": source}
        return len(rows)


def test_build_style_sector_dry_run_uses_supplied_rows() -> None:
    sectors = [{"name": "昨日涨停", "stock_codes": ["000001"]}]
    quotes = {"000001": {"change_pct": 9.9}}

    result = build_style_sector.run(
        PipelineContext(),
        JobRequest(job_key="build_style_sector", dry_run=True, params={"trade_date": "2026-06-26", "sectors": json.dumps(sectors, ensure_ascii=False), "quotes_by_code": json.dumps(quotes)}),
    )

    assert result["rows_read"] == 1
    assert result["planned_rows"] == 1
    assert result["payload"]["items"][0]["change_pct"] == 9.9


def test_build_style_sector_reads_sectors_fetches_quotes_and_persists(monkeypatch) -> None:
    FakeStyleRepo.saved = None
    monkeypatch.setattr(build_style_sector, "StockSectorRepository", FakeSectorRepo)
    monkeypatch.setattr(build_style_sector, "StyleSectorRepository", FakeStyleRepo)

    result = build_style_sector.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="build_style_sector", dry_run=False, params={"trade_date": "2026-06-26"}))

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert FakeStyleRepo.saved["trade_date"] == date(2026, 6, 26)
    assert FakeStyleRepo.saved["rows"][0]["change_pct"] == 3.0
