from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
import os
from pathlib import Path
import time
from typing import Any, Callable

import requests

from cynexus_data.connections.registry import ConnectionRegistry
from cynexus_data.hooks.storage.clickhouse import ClickHouseHook
from cynexus_data.hooks.storage.postgres import PostgresHook


Status = str


@dataclass
class DataSourceProbeResult:
    source_code: str
    source_name: str
    source_type: str
    category: str
    provider: str
    endpoint: str | None = None
    status: Status = "unknown"
    connectivity: bool = False
    authenticity_status: str = "unknown"
    latency_ms: int | None = None
    sample_count: int | None = None
    checked_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    error_message: str | None = None
    checks: dict[str, Any] = field(default_factory=dict)
    raw_payload: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


def run_data_source_health_checks(*, include_slow: bool = False) -> dict[str, Any]:
    registry = ConnectionRegistry()
    started = time.perf_counter()
    items = [
        _probe_postgres(registry, "postgres_market", "PostgreSQL Market"),
        _probe_postgres(registry, "postgres_core", "PostgreSQL Core"),
        _probe_clickhouse(registry),
        _probe_duckdb(registry),
        _probe_tdx_local(registry),
        _probe_python_import("pytdx_package", "pytdx reader package", "pytdx", "pytdx.reader"),
        _probe_python_import("eltdx_package", "eltdx TCP package", "eltdx", "eltdx"),
        _probe_python_import("mootdx_package", "mootdx TCP package", "mootdx", "mootdx"),
        _probe_http_connection(registry, "eastmoney_default", "EastMoney Push2", "/api/qt/stock/get", params={"secid": "1.600519", "fields": "f57,f58"}),
        _probe_http_connection(registry, "eastmoney_history", "EastMoney Push2His", "/api/qt/stock/fflow/daykline/get", params={"secid": "1.600519", "lmt": "1", "fields1": "f1,f2", "fields2": "f51,f52"}),
        _probe_eastmoney_datacenter(),
        _probe_eastmoney_reportapi(),
        _probe_eastmoney_search(),
        _probe_eastmoney_hot_rank(),
        _probe_http_connection(registry, "tencent_default", "Tencent Quote", "/", acceptable_status={200, 301, 302, 403, 404}),
        _probe_http_connection(registry, "sina_default", "Sina Finance", "/", acceptable_status={200, 301, 302, 403, 404}),
        _probe_http_connection(registry, "ths_default", "THS Data", "/", acceptable_status={200, 301, 302, 403, 404}),
        _probe_ths_basic(),
        _probe_http_connection(registry, "exchange_margin_default", "Exchange Margin", "/", acceptable_status={200, 301, 302, 403, 404}),
        _probe_baidu(),
        _probe_cninfo(),
        _probe_iwencai(),
        _probe_akshare(include_slow=include_slow),
        _probe_mootdx(include_slow=include_slow),
    ]
    summary = _summary(items)
    return {
        "ok": summary["failed"] == 0,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "duration_ms": int((time.perf_counter() - started) * 1000),
        "include_slow": include_slow,
        "summary": summary,
        "items": [item.to_dict() for item in items],
    }


def _probe_postgres(registry: ConnectionRegistry, conn_id: str, name: str) -> DataSourceProbeResult:
    return _timed_probe(
        source_code=conn_id,
        source_name=name,
        source_type="postgres",
        category="storage",
        provider="postgres",
        endpoint=conn_id,
        probe=lambda: _postgres_check(registry, conn_id),
    )


def _postgres_check(registry: ConnectionRegistry, conn_id: str) -> tuple[str, str, int | None, dict[str, Any], dict[str, Any]]:
    client = PostgresHook(conn_id, registry).get_client()
    with client.engine.connect() as conn:
        one = conn.exec_driver_sql("select 1").scalar_one()
        table_count = conn.exec_driver_sql(
            "select count(*) from information_schema.tables where table_schema = %s",
            (client.schema,),
        ).scalar_one()
    checks = {"select_1": one == 1, "schema": client.schema, "table_count": int(table_count or 0)}
    status = "healthy" if checks["select_1"] and checks["table_count"] > 0 else "degraded"
    authenticity = "valid" if checks["table_count"] > 0 else "warning"
    return status, authenticity, int(table_count or 0), checks, {"schema": client.schema}


