from __future__ import annotations

from dataclasses import asdict
from datetime import datetime, timezone
from typing import Any


def create_eltdx_f10_client(pool_size: int = 4, timeout: float = 8.0):
    try:
        import eltdx  # type: ignore
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError("eltdx is required for F10 queries") from exc
    return eltdx.TdxClient(pool_size=pool_size, timeout=timeout)


def fetch_stock_valuation(client: Any, symbol: str, *, req_id: str = "200191") -> dict[str, Any]:
    response = client.f10.valuation(symbol, req_id=req_id)
    rows = _result_set_rows(_f10_to_dict(response))
    normalized = [
        {
            "date": _text(row.get("DATE")) or "",
            "pe_ttm": _float_or_none(row.get("PETTM")),
            "pe_bfw": _float_or_none(row.get("PEBFW")),
            "pb_mrq": _float_or_none(row.get("PBMRQ")),
            "pb_bfw": _float_or_none(row.get("PBBFW")),
            "pcf_ocf_ttm": _float_or_none(row.get("PCFOCFTTM")),
            "pcf_bfw": _float_or_none(row.get("PCFBFW")),
            "ps_ttm": _float_or_none(row.get("PSTTM")),
            "ps_bfw": _float_or_none(row.get("PSBFW")),
            "peg": _float_or_none(row.get("PEG")),
            "avg_market_cap": _float_or_none(row.get("AVGMVM")),
            "aliq_market_cap": _float_or_none(row.get("ALIQMV")),
        }
        for row in rows
        if row.get("DATE") is not None
    ]
    return {
        "symbol": str(symbol),
        "req_id": str(req_id),
        "rows": normalized,
        "raw": _f10_to_dict(response),
        "fetched_at": _utcnow_iso(),
        "source": "eltdx",
    }


def fetch_stock_finance_report(client: Any, symbol: str, *, report_type: str = "zcfzb") -> dict[str, Any]:
    response = client.f10.finance_report(symbol, report_type=report_type)
    rows = _result_set_rows(_f10_to_dict(response))
    normalized = []
    for row in rows:
        rq = _text(row.get("rq"))
        if rq is None:
            continue
        normalized.append(
            {
                "rq": rq,
                "fields": {
                    str(key): _primitive(value)
                    for key, value in row.items()
                },
            }
        )
    return {
        "symbol": str(symbol),
        "report_type": str(report_type),
        "rows": normalized,
        "raw": _f10_to_dict(response),
        "fetched_at": _utcnow_iso(),
        "source": "eltdx",
    }


def fetch_stock_business_composition(client: Any, symbol: str, *, report_date: str | None = None) -> dict[str, Any]:
    response = client.f10.business_composition(symbol, report_date=report_date)
    rows = _result_set_rows(_f10_to_dict(response))
    normalized = []
    effective_report_date = report_date
    for row in rows:
        values = list(row.values())
        if effective_report_date is None:
            effective_report_date = _text(row.get("T002")) or _text(row.get("report_date"))
        normalized.append(
            {
                "category": _text(row.get("N000")),
                "sub_type": _primitive(values[1]) if len(values) > 1 else None,
                "name": _text(row.get("N002")),
                "revenue": _float_or_none(row.get("N003")),
                "ratio": _float_or_none(row.get("N004")),
                "extras": [_primitive(value) for value in values[5:]],
            }
        )
    return {
        "symbol": str(symbol),
        "report_date": effective_report_date,
        "items": normalized,
        "raw": _f10_to_dict(response),
        "fetched_at": _utcnow_iso(),
        "source": "eltdx",
    }


def fetch_stock_profit_forecast(client: Any, symbol: str) -> dict[str, Any]:
    raw = _f10_to_dict(client.f10.profit_forecast(symbol))
    result_sets = raw.get("ResultSets") if isinstance(raw, dict) else None
    year_row = _safe_content_row(result_sets, 0)
    forecast_cols = _safe_columns(result_sets, 1)
    forecast_values = _safe_content_row(result_sets, 1)
    year_base = _text(year_row[0]) if year_row else None
    items: list[dict[str, Any]] = []
    for index, col_name in enumerate(forecast_cols):
        value = forecast_values[index] if index < len(forecast_values) else None
        offset = len(forecast_cols) - 1 - index
        year = ""
        if year_base:
            try:
                year = str(int(year_base) + offset if offset > 0 else int(year_base))
            except ValueError:
                year = year_base
        items.append(
            {
                "year": year,
                "net_profit": None,
                "eps": _float_or_none(value),
                "revenue": None,
                "extras": [_primitive(col_name), _primitive(value)],
            }
        )
    return {
        "symbol": str(symbol),
        "items": items,
        "raw": raw,
        "fetched_at": _utcnow_iso(),
        "source": "eltdx",
    }


def fetch_stock_announcements(client: Any, symbol: str) -> dict[str, Any]:
    response = client.f10.announcements(symbol)
    items = _parse_table_rows(
        response,
        lambda row: {
            "issue_date": _text(row.get("issue_date")),
            "title": _text(row.get("title")),
            "typecode": _text(row.get("typecode")),
            "typename": _text(row.get("typename")),
            "rec_id": _text(row.get("rec_id")),
            "tableid": _text(row.get("tableid")),
            "url": _text(row.get("url")),
            "redistime": _text(row.get("redistime")),
            "source": _text(row.get("source")),
        },
    )
    return {
        "symbol": str(symbol),
        "items": items,
        "count": len(items),
        "raw": _f10_to_dict(response),
        "fetched_at": _utcnow_iso(),
        "source": "eltdx",
    }


