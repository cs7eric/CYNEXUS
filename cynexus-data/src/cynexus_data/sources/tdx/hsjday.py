from __future__ import annotations

import shutil
import struct
import zipfile
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from urllib.request import Request, urlopen


TDX_HSJDAY_URL = "https://data.tdx.com.cn/vipdoc/hsjday.zip"
VERIFY_SAMPLES = {"sh": ["000001"], "sz": ["399001"]}
DEFAULT_CHUNK_SIZE_BYTES = 4 * 1024 * 1024


class HsjdayDownloadChallenge(RuntimeError):
    """Raised when TDX returns a JavaScript challenge page instead of zip bytes."""


@dataclass(frozen=True)
class HsjdayVerifyResult:
    ok: bool
    per_market: dict[str, dict[str, int]]
    samples: list[dict[str, object]]
    errors: list[str]


@dataclass(frozen=True)
class HsjdayDownloadResult:
    bytes_downloaded: int
    method: str


def download_hsjday_zip(
    url: str,
    destination: Path,
    timeout_seconds: int = 120,
    method: str = "auto",
    playwright_headless: bool = True,
    playwright_wait_ms: int = 2000,
    chunk_size_bytes: int = DEFAULT_CHUNK_SIZE_BYTES,
) -> HsjdayDownloadResult:
    normalized_method = method.lower()
    if normalized_method not in {"auto", "urlopen", "playwright"}:
        raise ValueError("method must be one of: auto, urlopen, playwright")

    if normalized_method == "playwright":
        total = _download_hsjday_zip_playwright(
            url,
            destination,
            timeout_seconds=timeout_seconds,
            headless=playwright_headless,
            wait_ms=playwright_wait_ms,
            chunk_size_bytes=chunk_size_bytes,
        )
        return HsjdayDownloadResult(bytes_downloaded=total, method="playwright")

    try:
        total = _download_hsjday_zip_urlopen(url, destination, timeout_seconds=timeout_seconds)
        return HsjdayDownloadResult(bytes_downloaded=total, method="urlopen")
    except HsjdayDownloadChallenge:
        if normalized_method == "urlopen":
            raise
        total = _download_hsjday_zip_playwright(
            url,
            destination,
            timeout_seconds=timeout_seconds,
            headless=playwright_headless,
            wait_ms=playwright_wait_ms,
            chunk_size_bytes=chunk_size_bytes,
        )
        return HsjdayDownloadResult(bytes_downloaded=total, method="playwright")


def _download_hsjday_zip_urlopen(url: str, destination: Path, timeout_seconds: int = 120) -> int:
    destination.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = destination.with_suffix(f"{destination.suffix}.tmp")
    if tmp_path.exists():
        tmp_path.unlink()
    request = Request(
        url,
        headers={
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
            "Referer": "https://www.tdx.com.cn/",
            "Cache-Control": "no-cache",
            "Pragma": "no-cache",
        },
    )
    try:
        with urlopen(request, timeout=timeout_seconds) as response, tmp_path.open("wb") as handle:
            total = 0
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                if total == 0 and _looks_like_js_challenge(chunk):
                    raise HsjdayDownloadChallenge("TDX returned a JavaScript challenge page instead of hsjday.zip")
                handle.write(chunk)
                total += len(chunk)
    except Exception:
        if tmp_path.exists():
            tmp_path.unlink()
        raise
    tmp_path.replace(destination)
    return total


def _download_hsjday_zip_playwright(
    url: str,
    destination: Path,
    timeout_seconds: int = 120,
    headless: bool = True,
    wait_ms: int = 2000,
    chunk_size_bytes: int = DEFAULT_CHUNK_SIZE_BYTES,
) -> int:
    try:
        from playwright.sync_api import sync_playwright
    except ImportError as exc:  # pragma: no cover - exercised by integration/runtime environments
        raise RuntimeError(
            "TDX hsjday download hit a JavaScript challenge. Install optional Playwright runtime with "
            "`pip install playwright` and `playwright install chromium`, or manually download hsjday.zip "
            "and run the job with skip_download=true."
        ) from exc

    destination.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = destination.with_suffix(f"{destination.suffix}.tmp")
    if tmp_path.exists():
        tmp_path.unlink()

    try:
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(headless=headless)
            context = None
            try:
                context = browser.new_context(
                    user_agent=(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        "(KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
                    ),
                    viewport={"width": 1280, "height": 800},
                )
                page = context.new_page()
                page.goto("https://www.tdx.com.cn/", wait_until="domcontentloaded", timeout=timeout_seconds * 1000)
                if wait_ms > 0:
                    page.wait_for_timeout(wait_ms)

                total = _download_hsjday_zip_playwright_ranges(
                    context.request,
                    url,
                    tmp_path,
                    timeout_seconds=timeout_seconds,
                    chunk_size_bytes=chunk_size_bytes,
                )
            finally:
                if context is not None:
                    context.close()
                browser.close()
    except Exception:
        if tmp_path.exists():
            tmp_path.unlink()
        raise

    tmp_path.replace(destination)
    return total