def _probe_clickhouse(registry: ConnectionRegistry) -> DataSourceProbeResult:
    return _timed_probe(
        source_code="clickhouse_default",
        source_name="ClickHouse",
        source_type="clickhouse",
        category="storage",
        provider="clickhouse",
        endpoint="clickhouse_default",
        probe=lambda: _clickhouse_check(registry),
    )


def _clickhouse_check(registry: ConnectionRegistry) -> tuple[str, str, int | None, dict[str, Any], dict[str, Any]]:
    client = ClickHouseHook("clickhouse_default", registry).get_client()
    row = client.query_one("select count() from system.tables where database = currentDatabase()")
    table_count = int(row[0] if row else 0)
    status = "healthy" if table_count > 0 else "degraded"
    return status, "valid" if table_count > 0 else "warning", table_count, {"table_count": table_count}, {}


def _probe_duckdb(registry: ConnectionRegistry) -> DataSourceProbeResult:
    connection = registry.get("duckdb_default")
    path = Path(connection.path or "")
    exists = path.exists()
    return DataSourceProbeResult(
        source_code="duckdb_default",
        source_name="DuckDB local file",
        source_type="duckdb",
        category="storage",
        provider="duckdb",
        endpoint=str(path),
        status="healthy" if exists else "skipped",
        connectivity=exists,
        authenticity_status="valid" if exists else "unknown",
        sample_count=1 if exists else 0,
        checks={"file_exists": exists},
        raw_payload={"path": str(path)},
    )


def _probe_tdx_local(registry: ConnectionRegistry) -> DataSourceProbeResult:
    connection = registry.get("tdx_local")
    root = Path(connection.root_path or "")
    exists = root.exists()
    return DataSourceProbeResult(
        source_code="tdx_local",
        source_name="TDX local filesystem",
        source_type="filesystem",
        category="market-data",
        provider="tdx",
        endpoint=str(root),
        status="healthy" if exists else "degraded",
        connectivity=exists,
        authenticity_status="valid" if exists else "warning",
        sample_count=len(list(root.glob("*"))) if exists and root.is_dir() else 0,
        checks={"root_exists": exists},
        raw_payload={"root_path": str(root)},
    )


def _probe_python_import(source_code: str, name: str, provider: str, module_name: str) -> DataSourceProbeResult:
    started = time.perf_counter()
    try:
        __import__(module_name)
        return DataSourceProbeResult(
            source_code=source_code,
            source_name=name,
            source_type="python",
            category="runtime",
            provider=provider,
            endpoint=f"python import {module_name}",
            status="healthy",
            connectivity=True,
            authenticity_status="valid",
            latency_ms=int((time.perf_counter() - started) * 1000),
            sample_count=1,
            checks={"import_ok": True, "module": module_name},
        )
    except Exception as exc:
        return _failed_result(source_code, name, "python", "runtime", provider, f"python import {module_name}", started, exc)


def _probe_http_connection(
    registry: ConnectionRegistry,
    conn_id: str,
    name: str,
    path: str,
    *,
    params: dict[str, Any] | None = None,
    acceptable_status: set[int] | None = None,
) -> DataSourceProbeResult:
    connection = registry.get(conn_id)
    base_url = (connection.base_url or "").rstrip("/")
    url = base_url + path
    acceptable = acceptable_status or {200}
    return _timed_probe(
        source_code=conn_id,
        source_name=name,
        source_type="http",
        category="market-data",
        provider=_provider_from_conn(conn_id),
        endpoint=url,
        probe=lambda: _http_check(url, timeout=float(connection.timeout or 10), params=params, acceptable_status=acceptable),
    )


