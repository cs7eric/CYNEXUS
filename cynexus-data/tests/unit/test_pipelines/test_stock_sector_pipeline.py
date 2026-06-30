from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.stock_basic import sync_stock_sectors
from cynexus_data.runtime.context import PipelineContext


class FakeClient:
    def close(self):
        self.closed = True


class FakeStockBasicRepo:
    def __init__(self, client):
        self.client = client

    def list_codes(self, active_only=True):
        return ["000001"]


class FakeSectorRepo:
    created = 0
    written = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeSectorRepo.created += 1

    def replace_stock_mappings(self, code, mappings):
        FakeSectorRepo.written.append((code, mappings))
        return len(mappings)


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    postgres = FakePostgresHook()


def test_sync_stock_sectors_dry_run_skips_external_access() -> None:
    result = sync_stock_sectors.run(PipelineContext(), JobRequest(job_key="sync_stock_sectors", dry_run=True, params={"codes": "000001"}))

    assert result["planned_codes"] == 1
    assert result["rows_written"] == 0


def test_sync_stock_sectors_fetches_topics_and_writes(monkeypatch) -> None:
    FakeSectorRepo.created = 0
    FakeSectorRepo.written = []
    monkeypatch.setattr(sync_stock_sectors, "create_eltdx_client", lambda pool_size=8, timeout=8, host=None: FakeClient())
    monkeypatch.setattr(sync_stock_sectors, "fetch_stock_topics", lambda client, code: [{"topic_id": "T1", "topic_name": "人工智能", "reason": "公司属于软件服务（通达信研究行业）", "relation_level": 0.9, "category_raw": 2}])
    monkeypatch.setattr(sync_stock_sectors, "StockBasicRepository", FakeStockBasicRepo)
    monkeypatch.setattr(sync_stock_sectors, "StockSectorRepository", FakeSectorRepo)

    result = sync_stock_sectors.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="sync_stock_sectors", dry_run=False, params={"create_table": True}))

    assert result["code_source"] == "postgres.sec_stock_basic"
    assert result["rows_read"] == 1
    assert result["rows_written"] == 2
    assert FakeSectorRepo.created == 1
    assert FakeSectorRepo.written[0][0] == "000001"
