from __future__ import annotations

import json
from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market import sync_tdx_industry_fund_flow
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

    def replace_snapshot(self, *, trade_date, rows, source="eltdx.f10.theme_market.200742", req_id="200742", fetched_at=None):
        FakeRepo.calls.append({"trade_date": trade_date, "rows": rows, "source": source, "req_id": req_id})
        return {"rows_written": len(rows), "ok_count": len(rows), "empty_count": 0, "error_count": 0}


def test_sync_tdx_industry_fund_flow_dry_run_uses_sample_rows() -> None:
    result = sync_tdx_industry_fund_flow.run(
        PipelineContext(),
        JobRequest(job_key="sync_tdx_industry_fund_flow", dry_run=True, params={"snapshot_date": "2026-07-03"}),
    )

    assert result["rows_written"] == 0
    assert result["rows_read"] == 3
    assert result["sample"][0]["code6"] == "880301"


def test_sync_tdx_industry_fund_flow_persists_supplied_rows(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_tdx_industry_fund_flow, "TdxIndustryFundFlowRepository", FakeRepo)
    rows = [
        {
            "code6": "880471",
            "name": "银行",
            "seed_code": "sh601398",
            "sections": {
                "main_capital_flow": [
                    {
                        "sector_name": "全国性银行",
                        "date": "20260703",
                        "main_net": 12.5,
                        "large_order_net": 10.0,
                        "medium_order_net": 2.0,
                        "small_order_net": 0.5,
                    }
                ]
            },
            "source": "eltdx.f10.theme_market.200742",
        }
    ]

    result = sync_tdx_industry_fund_flow.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_tdx_industry_fund_flow",
            dry_run=False,
            params={"snapshot_date": "2026-07-03", "rows": json.dumps(rows), "create_table": True},
        ),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert FakeRepo.created == 1
    assert FakeRepo.calls[0]["trade_date"] == date(2026, 7, 3)
