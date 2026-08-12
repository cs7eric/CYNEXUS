from __future__ import annotations

import struct
from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.kline.sync_minute import run
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.local_minute import parse_tdx_minute_file, read_tdx_1m_file


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


def test_sync_minute_dry_run_discovers_no_files_without_clickhouse(tmp_path) -> None:
    context = PipelineContext(hooks=FakeHooks(tmp_path))

    result = run(context, JobRequest(job_key="sync_kline_minute", dry_run=True, params={}))

    assert result["providers"] == ["tdx", "easy_tdx", "tencent", "mootdx", "sina", "eastmoney"]
    assert result["discovered_files"] == 0
    assert result["rows_written"] == 0


def test_sync_minute_incremental_starts_at_latest_source_date_without_storage_state(tmp_path) -> None:
    path = tmp_path / "vipdoc" / "sh" / "minline" / "sh000001.lc1"
    path.parent.mkdir(parents=True)
    date_code = (2026 - 2004) * 2048 + 8 * 100 + 6
    path.write_bytes(struct.pack("<HHfffffII", date_code, 570, 1.0, 2.0, 0.5, 1.5, 100.0, 10, 0))

    context = PipelineContext(hooks=FakeHooks(tmp_path))
    result = run(
        context,
        JobRequest(job_key="sync_kline_minute", dry_run=True, params={"providers": ["tdx"]}),
    )

    assert result["incremental"] is True
    assert result["effective_trade_date"] is None
    assert result["start_date"] == "2026-08-06"
    assert result["end_date"] == "2026-08-06"


def test_sync_minute_incremental_resumes_after_effective_clickhouse_date(tmp_path, monkeypatch) -> None:
    path = tmp_path / "vipdoc" / "sh" / "minline" / "sh000001.lc1"
    path.parent.mkdir(parents=True)
    date_code = (2026 - 2004) * 2048 + 8 * 100 + 6
    path.write_bytes(struct.pack("<HHfffffII", date_code, 570, 1.0, 2.0, 0.5, 1.5, 100.0, 10, 0))

    class FakeSink:
        def __init__(self, _config) -> None:
            self.client = object()
            self.deleted = None

        def create_table(self) -> None:
            pass

        def insert_1m(self, frame, *, batch_size, replace_existing):
            return len(frame)

        def delete_1m_range(self, start_date, end_date, *, source, period, tdx_codes):
            self.deleted = (start_date, end_date, source, period, tdx_codes)

    monkeypatch.setattr("cynexus_data.pipelines.kline.sync_minute.ClickHouseKlineSink", FakeSink)
    monkeypatch.setattr(
        "cynexus_data.pipelines.kline.sync_minute.get_latest_effective_kline_trade_date",
        lambda _client, *, expected_symbols: date(2026, 7, 27),
    )

    class Hooks(FakeHooks):
        class ClickHouse:
            def get_config(self):
                return object()

        def __init__(self, root) -> None:
            self.tdx = FakeTdxHook(root)
            self.clickhouse = self.ClickHouse()

    result = run(
        PipelineContext(hooks=Hooks(tmp_path)),
        JobRequest(job_key="sync_kline_minute", params={"providers": ["tdx"]}),
    )

    assert result["effective_trade_date"] == "2026-07-27"
    assert result["start_date"] == "2026-07-28"
    assert result["end_date"] == "2026-08-06"
    assert result["rows_written"] == 1


def test_sync_minute_incremental_is_a_noop_when_effective_date_is_current(tmp_path, monkeypatch) -> None:
    path = tmp_path / "vipdoc" / "sh" / "minline" / "sh000001.lc1"
    path.parent.mkdir(parents=True)
    date_code = (2026 - 2004) * 2048 + 8 * 100 + 6
    path.write_bytes(struct.pack("<HHfffffII", date_code, 570, 1.0, 2.0, 0.5, 1.5, 100.0, 10, 0))

    class FakeSink:
        instances = []

        def __init__(self, _config) -> None:
            self.client = object()
            self.delete_calls = 0
            self.insert_calls = 0
            self.__class__.instances.append(self)

        def delete_1m_range(self, *_args, **_kwargs):
            self.delete_calls += 1

        def insert_1m(self, *_args, **_kwargs):
            self.insert_calls += 1
            return 0

    monkeypatch.setattr("cynexus_data.pipelines.kline.sync_minute.ClickHouseKlineSink", FakeSink)
    monkeypatch.setattr(
        "cynexus_data.pipelines.kline.sync_minute.get_latest_effective_kline_trade_date",
        lambda _client, *, expected_symbols: date(2026, 8, 6),
    )

    class Hooks(FakeHooks):
        class ClickHouse:
            def get_config(self):
                return object()

        def __init__(self, root) -> None:
            self.tdx = FakeTdxHook(root)
            self.clickhouse = self.ClickHouse()

    result = run(
        PipelineContext(hooks=Hooks(tmp_path)),
        JobRequest(job_key="sync_kline_minute", params={"providers": ["tdx"], "replace_existing": True}),
    )

    assert result["status"] == "up_to_date"
    assert result["reason"] == "effective_trade_date_is_not_behind_latest_source_date"
    assert result["start_date"] == "2026-08-07"
    assert result["end_date"] == "2026-08-06"
    assert FakeSink.instances[0].delete_calls == 0
    assert FakeSink.instances[0].insert_calls == 0


def test_read_tdx_minute_file_reads_only_requested_dates(tmp_path) -> None:
    path = tmp_path / "vipdoc" / "sh" / "minline" / "sh000001.lc1"
    path.parent.mkdir(parents=True)
    records = []
    for day, minute in [(5, 570), (5, 571), (6, 570)]:
        date_code = (2026 - 2004) * 2048 + 8 * 100 + day
        records.append(struct.pack("<HHfffffII", date_code, minute, 1.0, 2.0, 0.5, 1.5, 100.0, 10, 0))
    path.write_bytes(b"".join(records))

    frame = read_tdx_1m_file(
        parse_tdx_minute_file(path),
        start_date=date(2026, 8, 5),
        end_date=date(2026, 8, 5),
    )

    assert len(frame) == 2
    assert frame["trade_date"].tolist() == [date(2026, 8, 5), date(2026, 8, 5)]
    assert frame["trade_time"].iloc[-1].minute == 31

    assert read_tdx_1m_file(
        parse_tdx_minute_file(path),
        start_date=date(2026, 8, 7),
        end_date=date(2026, 8, 7),
    ).empty
