from __future__ import annotations

import struct

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.kline.sync_daily import _file_checkpoint_key, run
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.local_day import discover_day_files


class FakeTdxHook:
    def __init__(self, root) -> None:
        self.root = root

    def get_root_path(self):
        return self.root


class ExplodingClickHouseHook:
    def get_config(self):  # pragma: no cover - should not be reached in dry-run
        raise AssertionError("dry-run should not create ClickHouse sink")


class FakeHooks:
    def __init__(self, root) -> None:
        self.tdx = FakeTdxHook(root)
        self.clickhouse = ExplodingClickHouseHook()


def _write_day(path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(struct.pack("<IIIIIfII", 20260626, 10000, 11000, 9000, 10500, 1000.0, 100, 0))


def test_sync_daily_dry_run_discovers_tdx_day_files(tmp_path) -> None:
    _write_day(tmp_path / "vipdoc" / "sh" / "lday" / "sh600000.day")
    context = PipelineContext(hooks=FakeHooks(tmp_path))

    result = run(context, JobRequest(job_key="sync_kline_daily", dry_run=True, params={"market": "sh"}))

    assert result["providers"] == ["tencent", "eastmoney", "tdx"]
    assert result["discovered_files"] == 1
    assert result["rows_written"] == 0


class FakeSink:
    frames = []
    init_tables = []
    insert_replace_existing = []

    def __init__(self, config, table="daily_raw") -> None:
        self.config = config
        self.table = table
        FakeSink.init_tables.append(table)

    def create_table(self) -> None:
        pass

    def insert_daily(self, frame, batch_size=100_000, *, replace_existing=True):
        FakeSink.frames.append(frame)
        FakeSink.insert_replace_existing.append(replace_existing)
        return len(frame)


class FakeClickHouseHook:
    def get_config(self):
        return object()


class FakeNetworkHooks:
    def __init__(self, root) -> None:
        self.tdx = FakeTdxHook(root)
        self.clickhouse = FakeClickHouseHook()


def test_sync_daily_uses_network_provider_fallback(monkeypatch, tmp_path) -> None:
    FakeSink.frames = []
    FakeSink.init_tables = []
    FakeSink.insert_replace_existing = []
    monkeypatch.setattr("cynexus_data.pipelines.kline.sync_daily.ClickHouseDailyKlineSink", FakeSink)
    monkeypatch.setattr(
        "cynexus_data.pipelines.kline.sync_daily.fetch_daily_kline_with_fallback",
        lambda hooks, symbol, providers, adjust, limit: type(
            "Fetched",
            (),
            {
                "items": [{"trade_date": "2026-06-26", "open": 10, "high": 12, "low": 9, "close": 11, "volume": 100, "amount": 1000}],
                "source": "eastmoney",
                "meta": lambda self: {"source": "eastmoney", "stale": False, "dataQuality": "ok", "attempts": []},
            },
        )(),
    )
    context = PipelineContext(hooks=FakeNetworkHooks(tmp_path))

    result = run(context, JobRequest(job_key="sync_kline_daily", dry_run=False, params={"providers": "eastmoney,tencent", "codes": "600000"}))

    assert result["rows_written"] == 1
    assert result["provider_results"][0]["source"] == "eastmoney"
    assert FakeSink.frames[0].iloc[0]["symbol"] == "600000"
    assert FakeSink.init_tables == ["daily_raw"]
    assert FakeSink.insert_replace_existing == [True]


def test_sync_daily_resume_skips_completed_file(monkeypatch, tmp_path) -> None:
    file_path = tmp_path / "vipdoc" / "sh" / "lday" / "sh600000.day"
    _write_day(file_path)
    FakeSink.frames = []
    FakeSink.init_tables = []
    FakeSink.insert_replace_existing = []
    monkeypatch.setattr("cynexus_data.pipelines.kline.sync_daily.ClickHouseDailyKlineSink", FakeSink)
    context = PipelineContext(hooks=FakeNetworkHooks(tmp_path))
    discovered = discover_day_files(tmp_path, markets=["sh"])
    context.checkpoints.put(_file_checkpoint_key("kline.sync_daily", discovered[0], None, None), "sync_kline_daily", {"status": "completed"})

    result = run(context, JobRequest(job_key="sync_kline_daily", dry_run=False, params={"market": "sh", "resume": True}))

    assert result["skipped_checkpoints"] == 1
    assert result["rows_written"] == 0
    assert FakeSink.frames == []


def test_sync_daily_can_write_rebuild_table_without_replace(monkeypatch, tmp_path) -> None:
    file_path = tmp_path / "vipdoc" / "sh" / "lday" / "sh600000.day"
    _write_day(file_path)
    FakeSink.frames = []
    FakeSink.init_tables = []
    FakeSink.insert_replace_existing = []
    monkeypatch.setattr("cynexus_data.pipelines.kline.sync_daily.ClickHouseDailyKlineSink", FakeSink)
    context = PipelineContext(hooks=FakeNetworkHooks(tmp_path))

    result = run(
        context,
        JobRequest(
            job_key="sync_kline_daily",
            dry_run=False,
            params={"market": "sh", "target_table": "daily_raw_rebuild", "replace_existing": False},
        ),
    )

    assert result["target_table"] == "daily_raw_rebuild"
    assert result["replace_existing"] is False
    assert result["rows_written"] == 1
    assert FakeSink.init_tables == ["daily_raw_rebuild"]
    assert FakeSink.insert_replace_existing == [False]


def test_sync_daily_can_flush_multiple_files_in_one_batch(monkeypatch, tmp_path) -> None:
    _write_day(tmp_path / "vipdoc" / "sh" / "lday" / "sh600000.day")
    _write_day(tmp_path / "vipdoc" / "sh" / "lday" / "sh600001.day")
    FakeSink.frames = []
    FakeSink.init_tables = []
    FakeSink.insert_replace_existing = []
    monkeypatch.setattr("cynexus_data.pipelines.kline.sync_daily.ClickHouseDailyKlineSink", FakeSink)
    context = PipelineContext(hooks=FakeNetworkHooks(tmp_path))

    result = run(
        context,
        JobRequest(
            job_key="sync_kline_daily",
            dry_run=False,
            params={"market": "sh", "flush_rows": 10, "replace_existing": False},
        ),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 2
    assert result["batch_results"] == [{"files": 2, "rows_read": 2, "rows_written": 2}]
    assert len(FakeSink.frames) == 1
    assert set(FakeSink.frames[0]["code"]) == {"600000", "600001"}
    assert FakeSink.insert_replace_existing == [False]
