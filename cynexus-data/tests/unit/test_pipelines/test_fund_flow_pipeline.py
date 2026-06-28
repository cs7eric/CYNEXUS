from __future__ import annotations

import json

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.fund_flow import sync_sector_flow, sync_stock_flow
from cynexus_data.runtime.context import PipelineContext


class FakeRepo:
    created = 0
    calls = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeRepo.created += 1

    def replace_snapshot(self, **kwargs):
        FakeRepo.calls.append(kwargs)
        return len(kwargs["records"])


class FakeStockRepo:
    created = 0
    calls = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeStockRepo.created += 1

    def replace_snapshot(self, **kwargs):
        FakeStockRepo.calls.append(kwargs)
        return len(kwargs["records"])


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    postgres = FakePostgresHook()


class FakeTencentClient:
    def fetch_stock_fund_flow_batch(self, symbols):
        return {
            symbols[0]: {
                "symbol": symbols[0],
                "code": symbols[0][-6:],
                "name": "浦发银行",
                "last_price": 10,
                "outer_disc_lots": 600,
                "inside_dish_lots": 400,
                "active_net_amount_wan": 0.2,
            }
        }


class FakeTencentHook:
    def get_client(self):
        return FakeTencentClient()


class FakeStockHooks:
    postgres = FakePostgresHook()
    tencent = FakeTencentHook()


def test_sync_sector_flow_dry_run_skips_external_access(tmp_path) -> None:
    history_dir = tmp_path / "history"
    history_dir.mkdir()
    (history_dir / "2026-06-26.json").write_text(json.dumps({"rows": [{"行业": "软件服务"}]}, ensure_ascii=False), encoding="utf-8")

    result = sync_sector_flow.run(
        PipelineContext(),
        JobRequest(job_key="sync_fund_flow", dry_run=True, params={"history_path": str(history_dir), "trade_date": "2026-06-26"}),
    )

    assert result["planned_payloads"] == 1
    assert result["rows_written"] == 0


def test_sync_sector_flow_imports_history_json(monkeypatch, tmp_path) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    history_file = tmp_path / "2026-06-26.json"
    history_file.write_text(
        json.dumps(
            {
                "totalPages": 1,
                "pageRowCounts": [1],
                "fetchedAt": "2026-06-26T15:10:00",
                "rows": [{"行业": "软件服务", "净额(亿)": "6", "流入资金(亿)": "10", "流出资金(亿)": "4"}],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    monkeypatch.setattr(sync_sector_flow, "SectorFundFlowRepository", FakeRepo)

    result = sync_sector_flow.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(job_key="sync_fund_flow", dry_run=False, params={"history_path": str(history_file), "create_table": True}),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert result["snapshots_written"] == 1
    assert FakeRepo.created == 1
    call = FakeRepo.calls[0]
    assert call["source_type"] == "json_import"
    assert call["records"][0]["sector_name"] == "软件服务"


def test_sync_stock_flow_dry_run_skips_external_access(tmp_path) -> None:
    history_dir = tmp_path / "stock_history"
    history_dir.mkdir()
    (history_dir / "2026-06-26.json").write_text(json.dumps({"rows": [{"code": "600000"}]}, ensure_ascii=False), encoding="utf-8")

    result = sync_stock_flow.run(
        PipelineContext(),
        JobRequest(job_key="sync_stock_fund_flow", dry_run=True, params={"history_path": str(history_dir), "trade_date": "2026-06-26"}),
    )

    assert result["planned_payloads"] == 1
    assert result["rows_written"] == 0


def test_sync_stock_flow_imports_history_json(monkeypatch, tmp_path) -> None:
    FakeStockRepo.created = 0
    FakeStockRepo.calls = []
    history_file = tmp_path / "2026-06-26.json"
    history_file.write_text(
        json.dumps(
            {
                "fetchedAt": "2026-06-26T15:10:00",
                "requestedCodes": ["sh600000"],
                "rows": [{"symbol": "sh600000", "code": "600000", "name": "浦发银行", "activeNetAmountWan": "0.2"}],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    monkeypatch.setattr(sync_stock_flow, "StockFundFlowRepository", FakeStockRepo)

    result = sync_stock_flow.run(
        PipelineContext(hooks=FakeStockHooks()),
        JobRequest(job_key="sync_stock_fund_flow", dry_run=False, params={"history_path": str(history_file), "create_table": True}),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert FakeStockRepo.created == 1
    call = FakeStockRepo.calls[0]
    assert call["source_type"] == "json_import"
    assert call["requested_codes"] == ["sh600000"]
    assert call["records"][0]["active_net_amount_wan"] == 0.2


def test_sync_stock_flow_live_uses_tencent_client(monkeypatch) -> None:
    FakeStockRepo.created = 0
    FakeStockRepo.calls = []
    monkeypatch.setattr(sync_stock_flow, "StockFundFlowRepository", FakeStockRepo)

    result = sync_stock_flow.run(
        PipelineContext(hooks=FakeStockHooks()),
        JobRequest(job_key="sync_stock_fund_flow", dry_run=False, params={"trade_date": "2026-06-26", "codes": "600000"}),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    call = FakeStockRepo.calls[0]
    assert call["source_type"] == "crawler"
    assert call["records"][0]["code"] == "600000"
    assert call["records"][0]["source"] == "tencent.qt"
