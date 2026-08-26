from __future__ import annotations

from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.a_stock.materialize_stock_screening_facts import run
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import read_stock_screening_fact_rows
from cynexus_data.storage.postgres.stock_screening_fact import _to_record


def test_screening_fact_reader_uses_serving_screener_aggregation() -> None:
    class FakeClient:
        def __init__(self) -> None:
            self.sql = ""
            self.params = {}

        def query_rows(self, sql, params):
            self.sql = sql
            self.params = params
            return [
                ("SH", "600000", date(2026, 8, 14), 10.0, 11.0, 9.0, 10.5, 10.0, 120.0, 1_000.0, 5.0, 20.0),
            ]

    client = FakeClient()

    rows = read_stock_screening_fact_rows(
        client,
        start_date="2026-08-14",
        end_date="2026-08-14",
    )

    assert "lagInFrame(toNullable(close_price), 1, NULL)" in client.sql
    assert "market IN ('SH', 'SZ', 'BJ')" in client.sql
    assert "startsWith(symbol, '688')" in client.sql
    assert client.params["start_date"] == date(2026, 8, 14)
    assert client.params["end_date"] == date(2026, 8, 14)
    assert client.params["history_start_date"] < client.params["start_date"]
    assert rows[0]["market"] == "SH"
    assert rows[0]["code"] == "600000"
    assert rows[0]["close_price"] == 10.5
    assert rows[0]["return_5d"] is None
    assert "ma20" in rows[0]


def test_screening_fact_record_has_stable_natural_key_id() -> None:
    row = {"market": "SZ", "code": "000001", "trade_date": "2026-08-14", "close_price": 12.3}

    first = _to_record(row, source="clickhouse.daily_raw")
    second = _to_record(row, source="clickhouse.daily_raw")

    assert first["id"] == second["id"]
    assert first["raw_payload"] == '{"market":"SZ","source_table":"daily_raw","projection_version":1}'


def test_materialization_dry_run_has_no_storage_side_effect() -> None:
    result = run(
        PipelineContext.default(),
        JobRequest(job_key="materialize_stock_screening_facts", dry_run=True, params={"trade_date": "2026-08-14"}),
    )

    assert result["rows_read"] == 0
    assert result["rows_written"] == 0
    assert result["start_date"] == "2026-08-14"


def test_materialization_writes_bounded_batches_and_advances_watermark(monkeypatch) -> None:
    class FakeClickHouseHook:
        def get_client(self):
            return object()

    class FakePostgresHook:
        def get_client(self):
            return object()

    class FakeHooks:
        clickhouse = FakeClickHouseHook()
        postgres = FakePostgresHook()

    writes: list[tuple[list[dict], dict]] = []

    class FakeRepository:
        SOURCE = "clickhouse.daily_raw"

        def __init__(self, client) -> None:
            self.client = client

        def replace_range(self, rows, **kwargs):
            writes.append((rows, kwargs))
            return len(rows)

        def count_rows(self, **kwargs):
            return 3

    rows = [
        {"market": "SH", "code": "600000", "trade_date": "2026-08-14", "close_price": 10.0},
        {"market": "SZ", "code": "000001", "trade_date": "2026-08-14", "close_price": 12.0},
        {"market": "BJ", "code": "830001", "trade_date": "2026-08-15", "close_price": 8.0},
    ]
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_stock_screening_facts.read_stock_screening_fact_rows",
        lambda *args, **kwargs: rows,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_stock_screening_facts.StockScreeningFactRepository",
        FakeRepository,
    )
    monkeypatch.setattr(
        "cynexus_data.pipelines.a_stock.materialize_stock_screening_facts.count_stock_screening_fact_source_rows",
        lambda *args, **kwargs: 3,
    )
    context = PipelineContext(hooks=FakeHooks())

    result = run(
        context,
        JobRequest(
            job_key="materialize_stock_screening_facts",
            params={"start_date": "2026-08-14", "end_date": "2026-08-15", "batch_size": 2},
        ),
    )

    assert [len(batch) for batch, _ in writes] == [3]
    assert writes[0][1]["batch_size"] == 2
    assert result["rows_read"] == 3
    assert result["rows_written"] == 3
    assert result["watermark"]["coverage_pct"] == 100.0
    assert result["coverage_by_trade_date"] == [
        {"trade_date": "2026-08-14", "source_daily_rows": 2},
        {"trade_date": "2026-08-15", "source_daily_rows": 1},
    ]
    assert context.checkpoints.get("a_stock.stock_screening_fact.watermark")["end_date"] == "2026-08-15"
