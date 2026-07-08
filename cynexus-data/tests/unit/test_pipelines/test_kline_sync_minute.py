from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.kline.sync_minute import run
from cynexus_data.runtime.context import PipelineContext


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

    assert result["providers"] == ["tdx", "tencent", "mootdx", "sina", "eastmoney"]
    assert result["discovered_files"] == 0
    assert result["rows_written"] == 0
