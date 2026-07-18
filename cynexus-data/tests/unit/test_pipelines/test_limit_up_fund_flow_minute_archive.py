from __future__ import annotations

from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive import (
    daily_rows_from_minute_aggregates,
    run,
)
from cynexus_data.runtime.context import PipelineContext


def test_daily_rows_from_minute_aggregates_uses_latest_minute_values() -> None:
    rows = daily_rows_from_minute_aggregates(
        [
            {
                "code": "600415",
                "trade_date": "2026-07-17",
                "main_net": -61224951,
                "small_net": 38573543,
                "mid_net": 22656674,
                "large_net": 1978858,
                "super_net": -63203809,
                "latest_trade_time": "2026-07-17 14:57:00",
                "minute_rows": 240,
            }
        ]
    )

    assert rows == [
        {
            "source": "eastmoney.push2.minute_agg",
            "code": "600415",
            "trade_date": date(2026, 7, 17),
            "main_net": -61224951,
            "small_net": 38573543,
            "mid_net": 22656674,
            "large_net": 1978858,
            "super_net": -63203809,
            "raw_payload": {
                "source": "eastmoney.push2",
                "aggregation": "latest_minute_of_day",
                "latest_trade_time": "2026-07-17 14:57:00",
                "minute_rows": 240,
            },
        }
    ]


def test_pipeline_uses_checkpointed_round_robin_shard_and_flushes_batches(monkeypatch) -> None:
    class FakePostgresHook:
        def get_client(self):
            return object()

    class FakeClickHouseHook:
        def get_client(self):
            return object()

    class FakeHooks:
        postgres = FakePostgresHook()
        clickhouse = FakeClickHouseHook()

    class FakeMarketLimitRepository:
        def __init__(self, client):
            self.client = client

        def list_historical_limit_up_codes(self, *, limit=None, offset=0):
            codes = ["000001", "000002", "000003", "000004"]
            return codes[:limit] if limit else codes

    class FakeClickHouseRepository:
        instances = []

        def __init__(self, client):
            self.insert_sizes = []
            FakeClickHouseRepository.instances.append(self)

        def create_tables(self):
            return 0

        def insert_batches(self, batches):
            count = sum(len(rows) for rows in batches.values())
            self.insert_sizes.append(count)
            return count

        def latest_fund_flow_minute_daily_rows(self, **kwargs):
            return [
                {
                    "code": code,
                    "trade_date": kwargs["trade_date"],
                    "main_net": 10,
                    "latest_trade_time": f"{kwargs['trade_date']} 14:57:00",
                    "minute_rows": 1,
                }
                for code in kwargs["codes"]
            ]

    class FakePostgresRepository:
        def __init__(self, client):
            self.client = client

        def create_tables(self):
            return 0

        def upsert_many(self, table, rows):
            return len(rows)

    class FakeEastMoneyClient:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.MarketLimitRepository",
        FakeMarketLimitRepository,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.AStockClickHouseRepository",
        FakeClickHouseRepository,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.AStockPostgresRepository",
        FakePostgresRepository,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.EastMoneyHttpClient",
        FakeEastMoneyClient,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.em.fund_flow_minute",
        lambda client, code: [{"time": "2026-07-17 14:57", "main_net": "10"}],
    )

    context = PipelineContext(hooks=FakeHooks())
    context.checkpoints.put(
        "a_stock.sync_limit_up_fund_flow_minute_archive.round_robin",
        "sync_limit_up_fund_flow_minute_archive",
        {"next_offset": 2},
    )

    result = run(
        context,
        JobRequest(
            job_key="sync_limit_up_fund_flow_minute_archive",
            dry_run=False,
            params={
                "trade_date": "2026-07-17",
                "shard_size": 3,
                "batch_flush_successes": 2,
                "fetch_retries": 0,
                "request_sleep_seconds": 0,
            },
        ),
    )

    assert result["planned_codes"] == 3
    assert result["code_offset"] == 2
    assert result["next_offset"] == 1
    assert result["success_codes"] == 3
    assert result["clickhouse_rows_written"] == 3
    assert FakeClickHouseRepository.instances[-1].insert_sizes == [2, 1]
    assert context.checkpoints.get("a_stock.sync_limit_up_fund_flow_minute_archive.round_robin")["next_offset"] == 1


def test_pipeline_stops_at_runtime_bound_and_checkpoints_next_offset(monkeypatch) -> None:
    class FakePostgresHook:
        def get_client(self):
            return object()

    class FakeClickHouseHook:
        def get_client(self):
            return object()

    class FakeHooks:
        postgres = FakePostgresHook()
        clickhouse = FakeClickHouseHook()

    class FakeMarketLimitRepository:
        def __init__(self, client):
            self.client = client

        def list_historical_limit_up_codes(self, *, limit=None, offset=0):
            return ["000001", "000002", "000003"]

    class FakeClickHouseRepository:
        def __init__(self, client):
            self.client = client

        def insert_batches(self, batches):
            return sum(len(rows) for rows in batches.values())

        def latest_fund_flow_minute_daily_rows(self, **kwargs):
            return []

    class FakePostgresRepository:
        def __init__(self, client):
            self.client = client

        def upsert_many(self, table, rows):
            return len(rows)

    class FakeEastMoneyClient:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

    ticks = iter([0.0, 0.0, 10.0])

    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.MarketLimitRepository",
        FakeMarketLimitRepository,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.AStockClickHouseRepository",
        FakeClickHouseRepository,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.AStockPostgresRepository",
        FakePostgresRepository,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.EastMoneyHttpClient",
        FakeEastMoneyClient,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.em.fund_flow_minute",
        lambda client, code: [{"time": "2026-07-17 14:57", "main_net": "10"}],
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.sync_limit_up_fund_flow_minute_archive.time.monotonic",
        lambda: next(ticks),
    )

    context = PipelineContext(hooks=FakeHooks())
    result = run(
        context,
        JobRequest(
            job_key="sync_limit_up_fund_flow_minute_archive",
            dry_run=False,
            params={
                "trade_date": "2026-07-17",
                "shard_size": 3,
                "fetch_retries": 0,
                "request_sleep_seconds": 0,
                "max_runtime_seconds": 5,
            },
        ),
    )

    assert result["attempted_codes"] == 1
    assert result["stop_reason"] == "max_runtime_seconds"
    assert result["next_offset"] == 1
    assert result["status"] == "partial"
