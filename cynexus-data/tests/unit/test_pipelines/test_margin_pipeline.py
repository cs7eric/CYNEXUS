from __future__ import annotations

from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.margin import sync_margin
from cynexus_data.runtime.context import PipelineContext


class FakeAkshareClient:
    def fetch_margin_market_summary(self, trade_date):
        return [{"trade_date": trade_date, "exchange": "SSE", "source": "akshare.stock_margin_sse"}]

    def fetch_margin_stock_detail(self, trade_date):
        return [{"trade_date": trade_date, "exchange": "SSE", "stock_code": "600000", "source": "akshare.stock_margin_detail_sse"}]

    def fetch_margin_account_stats(self, trade_date):
        return [{"trade_date": trade_date, "source": "akshare.stock_margin_account_info"}]


class FakeAkshareHook:
    def get_client(self):
        return FakeAkshareClient()


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    akshare = FakeAkshareHook()
    postgres = FakePostgresHook()


class FakeRepo:
    created = 0
    calls = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeRepo.created += 1

    def replace_market_summary(self, *, trade_date, rows):
        FakeRepo.calls.append(("summary", trade_date, rows))
        return len(rows)

    def replace_stock_detail(self, *, trade_date, rows):
        FakeRepo.calls.append(("detail", trade_date, rows))
        return len(rows)

    def replace_account_stats(self, *, rows):
        FakeRepo.calls.append(("account", None, rows))
        return len(rows)


def test_sync_margin_dry_run_does_not_access_hooks() -> None:
    result = sync_margin.run(
        PipelineContext(),
        JobRequest(job_key="sync_margin", dry_run=True, params={"trade_date": "2026-07-04"}),
    )

    assert result["trade_date"] == "2026-07-04"
    assert result["rows_written"] == 0
    assert result["datasets"] == ["summary", "detail", "account"]


def test_sync_margin_fetches_selected_datasets(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_margin, "MarginRepository", FakeRepo)

    result = sync_margin.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_margin",
            dry_run=False,
            params={"trade_date": "2026-07-04", "datasets": "summary,account", "create_table": True},
        ),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 2
    assert result["summary_rows"] == 1
    assert result["detail_rows"] == 0
    assert result["account_rows"] == 1
    assert FakeRepo.created == 1
    assert FakeRepo.calls[0][1] == date(2026, 7, 4)
