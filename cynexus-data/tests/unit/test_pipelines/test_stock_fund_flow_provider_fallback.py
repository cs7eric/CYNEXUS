from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.fund_flow import sync_stock_flow
from cynexus_data.runtime.context import PipelineContext


class Repo:
    calls = []

    def __init__(self, client):
        pass

    def create_table(self):
        pass

    def replace_snapshot(self, **kwargs):
        self.calls.append(kwargs)
        return len(kwargs["records"])


class Hooks:
    class Postgres:
        def get_client(self):
            return object()

    postgres = Postgres()


def test_current_flow_fallback_is_per_symbol(monkeypatch):
    monkeypatch.setattr(sync_stock_flow, "StockFundFlowRepository", Repo)
    monkeypatch.setattr(sync_stock_flow, "fetch_current_stock_fund_flow_batch", lambda codes, trade_date: (_ for _ in ()).throw(RuntimeError("batch down")))
    monkeypatch.setattr(sync_stock_flow, "fetch_current_stock_fund_flow", lambda code, trade_date: [] if code.endswith("002") else [{"code": code, "trade_date": trade_date, "active_net_amount_wan": 1}])
    monkeypatch.setattr(sync_stock_flow.eastmoney_a_stock, "fund_flow_120d", lambda client, code, mode: {"source": "eastmoney.push2delay", "items": [{"date": str(date.today()), "main_net": 10000}]})

    result = sync_stock_flow.run(
        PipelineContext(hooks=Hooks()),
        JobRequest(job_key="sync_stock_fund_flow", params={"provider": "easy_tdx", "codes": ["600001", "600002"], "batch_size": 2}),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 2
    assert {call["source"] for call in Repo.calls} == {"easy_tdx.tdx", "eastmoney.push2delay"}