def _http_check(
    url: str,
    *,
    timeout: float,
    params: dict[str, Any] | None = None,
    acceptable_status: set[int],
) -> tuple[str, str, int | None, dict[str, Any], dict[str, Any]]:
    response = requests.get(url, params=params, timeout=timeout, headers={"User-Agent": "Mozilla/5.0"})
    ok_status = response.status_code in acceptable_status
    sample_count = 1 if response.text else 0
    checks = {"http_status": response.status_code, "content_length": len(response.content or b"")}
    if params and response.headers.get("content-type", "").lower().find("json") >= 0:
        try:
            payload = response.json()
            checks["json"] = isinstance(payload, dict)
            sample_count = _sample_count(payload)
        except Exception:
            checks["json"] = False
    status = "healthy" if response.status_code == 200 and sample_count else "degraded" if ok_status else "failed"
    authenticity = "valid" if response.status_code == 200 and sample_count else "warning" if ok_status else "failed"
    return status, authenticity, sample_count, checks, {"url": response.url}


def _http_post_json_check(
    url: str,
    *,
    timeout: float,
    json_body: dict[str, Any],
    acceptable_status: set[int],
) -> tuple[str, str, int | None, dict[str, Any], dict[str, Any]]:
    response = requests.post(url, json=json_body, timeout=timeout, headers={"User-Agent": "Mozilla/5.0"})
    ok_status = response.status_code in acceptable_status
    sample_count = 1 if response.text else 0
    checks = {"http_status": response.status_code, "content_length": len(response.content or b"")}
    if response.headers.get("content-type", "").lower().find("json") >= 0:
        try:
            payload = response.json()
            checks["json"] = isinstance(payload, dict)
            sample_count = _sample_count(payload)
        except Exception:
            checks["json"] = False
    status = "healthy" if response.status_code == 200 and sample_count else "degraded" if ok_status else "failed"
    authenticity = "valid" if response.status_code == 200 and sample_count else "warning" if ok_status else "failed"
    return status, authenticity, sample_count, checks, {"url": response.url}


def _probe_eastmoney_datacenter() -> DataSourceProbeResult:
    return _timed_probe(
        source_code="eastmoney_datacenter",
        source_name="EastMoney Datacenter",
        source_type="http",
        category="market-data",
        provider="eastmoney",
        endpoint="https://datacenter-web.eastmoney.com/api/data/v1/get",
        probe=lambda: _http_check(
            "https://datacenter-web.eastmoney.com/api/data/v1/get",
            timeout=10,
            params={
                "reportName": "RPT_HOLDERNUMLATEST",
                "columns": "SECURITY_CODE,SECURITY_NAME_ABBR,END_DATE,HOLDER_NUM",
                "pageNumber": "1",
                "pageSize": "1",
                "sortColumns": "END_DATE",
                "sortTypes": "-1",
                "source": "WEB",
                "client": "WEB",
            },
            acceptable_status={200},
        ),
    )


def _probe_eastmoney_reportapi() -> DataSourceProbeResult:
    return _timed_probe(
        source_code="eastmoney_reportapi",
        source_name="EastMoney Report API",
        source_type="http",
        category="research",
        provider="eastmoney",
        endpoint="https://reportapi.eastmoney.com/report/list",
        probe=lambda: _http_check(
            "https://reportapi.eastmoney.com/report/list",
            timeout=10,
            params={"pageSize": "1", "pageNo": "1", "qType": "0", "code": "600519", "industryCode": "*", "beginTime": "2025-01-01", "endTime": "2099-12-31"},
            acceptable_status={200},
        ),
    )


def _probe_eastmoney_search() -> DataSourceProbeResult:
    return _timed_probe(
        source_code="eastmoney_search_news",
        source_name="EastMoney News Search",
        source_type="http",
        category="news",
        provider="eastmoney",
        endpoint="https://search-api-web.eastmoney.com/search/jsonp",
        probe=lambda: _http_check(
            "https://search-api-web.eastmoney.com/search/jsonp",
            timeout=10,
            params={"cb": "jQuery_news", "param": '{"uid":"", "keyword":"600519", "type":["cmsArticleWebOld"], "client":"web", "clientType":"web", "pageIndex":1, "pageSize":1}'},
            acceptable_status={200},
        ),
    )


