from __future__ import annotations

from cynexus_data.storage.clickhouse.readers import aggregate_kline_period_to_clickhouse, count_aggregate_kline_bars


class FakeClickHouseClient:
    def __init__(self) -> None:
        self.queries: list[tuple[str, dict]] = []
        self.commands: list[tuple[str, dict]] = []

    def query_one(self, sql: str, parameters=None):
        self.queries.append((sql, dict(parameters or {})))
        return (7,)

    def command(self, sql: str, parameters=None, **kwargs):
        self.commands.append((sql, dict(parameters or {})))
        return None


def test_count_aggregate_kline_bars_uses_period_one_source_filter() -> None:
    client = FakeClickHouseClient()

    rows = count_aggregate_kline_bars(client, 5, start_date="2026-06-01", end_date="2026-06-02", symbols=["000001"])

    assert rows == 7
    sql, params = client.queries[0]
    assert "toStartOfInterval(trade_time, INTERVAL 5 minute)" in sql
    assert "period = %(period)s" in sql
    assert params == {"period": 1, "start_date": "2026-06-01", "end_date": "2026-06-02", "symbols": ["000001"]}


def test_aggregate_kline_period_inserts_aggregate_rows() -> None:
    client = FakeClickHouseClient()

    rows = aggregate_kline_period_to_clickhouse(client, 30, symbols=["000001"])

    assert rows == 7
    assert len(client.commands) == 1
    sql, params = client.commands[0]
    assert "INSERT INTO ch_dwd_kline_1m" in sql
    assert "30 AS period" in sql
    assert "'aggregate_1m' AS source" in sql
    assert params == {"period": 1, "symbols": ["000001"]}
