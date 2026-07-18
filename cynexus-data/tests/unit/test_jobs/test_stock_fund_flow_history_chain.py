from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.jobs import stock_fund_flow_history_chain as chain
from cynexus_data.runtime.context import PipelineContext


class FakeStockRepo:
    def __init__(self, client):
        self.client = client

    def list_codes(self, active_only=True):
        return ["600001", "600002", "600003"]


class FakeLimitRepo:
    def __init__(self, client):
        self.client = client

    def list_historical_limit_up_codes(self):
        return ["600002", "600003", "600002"]


def test_chain_skips_completed_codes_and_processes_only_next_shard(monkeypatch):
    monkeypatch.setattr(chain, "StockBasicRepository", FakeStockRepo)
    calls = []

    def fake_run(context, request):
        calls.append(request.params["code"])
        return {
            "rows_read": 120,
            "rows_written": 120,
            "dataset_results": {chain.DATASET: {"history_complete_count": 1, "sources": ["easy_tdx.tdx"]}},
        }

    monkeypatch.setattr(chain.sync_a_stock_dataset, "run", fake_run)
    context = PipelineContext()
    context.checkpoints.put(
        "stock-fund-flow-history:all-active:120",
        chain.JOB_KEY,
        {"completed_codes": ["600001"], "failed_codes": {}, "total_codes": 3},
    )

    result = chain.run(
        context,
        JobRequest(job_key=chain.JOB_KEY, params={"shard_size": 1, "fund_flow_count": 120}),
        "run-1",
    )

    assert calls == ["600002"]
    assert result.success is True
    assert result.metadata["completed_codes"] == 2
    assert result.metadata["pending_codes"] == 1


def test_chain_isolates_failed_stock_and_keeps_it_for_retry(monkeypatch):
    monkeypatch.setattr(chain, "StockBasicRepository", FakeStockRepo)

    def fake_run(context, request):
        if request.params["code"] == "600002":
            raise RuntimeError("provider unavailable")
        return {
            "rows_read": 120,
            "rows_written": 120,
            "dataset_results": {chain.DATASET: {"history_complete_count": 1, "sources": ["easy_tdx.tdx"]}},
        }

    monkeypatch.setattr(chain.sync_a_stock_dataset, "run", fake_run)
    context = PipelineContext()
    result = chain.run(context, JobRequest(job_key=chain.JOB_KEY, params={"shard_size": 3, "fund_flow_count": 120}), "run-2")

    assert result.success is False
    assert result.status == "partial"
    assert result.metadata["completed_codes"] == 2
    checkpoint = context.checkpoints.get("stock-fund-flow-history:all-active:120")
    assert checkpoint["completed_codes"] == ["600001", "600003"]
    assert "600002" in checkpoint["failed_codes"]


def test_chain_resolves_limit_up_and_remaining_scopes(monkeypatch):
    monkeypatch.setattr(chain, "StockBasicRepository", FakeStockRepo)
    monkeypatch.setattr(chain, "MarketLimitRepository", FakeLimitRepo)
    context = PipelineContext()

    assert chain._resolve_codes(context, {"stock_scope": "limit-up-history"}) == ["600002", "600003"]
    assert chain._resolve_codes(context, {"stock_scope": "remaining-active"}) == ["600001"]


def test_chain_resolves_missing_120_scope(monkeypatch):
    class FakeResult:
        def all(self):
            return [("600003",), ("600001",)]

    class FakeSession:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

        def execute(self, statement, params):
            assert params == {"minimum_rows": 120}
            assert "a_stock_fund_flow_daily" in str(statement)
            return FakeResult()

    class FakeClient:
        def session(self):
            return FakeSession()

    class FakePostgresHook:
        def get_client(self):
            return FakeClient()

    class FakeHooks:
        postgres = FakePostgresHook()

    context = PipelineContext(hooks=FakeHooks())

    assert chain._resolve_codes(context, {"stock_scope": "missing-120"}) == ["600003", "600001"]


def test_chain_run_all_processes_complete_scope(monkeypatch):
    monkeypatch.setattr(chain, "StockBasicRepository", FakeStockRepo)
    calls = []

    def fake_run(context, request):
        calls.append(request.params["code"])
        return {
            "rows_read": 5,
            "rows_written": 5,
            "dataset_results": {chain.DATASET: {"history_complete_count": 1, "sources": ["easy_tdx.tdx"]}},
        }

    monkeypatch.setattr(chain.sync_a_stock_dataset, "run", fake_run)
    result = chain.run(
        PipelineContext(),
        JobRequest(job_key=chain.JOB_KEY, params={"run_all": True, "fund_flow_count": 5}),
        "run-all",
    )

    assert calls == ["600001", "600002", "600003"]
    assert result.success is True
    assert result.metadata["history_count"] == 5


def test_chain_run_all_batches_with_sleep(monkeypatch):
    monkeypatch.setattr(chain, "StockBasicRepository", FakeStockRepo)
    calls = []
    sleeps = []

    def fake_run(context, request):
        calls.append(request.params["code"])
        return {
            "rows_read": 120,
            "rows_written": 120,
            "dataset_results": {chain.DATASET: {"history_complete_count": 1, "sources": ["easy_tdx.tdx"]}},
        }

    monkeypatch.setattr(chain.sync_a_stock_dataset, "run", fake_run)
    monkeypatch.setattr(chain.time, "sleep", lambda seconds: sleeps.append(seconds))

    result = chain.run(
        PipelineContext(),
        JobRequest(
            job_key=chain.JOB_KEY,
            params={"run_all": True, "shard_size": 2, "batch_sleep_seconds": 30, "fund_flow_count": 120},
        ),
        "run-all-batches",
    )

    assert sorted(calls) == ["600001", "600002", "600003"]
    assert sleeps == [30.0]
    assert result.success is True
    assert result.metadata["completed_codes"] == 3