def fetch_stock_news(client: Any, symbol: str) -> dict[str, Any]:
    response = client.f10.news(symbol)
    items = _parse_table_rows(
        response,
        lambda row: {
            "issue_date": _text(row.get("issue_date")),
            "title": _text(row.get("title")),
            "rec_id": _text(row.get("rec_id")),
            "tableid": _text(row.get("tableid")),
            "redistime": _text(row.get("redistime")),
            "source": _text(row.get("source")),
            "relatecolumn": _text(row.get("relatecolumn")),
        },
    )
    return {
        "symbol": str(symbol),
        "items": items,
        "count": len(items),
        "raw": _f10_to_dict(response),
        "fetched_at": _utcnow_iso(),
        "source": "eltdx",
    }


def fetch_stock_roadshows(client: Any, symbol: str) -> dict[str, Any]:
    response = client.f10.roadshows(symbol)
    items = _parse_table_rows(
        response,
        lambda row: {
            "title": _text(row.get("title")),
            "roadshow_type": _text(row.get("roadshow_type")),
            "start_date": _text(row.get("start_date")),
            "start_time": _text(row.get("start_time")),
            "end_time": _text(row.get("end_time")),
            "summary": _text(row.get("summary")),
            "url": _text(row.get("url")),
        },
    )
    return {
        "symbol": str(symbol),
        "items": items,
        "count": len(items),
        "raw": _f10_to_dict(response),
        "fetched_at": _utcnow_iso(),
        "source": "eltdx",
    }


def fetch_stock_company_news(client: Any, symbol: str, *, section: str = "gsyj") -> dict[str, Any]:
    response = client.f10.company_news(symbol, section=section)
    items = _parse_table_rows(
        response,
        lambda row: {
            "rating": _text(row.get("T004")),
            "analysts": _text(row.get("T009")),
            "rec_id": _text(row.get("T011")),
            "issue_date": _text(row.get("T012")),
            "title": _text(row.get("T039")),
            "nflag": _text(row.get("nflag")),
            "doc_hash": _text(row.get("ybdz")),
        },
    )
    return {
        "symbol": str(symbol),
        "section": str(section),
        "items": items,
        "count": len(items),
        "raw": _f10_to_dict(response),
        "fetched_at": _utcnow_iso(),
        "source": "eltdx",
    }


def _parse_table_rows(response: Any, mapper) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    tables = getattr(response, "tables", None) or []
    for table in tables:
        rows = getattr(table, "rows", None) or []
        for row in rows:
            payload = mapper(row if isinstance(row, dict) else {})
            if not payload.get("title"):
                continue
            out.append(payload)
    return out


def _result_set_rows(raw: dict[str, Any]) -> list[dict[str, Any]]:
    result_sets = raw.get("ResultSets")
    if not isinstance(result_sets, list):
        return []
    rows: list[dict[str, Any]] = []
    for table in result_sets:
        if not isinstance(table, dict):
            continue
        columns = _extract_columns(table)
        content = table.get("Content")
        if not isinstance(columns, list) or not isinstance(content, list):
            continue
        for item in content:
            if not isinstance(item, list):
                continue
            row: dict[str, Any] = {}
            for index, column in enumerate(columns):
                row[str(column)] = item[index] if index < len(item) else None
            rows.append(row)
    return rows


def _safe_columns(result_sets: Any, index: int) -> list[Any]:
    if not isinstance(result_sets, list) or index >= len(result_sets):
        return []
    table = result_sets[index]
    if not isinstance(table, dict):
        return []
    return _extract_columns(table)


def _safe_content_row(result_sets: Any, index: int) -> list[Any]:
    if not isinstance(result_sets, list) or index >= len(result_sets):
        return []
    table = result_sets[index]
    if not isinstance(table, dict):
        return []
    content = table.get("Content")
    if not isinstance(content, list) or not content:
        return []
    row = content[0]
    return row if isinstance(row, list) else []


def _f10_to_dict(response: Any) -> dict[str, Any]:
    if response is None:
        return {}
    if hasattr(response, "raw") and isinstance(response.raw, dict):
        return _sanitize_jsonable(response.raw)
    if isinstance(response, dict):
        return _sanitize_jsonable(response)
    if hasattr(response, "__dataclass_fields__"):
        return _sanitize_jsonable(asdict(response))
    raw = getattr(response, "__dict__", None)
    return _sanitize_jsonable(raw) if isinstance(raw, dict) else {}


def _extract_columns(table: dict[str, Any]) -> list[str]:
    columns = table.get("ColName")
    if isinstance(columns, list):
        return [str(column) for column in columns]
    col_des = table.get("ColDes")
    if not isinstance(col_des, list):
        return []
    extracted: list[str] = []
    for item in col_des:
        if isinstance(item, dict):
            name = item.get("Name")
            if name is not None:
                extracted.append(str(name))
    return extracted


def _sanitize_jsonable(value: Any):
    if isinstance(value, bytes):
        return value.hex()
    if isinstance(value, datetime):
        return value.isoformat()
    if isinstance(value, dict):
        return {key: _sanitize_jsonable(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [_sanitize_jsonable(item) for item in value]
    return value


def _primitive(value: Any) -> Any:
    if value is None:
        return None
    if isinstance(value, (str, int, float, bool)):
        return value
    if isinstance(value, datetime):
        return value.isoformat()
    text = str(value).strip()
    if not text:
        return None
    number = _float_or_none(text)
    return number if number is not None else text


def _text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _float_or_none(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", "").strip())
    except (TypeError, ValueError):
        return None


def _utcnow_iso() -> str:
    return datetime.now(timezone.utc).isoformat()
