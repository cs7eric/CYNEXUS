from __future__ import annotations

from cynexus_data.domain.stock.a_stock_persistence import normalize_dataset
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.a_stock.sync_a_stock_dataset import run
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres import a_stock


def test_fund_flow_daily_uses_stable_id_for_natural_key() -> None:
    row = {
        "source": "easy_tdx.tdx",
        "code": "000791",
        "trade_date": "2026-07-08",
        "main_net": 1,
    }

    first = a_stock._prepare_record("a_stock_fund_flow_daily", row, key_columns=["source", "code", "trade_date"])
    second = a_stock._prepare_record("a_stock_fund_flow_daily", row, key_columns=["source", "code", "trade_date"])
    other = a_stock._prepare_record(
        "a_stock_fund_flow_daily",
        {**row, "trade_date": "2026-07-09"},
        key_columns=["source", "code", "trade_date"],
    )

    assert first["id"] == second["id"]
    assert first["id"] != other["id"]


def test_pipeline_counts_content_extraction_result_without_rows_normalized_key(monkeypatch) -> None:
    class FakePostgresHook:
        def get_client(self):
            return object()

    class FakeHooks:
        postgres = FakePostgresHook()

    class FakeRepo:
        def __init__(self, client):
            self.client = client

        def upsert_batches(self, batches):
            return sum(len(rows) for rows in batches.values())

    monkeypatch.setattr("cynexus_data.pipelines.a_stock.sync_a_stock_dataset.AStockPostgresRepository", FakeRepo)
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_a_stock_dataset.extract_and_persist",
        lambda client, identities: {"ready": 1, "failed": 2, "needs_ocr": 0, "unsupported": 0, "summary_only": 1, "skipped_cached": 3},
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_a_stock_dataset.identities_from_batches",
        lambda batches: [object()],
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_a_stock_dataset.recent_backfill_identities",
        lambda client, limit: [],
    )

    result = run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_a_stock_dataset",
            dry_run=False,
            params={
                "dataset": "eastmoney_global_news",
                "payload": [{"id": "news-1", "title": "Market news", "time": "2026-07-17 09:00", "url": "https://example.test/news"}],
                "extract_market_information_content": True,
            },
        ),
    )

    assert result["rows_read"] == 1
    assert result["dataset_results"]["market_information_content"]["rows_normalized"] == 0


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


def test_normalize_sina_stock_news_to_postgres_with_viewable_url() -> None:
    batch = normalize_dataset(
        "sina_stock_news",
        [{"title": "Sina news", "time": "2026-07-10 09:30", "source": "Sina Finance", "url": "https://example.test/news"}],
        {"code": "600415"},
    )

    row = batch.postgres["a_stock_news"][0]
    assert row["source"] == "sina.stock"
    assert row["code"] == "600415"
    assert row["published_at"].isoformat() == "2026-07-10T09:30:00"
    assert row["url"] == "https://example.test/news"


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


def test_normalize_eastmoney_delayed_fund_flow_preserves_source() -> None:
    batch = normalize_dataset(
        "eastmoney_fund_flow_120d",
        {
            "source": "eastmoney.push2delay",
            "mode": "daily_fallback",
            "items": [{"date": "2026-07-10", "main_net": "43651509"}],
        },
        {"code": "600415"},
    )

    row = batch.postgres["a_stock_fund_flow_daily"][0]
    assert row["source"] == "eastmoney.push2delay"
    assert row["code"] == "600415"
    assert row["main_net"] == 43651509.0


def test_pipeline_reports_delayed_fallback_as_incomplete_history() -> None:
    result = run(
        PipelineContext.default(),
        JobRequest(
            job_key="sync_a_stock_dataset",
            dry_run=True,
            params={
                "dataset": "eastmoney_fund_flow_120d",
                "code": "600415",
                "payload": {
                    "source": "eastmoney.push2delay",
                    "mode": "daily_fallback",
                    "history_complete": False,
                    "items": [{"date": "2026-07-10", "main_net": "43651509"}],
                },
            },
        ),
    )

    dataset = result["dataset_results"]["eastmoney_fund_flow_120d"]
    assert dataset["sources"] == ["eastmoney.push2delay"]
    assert dataset["daily_fallback_count"] == 1
    assert dataset["history_complete_count"] == 0
