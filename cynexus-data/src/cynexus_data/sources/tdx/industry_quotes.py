from __future__ import annotations

from dataclasses import asdict
from typing import Any

from cynexus_data.domain.market.index_codes import TDX_INDUSTRY_56, get_tdx_industry_name


def create_eltdx_industry_client(pool_size: int = 8, timeout: float = 8.0):
    try:
        import eltdx  # type: ignore
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError("eltdx is required for TDX industry quote sync") from exc
    client = eltdx.TdxClient(pool_size=pool_size, timeout=timeout)
    client.connect()
    return client


def resolve_tdx_industry_full_codes(client: Any, mapping: dict[str, str] | None = None) -> dict[str, dict[str, Any]]:
    mapping = mapping or TDX_INDUSTRY_56
    code_to_full: dict[str, str] = {}
    for full_code in client.get_index_codes_all() or []:
        raw = str(full_code or "").strip().lower()
        if len(raw) < 8:
            continue
        prefix = raw[:2]
        if prefix not in {"sh", "sz", "bj"}:
            continue
        code6 = raw[2:]
        code_to_full.setdefault(code6, raw)
    return {
        code6: {"name": str(name), "full_code": full_code, "exchange": full_code[:2]}
        for code6, name in mapping.items()
        if (full_code := code_to_full.get(str(code6)))
    }


def fetch_tdx_industry_snapshots(
    client: Any,
    *,
    mapping: dict[str, str] | None = None,
    batch_size: int = 20,
) -> list[dict[str, Any]]:
    resolved = resolve_tdx_industry_full_codes(client, mapping=mapping)
    full_to_info = {info["full_code"]: {"code6": code6, **info} for code6, info in resolved.items()}
    rows: list[dict[str, Any]] = []
    full_codes = list(full_to_info)
    for start in range(0, len(full_codes), max(1, int(batch_size))):
        chunk = full_codes[start : start + max(1, int(batch_size))]
        for quote in client.get_quote(chunk) or []:
            raw = _quote_to_mapping(quote)
            full_code = str(raw.get("full_code") or raw.get("fullCode") or "").strip().lower()
            info = full_to_info.get(full_code)
            if not info:
                continue
            rows.append(_quote_mapping_to_row(raw, code6=info["code6"], name=info["name"], full_code=full_code, exchange=info["exchange"]))
    rows.sort(key=lambda row: (row.get("change_pct") is None, -(float(row.get("change_pct") or 0))))
    return rows


def build_tdx_industry_market_payload(rows: list[dict[str, Any]], *, fetched_at: str | None = None) -> dict[str, Any]:
    return {
        "kind": "industries",
        "label": "行业",
        "count": len(rows),
        "fetchedAt": fetched_at,
        "items": [
            {
                "code6": row.get("code6"),
                "name": row.get("name"),
                "fullCode": row.get("full_code"),
                "latestPrice": row.get("last_price"),
                "preClosePrice": row.get("pre_close_price"),
                "openPrice": row.get("open_price"),
                "highPrice": row.get("high_price"),
                "lowPrice": row.get("low_price"),
                "change": row.get("change"),
                "changePercent": row.get("change_pct"),
                "amount": row.get("amount"),
                "volume": row.get("total_hand"),
                "kind": "industries",
                "sectorCode": row.get("full_code"),
                "sectorName": row.get("name"),
            }
            for row in rows
        ],
        "source": "eltdx.get_index_codes_all+get_quote",
    }


def _quote_to_mapping(quote: Any) -> dict[str, Any]:
    if quote is None:
        return {}
    if isinstance(quote, dict):
        return dict(quote)
    if hasattr(quote, "__dataclass_fields__"):
        return asdict(quote)
    return {
        "full_code": getattr(quote, "full_code", None),
        "code": getattr(quote, "code", None),
        "exchange": getattr(quote, "exchange", None),
        "last_price": getattr(quote, "last_price", None),
        "pre_close_price": getattr(quote, "pre_close_price", None),
        "open_price": getattr(quote, "open_price", None),
        "high_price": getattr(quote, "high_price", None),
        "low_price": getattr(quote, "low_price", None),
        "change": getattr(quote, "change", None),
        "change_pct": getattr(quote, "change_pct", None),
        "amount": getattr(quote, "amount", None),
        "total_hand": getattr(quote, "total_hand", None),
        "current_hand": getattr(quote, "current_hand", None),
    }


def _quote_mapping_to_row(raw: dict[str, Any], *, code6: str, name: str, full_code: str, exchange: str) -> dict[str, Any]:
    return {
        "code6": str(code6),
        "name": name or get_tdx_industry_name(code6) or str(code6),
        "full_code": full_code,
        "exchange": exchange,
        "last_price": _float_or_none(raw.get("last_price") or raw.get("lastPrice")),
        "pre_close_price": _float_or_none(raw.get("pre_close_price") or raw.get("preClosePrice")),
        "open_price": _float_or_none(raw.get("open_price") or raw.get("openPrice")),
        "high_price": _float_or_none(raw.get("high_price") or raw.get("highPrice")),
        "low_price": _float_or_none(raw.get("low_price") or raw.get("lowPrice")),
        "change": _float_or_none(raw.get("change")),
        "change_pct": _float_or_none(raw.get("change_pct") or raw.get("changePct")),
        "amount": _float_or_none(raw.get("amount")),
        "total_hand": _float_or_none(raw.get("total_hand") or raw.get("totalHand")),
        "current_hand": _float_or_none(raw.get("current_hand") or raw.get("currentHand")),
    }


def _float_or_none(value: Any) -> float | None:
    if value in (None, ""):
        return None
    return float(value)