def _probe_eastmoney_hot_rank() -> DataSourceProbeResult:
    return _timed_probe(
        source_code="eastmoney_hot_rank",
        source_name="EastMoney Hot Rank",
        source_type="http",
        category="market-data",
        provider="eastmoney",
        endpoint="https://emappdata.eastmoney.com/stockrank/getAllCurrentList",
        probe=lambda: _http_post_json_check(
            "https://emappdata.eastmoney.com/stockrank/getAllCurrentList",
            timeout=10,
            json_body={
                "appId": "appId01",
                "globalId": "786e4c21-70dc-435a-93bb-38",
                "marketType": "",
                "pageNo": 1,
                "pageSize": 1,
            },
            acceptable_status={200, 403, 404},
        ),
    )


def _probe_ths_basic() -> DataSourceProbeResult:
    return _timed_probe(
        source_code="ths_basic",
        source_name="THS Basic Forecast",
        source_type="http",
        category="research",
        provider="ths",
        endpoint="https://basic.10jqka.com.cn/new/600519/worth.html",
        probe=lambda: _http_check(
            "https://basic.10jqka.com.cn/new/600519/worth.html",
            timeout=10,
            acceptable_status={200, 403},
        ),
    )


def _probe_baidu() -> DataSourceProbeResult:
    return _timed_probe(
        source_code="baidu_stock",
        source_name="Baidu Stock",
        source_type="http",
        category="market-data",
        provider="baidu",
        endpoint="https://finance.pae.baidu.com/selfselect/getstockquotation",
        probe=lambda: _http_check(
            "https://finance.pae.baidu.com/selfselect/getstockquotation",
            timeout=10,
            params={
                "all": "1",
                "isStock": "true",
                "newFormat": "1",
                "group": "quotation_kline_ab",
                "finClientType": "pc",
                "code": "600519",
                "ktype": "1",
            },
            acceptable_status={200, 403},
        ),
    )


def _probe_cninfo() -> DataSourceProbeResult:
    return _timed_probe(
        source_code="cninfo",
        source_name="CNInfo",
        source_type="http",
        category="announcement",
        provider="cninfo",
        endpoint="https://www.cninfo.com.cn/new/data/szse_stock.json",
        probe=lambda: _http_check("https://www.cninfo.com.cn/new/data/szse_stock.json", timeout=10, acceptable_status={200}),
    )


def _probe_iwencai() -> DataSourceProbeResult:
    has_key = bool(os.getenv("IWENCAI_API_KEY"))
    return DataSourceProbeResult(
        source_code="iwencai",
        source_name="iwencai semantic search",
        source_type="http",
        category="research",
        provider="iwencai",
        endpoint="api-key",
        status="skipped" if not has_key else "healthy",
        connectivity=has_key,
        authenticity_status="unknown" if not has_key else "valid",
        sample_count=0,
        checks={"api_key_configured": has_key},
        error_message=None if has_key else "IWENCAI_API_KEY is not configured",
    )


def _probe_akshare(*, include_slow: bool) -> DataSourceProbeResult:
    started = time.perf_counter()
    try:
        import akshare  # noqa: F401

        return DataSourceProbeResult(
            source_code="akshare_default",
            source_name="AkShare",
            source_type="python",
            category="market-data",
            provider="akshare",
            endpoint="python import",
            status="healthy" if include_slow else "skipped",
            connectivity=True,
            authenticity_status="valid" if include_slow else "unknown",
            latency_ms=int((time.perf_counter() - started) * 1000),
            sample_count=1 if include_slow else 0,
            checks={"import_ok": True, "slow_probe_enabled": include_slow},
        )
    except Exception as exc:
        return _failed_result("akshare_default", "AkShare", "python", "market-data", "akshare", "python import", started, exc)


