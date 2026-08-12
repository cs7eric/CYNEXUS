from __future__ import annotations

from datetime import date

from cynexus_data.pipelines.kline import rebuild_partitions
from cynexus_data.pipelines.kline.rebuild_partitions import (
    _preserve_non_parquet_target_scopes,
    _read_rebuild_file,
    _resolve_ingestion_settings,
)


def test_execute_rebuild_clamps_arrow_buffers_and_parallelism_by_default() -> None:
    settings = _resolve_ingestion_settings(
        {
            "workers": 2,
            "batch_rows": 100_000,
            "flush_rows": 1_000_000,
            "max_partitions_per_insert_block": 300,
        },
        years=[2018, 2020, 2021, 2022],
        execute=True,
    )

    assert settings == {
        "workers": 2,
        "batch_rows": 25_000,
        "flush_rows": 100_000,
        "max_partitions": 4,
        "safe_memory_mode": True,
        "requested_workers": 2,
        "requested_batch_rows": 100_000,
        "requested_flush_rows": 1_000_000,
    }


def test_rebuild_allows_explicit_high_memory_maintenance_override() -> None:
    settings = _resolve_ingestion_settings(
        {"workers": 2, "batch_rows": 100_000, "flush_rows": 1_000_000, "allow_high_memory": True},
        years=[2018],
        execute=True,
    )

    assert settings["workers"] == 2
    assert settings["batch_rows"] == 100_000
    assert settings["flush_rows"] == 1_000_000
    assert settings["safe_memory_mode"] is False


def test_single_year_rebuild_uses_parquet_date_predicate_pushdown(monkeypatch) -> None:
    captured = {}

    def fake_reader(file, **kwargs):
        captured["file"] = file
        captured.update(kwargs)
        return "table"

    monkeypatch.setattr(rebuild_partitions, "read_parquet_1m_arrow", fake_reader)

    assert _read_rebuild_file("fake-file", [2021]) == "table"
    assert captured == {
        "file": "fake-file",
        "start_date": date(2021, 1, 1),
        "end_date": date(2021, 12, 31),
    }


def test_multi_year_rebuild_keeps_explicit_year_filter(monkeypatch) -> None:
    captured = {}

    def fake_reader(file, **kwargs):
        captured["file"] = file
        captured.update(kwargs)
        return "table"

    monkeypatch.setattr(rebuild_partitions, "read_parquet_1m_arrow", fake_reader)

    assert _read_rebuild_file("fake-file", [2020, 2022]) == "table"
    assert captured == {"file": "fake-file", "years": {2020, 2022}}


class _PreserveClient:
    def __init__(self) -> None:
        self.commands = []

    def command(self, sql, parameters=None, **kwargs):
        self.commands.append((sql, parameters, kwargs))

    def query_rows(self, sql):
        return [(2026, "mootdx", 1, 240), (2026, "tdx_local", 1, 73_871_655), (2026, "tencent", 1, 18_547)]


def test_partition_rebuild_preserves_non_parquet_provider_scopes_before_replace() -> None:
    client = _PreserveClient()
    target_summary = {
        "unexpected_scopes": [
            {"year": 2026, "source": "mootdx", "period": 1, "rows": 240},
            {"year": 2026, "source": "tdx_local", "period": 1, "rows": 73_871_655},
            {"year": 2026, "source": "tencent", "period": 1, "rows": 18_547},
        ]
    }

    preserved = _preserve_non_parquet_target_scopes(
        client,
        target_table="cynexus.ch_dwd_kline_1m",
        stage_table="cynexus.stage_2026",
        years=[2026],
        target_summary=target_summary,
        execute=True,
    )

    assert preserved == {"2026": target_summary["unexpected_scopes"]}
    sql, _, kwargs = client.commands[0]
    assert "INSERT INTO cynexus.stage_2026" in sql
    assert "NOT (source = 'parquet_1m' AND period = 1)" in sql
    assert kwargs["settings"]["max_threads"] == 1
