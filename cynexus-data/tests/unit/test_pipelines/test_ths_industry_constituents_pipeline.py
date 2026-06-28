from __future__ import annotations

import json

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.stock_basic import sync_ths_industry_constituents
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


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeThsClient:
    def fetch_industry_constituents_all(self, code, *, max_pages=None, page_sleep=0.4):
        return {
            "code": code,
            "totalPages": max_pages or 1,
            "pageRowCounts": [1],
            "fetchedAt": "2026-06-26T15:10:00",
            "rows": [{"序号": 1, "代码": 3030, "名称": "祖名股份"}],
        }


class FakeThsHook:
    def get_client(self):
        return FakeThsClient()


class FakeHooks:
    postgres = FakePostgresHook()
    ths = FakeThsHook()


def test_sync_ths_industry_constituents_dry_run_counts_history(tmp_path) -> None:
    history_dir = tmp_path / "constituents"
    history_dir.mkdir()
    (history_dir / "881103.json").write_text(json.dumps({"rows": [{"代码": "003030"}]}, ensure_ascii=False), encoding="utf-8")

    result = sync_ths_industry_constituents.run(
        PipelineContext(),
        JobRequest(
            job_key="sync_ths_industry_constituents",
            dry_run=True,
            params={"history_path": str(history_dir), "snapshot_date": "2026-06-26"},
        ),
    )

    assert result["planned_payloads"] == 1
    assert result["rows_written"] == 0


def test_sync_ths_industry_constituents_imports_history_json(monkeypatch, tmp_path) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    history_file = tmp_path / "881103.json"
    history_file.write_text(
        json.dumps(
            {
                "fetchedAt": "2026-06-26T15:10:00",
                "pageRowCounts": [2],
                "rows": [
                    {"序号": 1, "代码": 3030, "名称": "祖名股份", "涨跌幅(%)": "10.01"},
                    {"序号": 2, "代码": "600597", "名称": "光明乳业"},
                ],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    monkeypatch.setattr(sync_ths_industry_constituents, "ThsIndustryConstituentRepository", FakeRepo)

    result = sync_ths_industry_constituents.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_ths_industry_constituents",
            dry_run=False,
            params={"history_path": str(history_file), "snapshot_date": "2026-06-26", "create_table": True},
        ),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 2
    assert FakeRepo.created == 1
    call = FakeRepo.calls[0]
    assert call["source_type"] == "json_import"
    assert call["requested_industry_codes"] == ["881103"]
    assert call["records"][0]["stock_code"] == "003030"


def test_sync_ths_industry_constituents_live_uses_ths_client(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_ths_industry_constituents, "ThsIndustryConstituentRepository", FakeRepo)

    result = sync_ths_industry_constituents.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_ths_industry_constituents",
            dry_run=False,
            params={"industry_codes": "881103", "snapshot_date": "2026-06-26", "max_pages": 1},
        ),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    call = FakeRepo.calls[0]
    assert call["source_type"] == "crawler"
    assert call["records"][0]["industry_code"] == "881103"