def _probe_mootdx(*, include_slow: bool) -> DataSourceProbeResult:
    if not include_slow:
        return DataSourceProbeResult(
            source_code="mootdx",
            source_name="mootdx TCP quote",
            source_type="tcp",
            category="market-data",
            provider="mootdx",
            endpoint="quote server",
            status="skipped",
            connectivity=False,
            authenticity_status="unknown",
            checks={"slow_probe_enabled": False},
        )
    started = time.perf_counter()
    try:
        from cynexus_data.sources.tdx.mootdx_market import fetch_quotes

        rows = fetch_quotes(["600519"])
        valid = bool(rows)
        return DataSourceProbeResult(
            source_code="mootdx",
            source_name="mootdx TCP quote",
            source_type="tcp",
            category="market-data",
            provider="mootdx",
            endpoint="quote server",
            status="healthy" if valid else "degraded",
            connectivity=valid,
            authenticity_status="valid" if valid else "warning",
            latency_ms=int((time.perf_counter() - started) * 1000),
            sample_count=len(rows),
            checks={"quote_rows": len(rows)},
            raw_payload={"sample": rows[:1]},
        )
    except Exception as exc:
        return _failed_result("mootdx", "mootdx TCP quote", "tcp", "market-data", "mootdx", "quote server", started, exc)


def _timed_probe(
    *,
    source_code: str,
    source_name: str,
    source_type: str,
    category: str,
    provider: str,
    endpoint: str | None,
    probe: Callable[[], tuple[str, str, int | None, dict[str, Any], dict[str, Any]]],
) -> DataSourceProbeResult:
    started = time.perf_counter()
    try:
        status, authenticity, sample_count, checks, raw_payload = probe()
        return DataSourceProbeResult(
            source_code=source_code,
            source_name=source_name,
            source_type=source_type,
            category=category,
            provider=provider,
            endpoint=endpoint,
            status=status,
            connectivity=status in {"healthy", "degraded"},
            authenticity_status=authenticity,
            latency_ms=int((time.perf_counter() - started) * 1000),
            sample_count=sample_count,
            checks=checks,
            raw_payload=raw_payload,
        )
    except Exception as exc:
        return _failed_result(source_code, source_name, source_type, category, provider, endpoint, started, exc)


def _failed_result(
    source_code: str,
    source_name: str,
    source_type: str,
    category: str,
    provider: str,
    endpoint: str | None,
    started: float,
    exc: Exception,
) -> DataSourceProbeResult:
    return DataSourceProbeResult(
        source_code=source_code,
        source_name=source_name,
        source_type=source_type,
        category=category,
        provider=provider,
        endpoint=endpoint,
        status="failed",
        connectivity=False,
        authenticity_status="failed",
        latency_ms=int((time.perf_counter() - started) * 1000),
        sample_count=0,
        error_message=str(exc) or exc.__class__.__name__,
        checks={"exception": exc.__class__.__name__},
    )


def _summary(items: list[DataSourceProbeResult]) -> dict[str, Any]:
    total = len(items)
    healthy = sum(1 for item in items if item.status == "healthy")
    degraded = sum(1 for item in items if item.status == "degraded")
    failed = sum(1 for item in items if item.status == "failed")
    skipped = sum(1 for item in items if item.status == "skipped")
    latencies = [item.latency_ms for item in items if item.latency_ms is not None]
    return {
        "total": total,
        "healthy": healthy,
        "degraded": degraded,
        "failed": failed,
        "skipped": skipped,
        "success_rate": round(healthy / total * 100, 2) if total else 0,
        "avg_latency_ms": round(sum(latencies) / len(latencies), 2) if latencies else None,
    }


def _sample_count(payload: Any) -> int:
    if isinstance(payload, dict):
        if isinstance(payload.get("data"), list):
            return len(payload["data"])
        if isinstance(payload.get("stockList"), list):
            return len(payload["stockList"])
        return 1 if payload else 0
    if isinstance(payload, list):
        return len(payload)
    return 1 if payload else 0


def _provider_from_conn(conn_id: str) -> str:
    return conn_id.split("_", 1)[0]
