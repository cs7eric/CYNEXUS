from __future__ import annotations

import ssl
import struct
import sys
import zipfile
from datetime import date
from types import ModuleType
from urllib.error import URLError

import pytest

import cynexus_data.sources.tdx.hsjday as hsjday
from cynexus_data.sources.tdx.hsjday import (
    HsjdayDownloadChallenge,
    inspect_zip_freshness,
    replace_hsjday_root,
    safe_extract_hsjday_zip,
    verify_hsjday_root,
)


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


def test_inspect_extract_and_verify_hsjday_zip(tmp_path) -> None:
    zip_path = tmp_path / "hsjday.zip"
    _write_hsjday_zip(zip_path)

    zip_check = inspect_zip_freshness(zip_path, date(2026, 6, 26))
    extracted_root = safe_extract_hsjday_zip(zip_path, tmp_path / "extract")
    root_check = verify_hsjday_root(extracted_root, date(2026, 6, 26))

    assert zip_check.ok is True
    assert root_check.ok is True
    assert root_check.per_market["sh"]["files"] == 1
    assert root_check.per_market["sz"]["files"] == 2
    assert root_check.per_market["bj"]["files"] == 1


def test_inspect_hsjday_zip_ignores_stale_non_sample_bj_stocks(tmp_path) -> None:
    zip_path = tmp_path / "hsjday.zip"
    _write_hsjday_zip(zip_path)
    with zipfile.ZipFile(zip_path, "a") as archive:
        archive.writestr("hsjday/bj/lday/bj430047.day", _day_record(20250930))
        archive.writestr("hsjday/bj/lday/bj830799.day", _day_record(20250430))

    zip_check = inspect_zip_freshness(zip_path, date(2026, 6, 26))

    assert zip_check.ok is True
    assert zip_check.errors == []


def test_hsjday_zip_supports_backslash_members(tmp_path) -> None:
    zip_path = tmp_path / "hsjday.zip"
    with zipfile.ZipFile(zip_path, "w") as archive:
        for market, codes in {
            "sh": ["000001"],
            "sz": ["399001", "399006"],
            "bj": ["899050"],
        }.items():
            for code in codes:
                archive.writestr(f"{market}\\lday\\{market}{code}.day", _day_record(20260626))

    zip_check = inspect_zip_freshness(zip_path, date(2026, 6, 26))
    extracted_root = safe_extract_hsjday_zip(zip_path, tmp_path / "extract")
    root_check = verify_hsjday_root(extracted_root, date(2026, 6, 26))

    assert zip_check.ok is True
    assert root_check.ok is True
    assert (extracted_root / "sh" / "lday" / "sh000001.day").exists()


def test_replace_hsjday_root_does_not_depend_on_fixed_backup_path(tmp_path) -> None:
    target_root = tmp_path / "hsjday"
    (target_root / "sh" / "lday").mkdir(parents=True)
    (target_root / "sh" / "lday" / "old.day").write_bytes(b"old")
    legacy_backup = tmp_path / "hsjday.bak" / "sh" / "lday"
    legacy_backup.mkdir(parents=True)
    (legacy_backup / "stale.day").write_bytes(b"stale")

    new_root = tmp_path / "hsjday-new"
    (new_root / "sh" / "lday").mkdir(parents=True)
    (new_root / "sh" / "lday" / "new.day").write_bytes(b"new")

    replace_hsjday_root(new_root, target_root)

    assert (target_root / "sh" / "lday" / "new.day").read_bytes() == b"new"
    assert (tmp_path / "hsjday.bak").exists()


class _FakeUrlopenResponse:
    def __init__(self, chunks: list[bytes]) -> None:
        self._chunks = chunks

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback) -> None:
        return None

    def read(self, size: int = -1) -> bytes:
        if not self._chunks:
            return b""
        return self._chunks.pop(0)


def test_download_hsjday_zip_urlopen_detects_js_challenge(monkeypatch, tmp_path) -> None:
    def fake_urlopen(request, timeout):
        return _FakeUrlopenResponse([b"<script>document.cookie='x'</script>"])

    monkeypatch.setattr(hsjday, "urlopen", fake_urlopen)

    with pytest.raises(HsjdayDownloadChallenge):
        hsjday.download_hsjday_zip("https://example.test/hsjday.zip", tmp_path / "hsjday.zip", method="urlopen")


