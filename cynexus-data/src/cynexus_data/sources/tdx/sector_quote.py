from __future__ import annotations

from typing import Any

from cynexus_data.domain.market.index_codes import TDX_INDUSTRY_56, TDX_INDUSTRY_SEEDS


PERIOD_LABELS = {
    "0": "近1日",
    "1": "近3日",
    "2": "近5日",
    "3": "近20日",
    "4": "近60日",
    "5": "近1年",
}

REQ_IDS = {
    "period_stats": "200741",
    "main_capital_flow": "200742",
    "related": "200743",
    "constituents": "200744",
    "main_control": "200745",
}


def create_eltdx_sector_quote_client(pool_size: int = 4, timeout: float = 8.0):
    try:
        import eltdx  # type: ignore
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError("eltdx is required for TDX sector quote detail sync") from exc
    client = eltdx.TdxClient(pool_size=pool_size, timeout=timeout)
    client.connect()
    return client


def fetch_tdx_industry_sector_quote_details(
    client: Any,
    *,
    mapping: dict[str, str] | None = None,
    seed_mapping: dict[str, list[str]] | None = None,
    datasets: list[str] | None = None,
) -> list[dict[str, Any]]:
    mapping = mapping or TDX_INDUSTRY_56
    seed_mapping = seed_mapping or TDX_INDUSTRY_SEEDS
    rows: list[dict[str, Any]] = []
    for code6, name in mapping.items():
        seeds = list(seed_mapping.get(str(code6)) or [])
        seed_code = seeds[0] if seeds else None
        if not seed_code:
            rows.append({"code6": str(code6), "name": name, "seed_code": None, "ok": False, "error": "missing_seed", "sections": {}})
            continue
        rows.append(fetch_sector_quote_detail(client, code6=str(code6), name=str(name), seed_code=seed_code, datasets=datasets))
    return rows


def fetch_sector_quote_detail(
    client: Any,
    *,
    code6: str,
    name: str,
    seed_code: str,
    datasets: list[str] | None = None,
) -> dict[str, Any]:
    requested = _normalize_datasets(datasets)
    sections: dict[str, Any] = {}
    errors: dict[str, str] = {}
    for dataset in requested:
        req_id = REQ_IDS[dataset]
        try:
            extra = {"sectype": code6} if dataset == "constituents" else None
            response = client.f10.theme_market(seed_code, req_id, page=-1, page_size=-1, extra=extra)
            sections[dataset] = parse_theme_market_table(response, dataset)
        except Exception as exc:  # noqa: BLE001
            sections[dataset] = []
            errors[dataset] = str(exc)
    return {
        "code6": str(code6),
        "name": str(name),
        "seed_code": str(seed_code),
        "ok": not errors,
        "sections": sections,
        "errors": errors,
        "source": "eltdx.f10.theme_market",
    }


def parse_theme_market_table(response: Any, kind: str) -> list[dict[str, Any]]:
    if not getattr(response, "ok", False):
        return []
    tables = getattr(response, "tables", None) or []
    if len(tables) < 2:
        return []
    rows = getattr(tables[1], "rows", None) or []
    parsed: list[dict[str, Any]] = []
    for row in rows:
        data = dict(row) if isinstance(row, dict) else {}
        item = _parse_row(data, kind)
        if item:
            parsed.append(item)
    if kind == "constituents":
        parsed.sort(key=lambda item: (item.get("change_pct") is None, -(item.get("change_pct") or 0), item.get("code") or ""))
    return parsed


def _parse_row(row: dict[str, Any], kind: str) -> dict[str, Any]:
    if kind == "related":
        return {
            "market": _text(row.get("N001")),
            "sector_code": _text(row.get("N002")),
            "sector_name": _text(row.get("N003")),
            "change_pct": _float_or_none(row.get("N004")),
            "limit_up_count": _int_or_none(row.get("N005")),
        }
    if kind == "period_stats":
        range_code = _text(row.get("N001")) or ""
        return {
            "range_code": range_code,
            "range_label": PERIOD_LABELS.get(range_code, "未知"),
            "value1": _float_or_none(row.get("N002")),
            "value2": _float_or_none(row.get("N003")),
            "value3": _int_or_none(row.get("N004")),
        }
    if kind == "main_capital_flow":
        return {
            "sector_name": _text(row.get("N001")),
            "date": _text(row.get("N002")),
            "main_net": _float_or_none(row.get("N003")),
            "large_order_net": _float_or_none(row.get("N004")),
            "medium_order_net": _float_or_none(row.get("N005")),
            "small_order_net": _float_or_none(row.get("N006")),
        }
    if kind == "constituents":
        return {
            "market": _text(row.get("N001")),
            "code": _text(row.get("N002")),
            "name": _text(row.get("N003")),
            "change_pct": _float_or_none(row.get("N004")),
            "last_price": _float_or_none(row.get("N005")),
        }
    if kind == "main_control":
        return {
            "date": _text(row.get("N001")),
            "control_pct": _float_or_none(row.get("N002")),
        }
    raise ValueError(f"unknown theme_market table kind: {kind}")


def _normalize_datasets(datasets: list[str] | None) -> list[str]:
    if not datasets:
        return list(REQ_IDS)
    normalized = []
    for item in datasets:
        value = str(item or "").strip()
        if not value:
            continue
        if value not in REQ_IDS:
            raise ValueError(f"unsupported sector quote dataset: {value}")
        normalized.append(value)
    return normalized or list(REQ_IDS)


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


def _int_or_none(value: Any) -> int | None:
    parsed = _float_or_none(value)
    return int(parsed) if parsed is not None else None