def _download_hsjday_zip_playwright_ranges(
    request_context,
    url: str,
    destination: Path,
    timeout_seconds: int = 120,
    chunk_size_bytes: int = DEFAULT_CHUNK_SIZE_BYTES,
) -> int:
    first = request_context.get(
        url,
        headers={"Range": "bytes=0-0", "Cache-Control": "no-cache", "Pragma": "no-cache"},
        timeout=timeout_seconds * 1000,
    )
    if first.status not in (200, 206):
        raise RuntimeError(f"TDX hsjday browser request HTTP {first.status}")

    if first.status == 206:
        content_range = first.headers.get("content-range") or first.headers.get("Content-Range") or ""
        total = _parse_content_range_total(content_range)
        if total <= 0:
            raise RuntimeError(f"Cannot determine hsjday.zip size from Content-Range: {content_range!r}")
        return _download_ranges(request_context, url, destination, total, timeout_seconds, chunk_size_bytes)

    body = first.body()
    if _looks_like_js_challenge(body):
        raise HsjdayDownloadChallenge("TDX returned a JavaScript challenge page through Playwright request")
    destination.write_bytes(body)
    return len(body)


def _download_ranges(
    request_context,
    url: str,
    destination: Path,
    total: int,
    timeout_seconds: int,
    chunk_size_bytes: int,
) -> int:
    written = 0
    with destination.open("wb") as handle:
        while written < total:
            end = min(written + chunk_size_bytes - 1, total - 1)
            response = request_context.get(
                url,
                headers={
                    "Range": f"bytes={written}-{end}",
                    "Cache-Control": "no-cache",
                    "Pragma": "no-cache",
                },
                timeout=timeout_seconds * 1000,
            )
            if response.status not in (200, 206):
                raise RuntimeError(f"TDX hsjday range request HTTP {response.status}")
            data = response.body()
            if written == 0 and _looks_like_js_challenge(data):
                raise HsjdayDownloadChallenge("TDX returned a JavaScript challenge page during range download")
            if not data:
                raise RuntimeError(f"TDX hsjday range request returned empty body at offset {written}")
            handle.write(data)
            written += len(data)
            if response.status == 200:
                break
    return written


def inspect_zip_freshness(zip_path: Path, target_date: date) -> HsjdayVerifyResult:
    errors: list[str] = []
    samples: list[dict[str, object]] = []
    per_market: dict[str, dict[str, int]] = {}
    target_int = _date_int(target_date)
    try:
        with zipfile.ZipFile(zip_path) as archive:
            names = archive.namelist()
            name_set = set(names)
            for market in ("sh", "sz", "bj"):
                market_files = [name for name in names if _zip_day_member_market(name) == market]
                per_market[market] = {"files": len(market_files), "bytes": 0}
            has_hsjday_root = any(name.startswith("hsjday/") for name in names)
            for market, codes in VERIFY_SAMPLES.items():
                for code in codes:
                    member = f"{market}/lday/{market}{code}.day"
                    if has_hsjday_root:
                        member = f"hsjday/{member}"
                    if member not in name_set:
                        error = f"{member} not found"
                        errors.append(error)
                        samples.append({"market": market, "code": code, "ok": False, "error": error})
                        continue
                    with archive.open(member) as handle:
                        raw = handle.read()
                    first_date, last_date = read_day_first_last(raw)
                    ok = last_date is not None and last_date >= target_int
                    if not ok:
                        errors.append(f"{member} last_date={last_date} < target={target_int}")
                    samples.append({"market": market, "code": code, "first_date": first_date, "last_date": last_date, "ok": ok})
    except Exception as exc:  # noqa: BLE001
        return HsjdayVerifyResult(ok=False, per_market={}, samples=[], errors=[f"{type(exc).__name__}: {exc}"])
    return HsjdayVerifyResult(ok=not errors, per_market=per_market, samples=samples, errors=errors)