def test_download_hsjday_zip_urlopen_retries_ssl_eof(monkeypatch, tmp_path) -> None:
    calls = 0

    def fake_urlopen(request, timeout):
        nonlocal calls
        calls += 1
        if calls == 1:
            raise URLError(ssl.SSLEOFError("EOF occurred in violation of protocol"))
        return _FakeUrlopenResponse([b"PK retry ok"])

    monkeypatch.setattr(hsjday, "urlopen", fake_urlopen)

    result = hsjday.download_hsjday_zip(
        "https://example.test/hsjday.zip",
        tmp_path / "hsjday.zip",
        method="urlopen",
        urlopen_attempts=2,
        urlopen_backoff_seconds=0,
    )

    assert result.bytes_downloaded == 11
    assert result.method == "urlopen"
    assert calls == 2
    assert (tmp_path / "hsjday.zip").read_bytes() == b"PK retry ok"


def test_download_hsjday_zip_auto_falls_back_to_playwright(monkeypatch, tmp_path) -> None:
    calls: list[str] = []

    def fake_urlopen_download(url, destination, timeout_seconds=120, attempts=3, backoff_seconds=1.0):
        calls.append("urlopen")
        raise HsjdayDownloadChallenge("challenge")

    def fake_playwright_download(url, destination, timeout_seconds=120, headless=True, wait_ms=2000, chunk_size_bytes=1):
        calls.append("playwright")
        destination.write_bytes(b"PK fake")
        return 7

    monkeypatch.setattr(hsjday, "_download_hsjday_zip_urlopen", fake_urlopen_download)
    monkeypatch.setattr(hsjday, "_download_hsjday_zip_playwright", fake_playwright_download)

    result = hsjday.download_hsjday_zip(
        "https://example.test/hsjday.zip",
        tmp_path / "hsjday.zip",
        method="auto",
        chunk_size_bytes=1,
    )

    assert result.bytes_downloaded == 7
    assert result.method == "playwright"
    assert calls == ["urlopen", "playwright"]


def test_playwright_download_uses_configured_system_browser(monkeypatch, tmp_path) -> None:
    launched_with: dict[str, object] = {}

    class FakePage:
        def goto(self, *args, **kwargs):
            return None

        def wait_for_timeout(self, *args, **kwargs):
            return None

    class FakeContext:
        request = object()

        def new_page(self):
            return FakePage()

        def close(self):
            return None

    class FakeBrowser:
        def new_context(self, **kwargs):
            return FakeContext()

        def close(self):
            return None

    class FakeChromium:
        def launch(self, **kwargs):
            launched_with.update(kwargs)
            return FakeBrowser()

    class FakePlaywright:
        chromium = FakeChromium()

    class FakePlaywrightContext:
        def __enter__(self):
            return FakePlaywright()

        def __exit__(self, exc_type, exc, traceback):
            return None

    monkeypatch.setenv("CYNEXUS_PLAYWRIGHT_EXECUTABLE_PATH", "/usr/bin/chromium")
    playwright_package = ModuleType("playwright")
    playwright_api = ModuleType("playwright.sync_api")
    playwright_api.sync_playwright = lambda: FakePlaywrightContext()
    monkeypatch.setitem(sys.modules, "playwright", playwright_package)
    monkeypatch.setitem(sys.modules, "playwright.sync_api", playwright_api)

    def fake_ranges(request_context, url, destination, **kwargs):
        destination.write_bytes(b"PK fake")
        return 7

    monkeypatch.setattr(hsjday, "_download_hsjday_zip_playwright_ranges", fake_ranges)

    result = hsjday._download_hsjday_zip_playwright(
        "https://example.test/hsjday.zip",
        tmp_path / "hsjday.zip",
        wait_ms=0,
    )

    assert result == 7
    assert launched_with == {"headless": True, "executable_path": "/usr/bin/chromium"}
