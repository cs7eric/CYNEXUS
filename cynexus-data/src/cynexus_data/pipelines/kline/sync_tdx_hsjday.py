from __future__ import annotations

import shutil
from datetime import date
from pathlib import Path

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.hsjday import (
    TDX_HSJDAY_URL,
    download_hsjday_zip,
    inspect_zip_freshness,
    replace_hsjday_root,
    safe_extract_hsjday_zip,
    verify_hsjday_root,
)


def run(context: PipelineContext, request: JobRequest) -> dict:
    target_date = date.fromisoformat(str(request.params.get("date") or request.params.get("trade_date") or date.today().isoformat()))
    url = str(request.params.get("url") or TDX_HSJDAY_URL)
    work_dir = Path(str(request.params.get("work_dir") or PROJECT_ROOT / "var" / "tdx" / "downloads" / target_date.isoformat()))
    zip_path = Path(str(request.params.get("zip_path") or work_dir / "hsjday.zip"))
    target_root = Path(str(request.params.get("target_root") or PROJECT_ROOT / "var" / "tdx" / "hsjday"))
    extract_dir = Path(str(request.params.get("extract_dir") or work_dir / "extract"))
    skip_download = _truthy(request.params.get("skip_download"))
    keep_zip = _truthy(request.params.get("keep_zip"))
    timeout_seconds = int(request.params.get("timeout_seconds") or 120)
    download_method = str(request.params.get("download_method") or request.params.get("method") or "auto")
    playwright_headless = _truthy_default(request.params.get("playwright_headless"), True)
    playwright_wait_ms = int(request.params.get("playwright_wait_ms") or 2000)
    chunk_size_bytes = int(request.params.get("chunk_size_bytes") or 4 * 1024 * 1024)

    result = {
        "pipeline": "kline.sync_tdx_hsjday",
        "dry_run": request.dry_run,
        "date": target_date.isoformat(),
        "url": url,
        "zip_path": str(zip_path),
        "target_root": str(target_root),
        "extract_dir": str(extract_dir),
        "skip_download": skip_download,
        "download_method": download_method,
        "rows_read": 0,
        "rows_written": 0,
        "steps": [],
    }
    if request.dry_run:
        result["steps"].append({"step": "plan", "ok": True})
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
    root_check = verify_hsjday_root(extracted_root, target_date)
    result["steps"].append({"step": "verify_extracted", **_verify_to_dict(root_check)})
    if not root_check.ok:
        raise RuntimeError(f"TDX hsjday extracted data check failed: {root_check.errors}")

    replace_hsjday_root(extracted_root, target_root)
    result["steps"].append({"step": "replace", "ok": True})
    result["rows_read"] = sum(item.get("files", 0) for item in root_check.per_market.values())
    result["rows_written"] = result["rows_read"]

    if extract_dir.exists():
        shutil.rmtree(extract_dir, ignore_errors=True)
    if not keep_zip and zip_path.exists():
        zip_path.unlink()
    return result


def _verify_to_dict(result) -> dict:
    return {"ok": result.ok, "per_market": result.per_market, "samples": result.samples, "errors": result.errors}


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
