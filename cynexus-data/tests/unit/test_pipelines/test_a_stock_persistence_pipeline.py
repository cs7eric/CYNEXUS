from __future__ import annotations

from cynexus_data.domain.stock.a_stock_persistence import normalize_dataset
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.a_stock.sync_a_stock_dataset import run
from cynexus_data.runtime.context import PipelineContext


def test_normalize_eastmoney_minute_fund_flow_to_clickhouse() -> None:
    batch = normalize_dataset(
        "eastmoney_fund_flow_minute",
        [{"time": "2026-07-07 09:31", "main_net": "12.5", "small_net": "-1"}],
        {"code": "600519"},
    )

    rows = batch.clickhouse["ch_ods_stock_fund_flow_minute"]
    assert rows[0]["code"] == "600519"
    assert rows[0]["main_net"] == 12.5
    assert rows[0]["small_net"] == -1.0


def test_normalize_cninfo_announcement_to_postgres() -> None:
    batch = normalize_dataset(
        "cninfo_announcements",
        [{"announcement_id": "abc", "title": "测试公告", "date": "2026-07-07", "url": "https://example.test/a.pdf"}],
        {"code": "000001"},
    )

    rows = batch.postgres["a_stock_announcement"]
    assert rows[0]["source"] == "cninfo"
    assert rows[0]["code"] == "000001"
    assert rows[0]["announcement_id"] == "abc"


def test_pipeline_dry_run_uses_payload_without_network_or_storage() -> None:
    request = JobRequest(
        job_key="sync_a_stock_dataset",
        dry_run=True,
        params={
            "dataset": "eastmoney_fund_flow_120d",
            "code": "600519",
            "payload": [{"date": "2026-07-07", "main_net": "1.25"}],
        },
    )

    result = run(PipelineContext.default(), request)

    assert result["rows_read"] == 1
    assert result["rows_written"] == 0
    assert result["postgres_tables"] == {"a_stock_fund_flow_daily": 1}
    assert result["clickhouse_tables"] == {}


def test_pipeline_dry_run_accepts_json_string_payload() -> None:
    request = JobRequest(
        job_key="sync_a_stock_dataset",
        dry_run=True,
        params={
            "dataset": "eastmoney_fund_flow_120d",
            "code": "600519",
            "payload": '[{"date":"2026-07-07","main_net":"1.25"}]',
        },
    )

    result = run(PipelineContext.default(), request)

    assert result["rows_read"] == 1
    assert result["postgres_tables"] == {"a_stock_fund_flow_daily": 1}
