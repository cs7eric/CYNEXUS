from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.stock_basic import sync_stock_basic
from cynexus_data.runtime.context import PipelineContext


class FakeRepo:
    created = 0
    records = []
    list_codes_result = []

    def __init__(self, client) -> None:
        self.client = client

    def create_table(self) -> None:
        FakeRepo.created += 1

    def upsert_many(self, records):
        FakeRepo.records = list(records)
        return len(records)

    def list_codes(self, active_only=True):
        return list(FakeRepo.list_codes_result)


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    postgres = FakePostgresHook()


class FakeEastMoneyClient:
    def fetch_stock_meta(self, code):
        return {
            "code": code,
            "name": "浦发银行",
            "industry": "银行",
            "total_market_cap": 400_000_000_000,
            "float_market_cap": 300_000_000_000,
            "source": "eastmoney",
        }


class FakeEastMoneyHook:
    def get_client(self):
        return FakeEastMoneyClient()


class FakeHooksWithEastMoney:
    postgres = FakePostgresHook()
    eastmoney = FakeEastMoneyHook()


def test_sync_stock_basic_dry_run_skips_external_access() -> None:
    result = sync_stock_basic.run(PipelineContext(), JobRequest(job_key="sync_stock_basic", dry_run=True, params={}))

    assert result["rows_written"] == 0


def test_sync_stock_basic_fetches_and_writes(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.records = []
    FakeRepo.list_codes_result = []
    monkeypatch.setattr(sync_stock_basic, "fetch_stock_code_name", lambda client: [{"code": "000001", "name": "平安银行", "market": "SZ", "board": "main_sz", "is_st": False, "limit_up_threshold": 0.1, "is_active": True, "source": "akshare"}])
    monkeypatch.setattr(sync_stock_basic, "StockBasicRepository", FakeRepo)

    result = sync_stock_basic.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="sync_stock_basic", dry_run=False, params={"create_table": True}))

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert FakeRepo.created == 1
    assert FakeRepo.records[0]["code"] == "000001"


def test_sync_stock_basic_fetches_eastmoney_meta_for_explicit_codes(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.records = []
    FakeRepo.list_codes_result = []
    monkeypatch.setattr(sync_stock_basic, "StockBasicRepository", FakeRepo)

    result = sync_stock_basic.run(
        PipelineContext(hooks=FakeHooksWithEastMoney()),
        JobRequest(job_key="sync_stock_basic", dry_run=False, params={"providers": "eastmoney", "codes": "600000"}),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert FakeRepo.records[0]["source"] == "eastmoney"
    assert FakeRepo.records[0]["industry"] == "银行"
    assert FakeRepo.records[0]["cap_style"] == "large"


def test_sync_stock_basic_eastmoney_can_use_existing_active_codes(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.records = []
    FakeRepo.list_codes_result = ["600000"]
    monkeypatch.setattr(sync_stock_basic, "StockBasicRepository", FakeRepo)

    result = sync_stock_basic.run(
        PipelineContext(hooks=FakeHooksWithEastMoney()),
        JobRequest(job_key="sync_stock_basic", dry_run=False, params={"providers": "eastmoney"}),
    )

    assert result["code_source"] == "postgres.sec_stock_basic"
    assert result["planned_codes"] == 1
    assert FakeRepo.records[0]["code"] == "600000"
