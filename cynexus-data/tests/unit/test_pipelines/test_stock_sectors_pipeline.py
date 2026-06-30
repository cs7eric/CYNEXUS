from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.stock_basic import sync_stock_sectors
from cynexus_data.runtime.context import PipelineContext


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    postgres = FakePostgresHook()


class FakeBasicRepo:
    def __init__(self, client) -> None:
        self.client = client

    def list_codes(self, active_only: bool = True) -> list[str]:
        return ["000001", "000002", "000003"]


class FakeSectorRepo:
    calls: list[tuple[str, list[dict]]] = []

    def __init__(self, client) -> None:
        self.client = client

    def create_table(self) -> None:
        return None

    def replace_stock_mappings(self, code: str, mappings: list[dict]) -> int:
        self.calls.append((code, list(mappings)))
        return len(mappings)


class FakeClient:
    closed = 0

    def connect(self) -> None:
        return None

    def close(self) -> None:
        FakeClient.closed += 1


def test_sync_stock_sectors_skips_non_trading_day() -> None:
    result = sync_stock_sectors.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_stock_sectors",
            params={"trading_day_only": True, "target_date": "2026-06-19"},
        ),
    )

    assert result["skipped"] is True
    assert result["skip_reason"] == "non_trading_day"


def test_sync_stock_sectors_retries_empty_topics_before_persisting(monkeypatch) -> None:
    FakeClient.closed = 0
    FakeSectorRepo.calls = []
    attempts: dict[str, int] = {}

    def fake_fetch_topics(client, code: str) -> list[dict]:
        attempts[code] = attempts.get(code, 0) + 1
        if code == "000002" and attempts[code] == 1:
            return []
        return [{"topic_id": f"topic-{code}", "topic_name": "topic", "category_raw": 2}]

    monkeypatch.setattr(sync_stock_sectors, "StockBasicRepository", FakeBasicRepo)
    monkeypatch.setattr(sync_stock_sectors, "StockSectorRepository", FakeSectorRepo)
    monkeypatch.setattr(sync_stock_sectors, "create_eltdx_client", lambda **kwargs: FakeClient())
    monkeypatch.setattr(sync_stock_sectors, "fetch_stock_topics", fake_fetch_topics)
    monkeypatch.setattr(
        sync_stock_sectors,
        "stock_topics_to_sector_mappings",
        lambda code, topics: [{"sector_id": f"sector-{code}", "code": code}],
    )

    result = sync_stock_sectors.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_stock_sectors",
            params={"limit_codes": 3, "shard_size": 2, "workers": 1, "max_retry_rounds": 2, "sleep_retry": 0},
        ),
    )

    assert result["planned_codes"] == 3
    assert result["failed_codes"] == 0
    assert result["retry_rounds"] == 1
    assert attempts["000002"] == 2
    assert [code for code, _ in FakeSectorRepo.calls] == ["000001", "000003", "000002"]
    assert result["rows_written"] == 3
    assert FakeClient.closed == 3


def test_fetch_batch_marks_whole_batch_failed_when_connect_fails(monkeypatch) -> None:
    class BrokenClient:
        def connect(self) -> None:
            raise RuntimeError("unable to connect")

        def close(self) -> None:
            return None

    monkeypatch.setattr(sync_stock_sectors, "create_eltdx_client", lambda **kwargs: BrokenClient())

    result = sync_stock_sectors._fetch_batch(
        ["000001", "000002"],
        batch_index=0,
        pool_size=1,
        timeout=1,
        workers=1,
        hosts=["127.0.0.1:7709"],
    )

    assert set(result) == {"000001", "000002"}
    assert all(item["ok"] is False for item in result.values())
    assert all("unable to connect" in item["error"] for item in result.values())
