from __future__ import annotations

import shutil
from datetime import date, timedelta
from pathlib import Path

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.hsjday import (
    TDX_HSJDAY_URL,
    cleanup_old_hsjday_zips,
    download_hsjday_zip,
    inspect_existing_hsjday_root,
    inspect_zip_freshness,
    replace_hsjday_root,
    safe_extract_hsjday_zip,
    verify_hsjday_root,
)


def run(context: PipelineContext, request: JobRequest) -> dict:
    requested_date = date.fromisoformat(
        str(request.params.get("date") or request.params.get("trade_date") or date.today().isoformat())
    )
    # TDX does not publish new daily bars on weekends. Use the last weekday
    # for the freshness check so a scheduled weekend refresh remains idempotent.
    target_date = _last_weekday(requested_date)
    url = str(request.params.get("url") or TDX_HSJDAY_URL)
    download_base = Path(str(request.params.get("download_base") or PROJECT_ROOT / "var" / "stock" / "download"))
    work_dir = Path(str(request.params.get("work_dir") or download_base / target_date.isoformat()))
    zip_path = Path(str(request.params.get("zip_path") or work_dir / "hsjday.zip"))
    target_root = Path(str(request.params.get("target_root") or PROJECT_ROOT / "var" / "tdx" / "day" / "hsjday"))
    dated_target = Path(str(request.params.get("dated_target") or target_root.parent / f"hsjday-{target_date.isoformat()}"))
    extract_dir = Path(str(request.params.get("extract_dir") or work_dir / f"hsjday_extract_tmp"))
    skip_download = _truthy(request.params.get("skip_download"))
    keep_zip = _truthy_default(request.params.get("keep_zip"), True)
    skip_existing_check = _truthy(request.params.get("skip_existing_check"))
    timeout_seconds = int(request.params.get("timeout_seconds") or 120)
    download_method = str(request.params.get("download_method") or request.params.get("method") or "auto")
    playwright_headless = _truthy_default(request.params.get("playwright_headless"), True)
    playwright_wait_ms = int(request.params.get("playwright_wait_ms") or 2000)
    chunk_size_bytes = int(request.params.get("chunk_size_bytes") or 4 * 1024 * 1024)
    urlopen_attempts = int(request.params.get("urlopen_attempts") or 3)
    urlopen_backoff_seconds = float(request.params.get("urlopen_backoff_seconds") or 1.0)

    result = {
        "pipeline": "kline.sync_tdx_hsjday",
        "dry_run": request.dry_run,
        "date": target_date.isoformat(),
        "url": url,
        "zip_path": str(zip_path),
        "download_base": str(download_base),
        "target_root": str(target_root),
        "dated_target": str(dated_target),
        "extract_dir": str(extract_dir),
        "skip_download": skip_download,
        "keep_zip": keep_zip,
        "download_method": download_method,
        "urlopen_attempts": urlopen_attempts,
        "urlopen_backoff_seconds": urlopen_backoff_seconds,
        "rows_read": 0,
        "rows_written": 0,
        "steps": [],
    }
    if request.dry_run:
        result["steps"].append({"step": "plan", "ok": True})
        return result

    if not skip_existing_check:
        existing_check = inspect_existing_hsjday_root(target_root, target_date)
        result["steps"].append({"step": "existing_data", **existing_check})
        if existing_check.get("already_have_data"):
            result["skipped"] = True
            result["skip_reason"] = f"target already covers {target_date.isoformat()}"
            return result

    if not skip_download:
        download_result = download_hsjday_zip(
            url,
            zip_path,
            timeout_seconds=timeout_seconds,
            method=download_method,
            playwright_headless=playwright_headless,
            playwright_wait_ms=playwright_wait_ms,
            chunk_size_bytes=chunk_size_bytes,
            urlopen_attempts=urlopen_attempts,
            urlopen_backoff_seconds=urlopen_backoff_seconds,
        )
        result["steps"].append(
            {
                "step": "download",
                "ok": True,
                "bytes": download_result.bytes_downloaded,
                "method": download_result.method,
            }
        )
    elif not zip_path.exists():
        raise FileNotFoundError(f"skip_download=true but zip does not exist: {zip_path}")
    else:
        result["steps"].append({"step": "download", "ok": True, "skipped": True, "bytes": zip_path.stat().st_size})

    zip_check = inspect_zip_freshness(zip_path, target_date)
    result["steps"].append({"step": "zip_freshness", **_verify_to_dict(zip_check)})
    if not zip_check.ok:
        raise RuntimeError(f"TDX hsjday zip freshness check failed: {zip_check.errors}")

    extracted_root = safe_extract_hsjday_zip(zip_path, extract_dir)
    if dated_target.exists():
        shutil.rmtree(dated_target, ignore_errors=True)
    dated_target.parent.mkdir(parents=True, exist_ok=True)
    extracted_root.rename(dated_target)
    result["steps"].append({"step": "extract", "ok": True, "extracted_to": str(dated_target)})
    if extract_dir.exists():
        shutil.rmtree(extract_dir, ignore_errors=True)

    root_check = verify_hsjday_root(dated_target, target_date)
    result["steps"].append({"step": "verify_extracted", **_verify_to_dict(root_check)})
    if not root_check.ok:
        if dated_target.exists():
            shutil.rmtree(dated_target, ignore_errors=True)
        raise RuntimeError(f"TDX hsjday extracted data check failed: {root_check.errors}")

    replace_hsjday_root(dated_target, target_root)
    result["steps"].append({"step": "replace", "ok": True})
    result["rows_read"] = sum(item.get("files", 0) for item in root_check.per_market.values())
    result["rows_written"] = result["rows_read"]

    if extract_dir.exists():
        shutil.rmtree(extract_dir, ignore_errors=True)
    if not keep_zip and zip_path.exists():
        zip_path.unlink()
    else:
        result["deleted_old_zips"] = cleanup_old_hsjday_zips(download_base, today=target_date)
    return result


def _verify_to_dict(result) -> dict:
    return {"ok": result.ok, "per_market": result.per_market, "samples": result.samples, "errors": result.errors}


def _last_weekday(value: date) -> date:
    while value.weekday() >= 5:
        value -= timedelta(days=1)
    return value


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}


def _truthy_default(value, default: bool) -> bool:
    if value is None:
        return default
    return _truthy(value)
