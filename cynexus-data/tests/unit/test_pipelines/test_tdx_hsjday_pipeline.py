from __future__ import annotations

import struct
import zipfile
from datetime import date

from cynexus_data.jobs.contracts import JobRequest
import cynexus_data.pipelines.kline.sync_tdx_hsjday as sync_tdx_hsjday
from cynexus_data.pipelines.kline.sync_tdx_hsjday import run
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.hsjday import HsjdayDownloadResult


def _day_record(trade_date: int) -> bytes:
    return struct.pack("<IIIIIfII", trade_date, 10000, 11000, 9000, 10500, 1000.0, 100, 0)


def _write_hsjday_zip(path) -> None:
    with zipfile.ZipFile(path, "w") as archive:
        for market, codes in {
            "sh": ["000001"],
            "sz": ["399001", "399006"],
            "bj": ["899050"],
        }.items():
            for code in codes:
                archive.writestr(f"hsjday/{market}/lday/{market}{code}.day", _day_record(20260626))


def test_sync_tdx_hsjday_pipeline_skip_download_replaces_target(tmp_path) -> None:
    zip_path = tmp_path / "download" / "hsjday.zip"
    zip_path.parent.mkdir(parents=True)
    _write_hsjday_zip(zip_path)
    target_root = tmp_path / "target" / "hsjday"

    result = run(
        PipelineContext(),
        JobRequest(
            job_key="sync_tdx_hsjday",
            dry_run=False,
            params={
                "date": "2026-06-26",
                "zip_path": str(zip_path),
                "target_root": str(target_root),
                "extract_dir": str(tmp_path / "extract"),
                "skip_download": True,
                "keep_zip": True,
            },
        ),
    )

    assert result["rows_written"] == 4
    assert (target_root / "sh" / "lday" / "sh000001.day").exists()
    assert (target_root / "sz" / "lday" / "sz399001.day").exists()
    assert (target_root / "bj" / "lday" / "bj899050.day").exists()


def test_sync_tdx_hsjday_pipeline_passes_download_method(monkeypatch, tmp_path) -> None:
    zip_path = tmp_path / "download" / "hsjday.zip"
    captured: dict[str, object] = {}

    def fake_download(
        url,
        destination,
        timeout_seconds,
        method,
        playwright_headless,
        playwright_wait_ms,
        chunk_size_bytes,
        urlopen_attempts,
        urlopen_backoff_seconds,
    ):
        captured.update(
            {
                "url": url,
                "destination": destination,
                "timeout_seconds": timeout_seconds,
                "method": method,
                "playwright_headless": playwright_headless,
                "playwright_wait_ms": playwright_wait_ms,
                "chunk_size_bytes": chunk_size_bytes,
                "urlopen_attempts": urlopen_attempts,
                "urlopen_backoff_seconds": urlopen_backoff_seconds,
            }
        )
        destination.parent.mkdir(parents=True)
        _write_hsjday_zip(destination)
        return HsjdayDownloadResult(bytes_downloaded=destination.stat().st_size, method="playwright")

    monkeypatch.setattr(sync_tdx_hsjday, "download_hsjday_zip", fake_download)

    result = run(
        PipelineContext(),
        JobRequest(
            job_key="sync_tdx_hsjday",
            dry_run=False,
            params={
                "date": "2026-06-26",
                "url": "https://example.test/hsjday.zip",
                "zip_path": str(zip_path),
                "target_root": str(tmp_path / "target" / "hsjday"),
                "extract_dir": str(tmp_path / "extract"),
                "download_method": "auto",
                "playwright_headless": False,
                "playwright_wait_ms": 123,
                "chunk_size_bytes": 1024,
                "keep_zip": True,
            },
        ),
    )

    assert captured["method"] == "auto"
    assert captured["playwright_headless"] is False
    assert captured["playwright_wait_ms"] == 123
    assert captured["chunk_size_bytes"] == 1024
    assert captured["urlopen_attempts"] == 3
    assert captured["urlopen_backoff_seconds"] == 1.0
    download_step = next(step for step in result["steps"] if step["step"] == "download")
    assert download_step["method"] == "playwright"
def test_last_weekday_rolls_weekend_back_to_friday() -> None:
    assert sync_tdx_hsjday._last_weekday(date(2026, 8, 29)) == date(2026, 8, 28)
    assert sync_tdx_hsjday._last_weekday(date(2026, 8, 28)) == date(2026, 8, 28)