def safe_extract_hsjday_zip(zip_path: Path, extract_dir: Path) -> Path:
    if extract_dir.exists():
        shutil.rmtree(extract_dir)
    extract_dir.mkdir(parents=True, exist_ok=True)
    base = extract_dir.resolve()
    with zipfile.ZipFile(zip_path) as archive:
        for member in archive.infolist():
            target = (extract_dir / member.filename).resolve()
            if not str(target).startswith(str(base)):
                raise ValueError(f"unsafe zip member path: {member.filename}")
            archive.extract(member, extract_dir)
    return find_hsjday_root(extract_dir)


def find_hsjday_root(extracted_dir: Path) -> Path:
    candidates = [extracted_dir / "hsjday", extracted_dir]
    for candidate in candidates:
        if any((candidate / market / "lday").is_dir() for market in ("sh", "sz", "bj")):
            return candidate
    sample = next(extracted_dir.rglob("*.day"), None)
    if sample is not None:
        for ancestor in sample.parents:
            if ancestor.name == "lday":
                return ancestor.parent.parent
    raise FileNotFoundError(f"Cannot find hsjday root under {extracted_dir}")


def verify_hsjday_root(root: Path, target_date: date) -> HsjdayVerifyResult:
    target_int = _date_int(target_date)
    per_market: dict[str, dict[str, int]] = {}
    samples: list[dict[str, object]] = []
    errors: list[str] = []
    for market in ("sh", "sz", "bj"):
        lday = root / market / "lday"
        files = list(lday.glob(f"{market}*.day")) if lday.is_dir() else []
        per_market[market] = {"files": len(files), "bytes": sum(path.stat().st_size for path in files)}
    for market, codes in VERIFY_SAMPLES.items():
        for code in codes:
            path = root / market / "lday" / f"{market}{code}.day"
            if not path.exists():
                error = f"{path} not found"
                errors.append(error)
                samples.append({"market": market, "code": code, "ok": False, "error": error})
                continue
            first_date, last_date = read_day_first_last(path.read_bytes())
            ok = last_date is not None and last_date >= target_int
            if not ok:
                errors.append(f"{path.name} last_date={last_date} < target={target_int}")
            samples.append({"market": market, "code": code, "first_date": first_date, "last_date": last_date, "ok": ok})
    return HsjdayVerifyResult(ok=not errors, per_market=per_market, samples=samples, errors=errors)


def replace_hsjday_root(new_root: Path, target_root: Path) -> None:
    target_root.parent.mkdir(parents=True, exist_ok=True)
    backup = target_root.with_name(f"{target_root.name}.bak")
    if backup.exists():
        shutil.rmtree(backup)
    if target_root.exists():
        target_root.rename(backup)
    try:
        new_root.rename(target_root)
    except Exception:
        if target_root.exists():
            shutil.rmtree(target_root)
        if backup.exists():
            backup.rename(target_root)
        raise
    if backup.exists():
        shutil.rmtree(backup)


def read_day_first_last(raw: bytes) -> tuple[int | None, int | None]:
    if len(raw) < 32:
        return None, None
    first = struct.unpack("<I", raw[:4])[0]
    last = struct.unpack("<I", raw[-32:-28])[0]
    return first, last


def _zip_day_member_market(name: str) -> str | None:
    parts = [part.lower() for part in Path(name).parts]
    for market in ("sh", "sz", "bj"):
        if len(parts) >= 3 and parts[-3:-1] == [market, "lday"] and parts[-1].endswith(".day"):
            return market
    return None


def _date_int(value: date) -> int:
    return value.year * 10000 + value.month * 100 + value.day


def _parse_content_range_total(value: str) -> int:
    if "/" not in value:
        return 0
    tail = value.rsplit("/", 1)[-1].strip()
    if not tail.isdigit():
        return 0
    return int(tail)


def _looks_like_js_challenge(chunk: bytes) -> bool:
    head = chunk[:2048].lower()
    markers = (b"<script", b"document.cookie", b"function a(a)", b"_0x649a")
    return b"<script" in head or (b"function " in head and b"document.cookie" in head) or any(marker in head for marker in markers)
