from __future__ import annotations

from datetime import date, datetime
from typing import Any


VALID_SELECTION_MODES = {"exact", "fallback_previous", "latest_any", "empty"}
VALID_NAME_MATCH_TYPES = {"exact", "mismatch", "empty"}
VALID_SNAPSHOT_STATUSES = {"ok", "empty", "error"}


def build_tdx_industry_fund_flow_snapshots(
    rows: list[dict[str, Any]],
    *,
    snapshot_date: date,
    source: str = "eltdx.f10.theme_market.200742",
) -> list[dict[str, Any]]:
    snapshots = [
        build_tdx_industry_fund_flow_snapshot(row, snapshot_date=snapshot_date, source=source)
        for row in rows
    ]
    return [snapshot for snapshot in snapshots if snapshot is not None]


def build_tdx_industry_fund_flow_snapshot(
    row: dict[str, Any],
    *,
    snapshot_date: date,
    source: str = "eltdx.f10.theme_market.200742",
) -> dict[str, Any] | None:
    code6 = _normalize_code6(row.get("code6"))
    tdx_name = _text(row.get("name"))
    if not code6 or not tdx_name:
        return None

    history = _normalize_history((row.get("sections") or {}).get("main_capital_flow"))
    selected, selection_mode = select_fund_flow_history_item(history, snapshot_date)
    errors = row.get("errors") if isinstance(row.get("errors"), dict) else {}
    status = "ok" if selected else ("error" if errors.get("main_capital_flow") else "empty")
    flow_sector_name = _text(selected.get("sector_name")) if selected else None
    name_match_type = "empty"
    if flow_sector_name:
        name_match_type = "exact" if flow_sector_name == tdx_name else "mismatch"

    return {
        "trade_date": snapshot_date,
        "code6": code6,
        "full_code": f"sh{code6}",
        "tdx_name": tdx_name,
        "flow_sector_name": flow_sector_name,
        "seed_code": _normalize_full_code(row.get("seed_code")),
        "flow_trade_date": selected.get("trade_date") if selected else None,
        "snapshot_status": status,
        "selection_mode": selection_mode,
        "name_match_type": name_match_type,
        "main_net": _decimal_or_none(selected.get("main_net")) if selected else None,
        "large_order_net": _decimal_or_none(selected.get("large_order_net")) if selected else None,
        "medium_order_net": _decimal_or_none(selected.get("medium_order_net")) if selected else None,
        "small_order_net": _decimal_or_none(selected.get("small_order_net")) if selected else None,
        "row_count": len(history),
        "source": _text(row.get("source")) or source,
        "extra": {
            "errors": errors,
            "available_dates": [item["trade_date"].isoformat() for item in history if item.get("trade_date")],
        },
    }


def select_fund_flow_history_item(history: list[dict[str, Any]], snapshot_date: date) -> tuple[dict[str, Any] | None, str]:
    if not history:
        return None, "empty"
    exact = next((item for item in history if item.get("trade_date") == snapshot_date), None)
    if exact:
        return exact, "exact"
    previous = [item for item in history if item.get("trade_date") and item["trade_date"] < snapshot_date]
    if previous:
        previous.sort(key=lambda item: item["trade_date"])
        return previous[-1], "fallback_previous"
    latest = [item for item in history if item.get("trade_date")]
    if latest:
        latest.sort(key=lambda item: item["trade_date"])
        return latest[-1], "latest_any"
    return None, "empty"


def build_tdx_industry_fund_flow_response(
    *,
    target_type: str,
    symbol: str,
    requested_trade_date: date | None,
    days: int,
    latest_row: dict[str, Any] | None,
    history_rows: list[dict[str, Any]] | None = None,
    applicable: bool,
    error: str | None = None,
) -> dict[str, Any]:
    history = history_rows or []
    if latest_row is None:
        return {
            "applicable": applicable,
            "ok": False,
            "target_type": target_type,
            "symbol": symbol,
            "requested_trade_date": requested_trade_date.isoformat() if requested_trade_date else None,
            "history_days": max(days, 1),
            "history": [],
            "error": error,
        }
    return {
        "applicable": applicable,
        "ok": str(latest_row.get("snapshot_status") or "") == "ok",
        "target_type": target_type,
        "symbol": symbol,
        "requested_trade_date": requested_trade_date.isoformat() if requested_trade_date else None,
        "trade_date": _date_text(latest_row.get("trade_date")),
        "code6": _text(latest_row.get("code6")),
        "full_code": _text(latest_row.get("full_code")),
        "tdx_name": _text(latest_row.get("tdx_name")),
        "flow_sector_name": _text(latest_row.get("flow_sector_name")),
        "seed_code": _text(latest_row.get("seed_code")),
        "flow_trade_date": _date_text(latest_row.get("flow_trade_date")),
        "snapshot_status": _enum_text(latest_row.get("snapshot_status"), VALID_SNAPSHOT_STATUSES),
        "selection_mode": _enum_text(latest_row.get("selection_mode"), VALID_SELECTION_MODES),
        "name_match_type": _enum_text(latest_row.get("name_match_type"), VALID_NAME_MATCH_TYPES),
        "main_net": _decimal_or_none(latest_row.get("main_net")),
        "large_order_net": _decimal_or_none(latest_row.get("large_order_net")),
        "medium_order_net": _decimal_or_none(latest_row.get("medium_order_net")),
        "small_order_net": _decimal_or_none(latest_row.get("small_order_net")),
        "row_count": _int_or_zero(latest_row.get("row_count")),
        "source": _text(latest_row.get("source")),
        "history_days": max(days, 1),
        "history": [
            {
                "trade_date": _date_text(item.get("trade_date")),
                "flow_trade_date": _date_text(item.get("flow_trade_date")),
                "snapshot_status": _enum_text(item.get("snapshot_status"), VALID_SNAPSHOT_STATUSES),
                "selection_mode": _enum_text(item.get("selection_mode"), VALID_SELECTION_MODES),
                "main_net": _decimal_or_none(item.get("main_net")),
                "large_order_net": _decimal_or_none(item.get("large_order_net")),
                "medium_order_net": _decimal_or_none(item.get("medium_order_net")),
                "small_order_net": _decimal_or_none(item.get("small_order_net")),
            }
            for item in history
        ],
        "error": error,
    }


def _normalize_history(value: Any) -> list[dict[str, Any]]:
    if not isinstance(value, list):
        return []
    out: list[dict[str, Any]] = []
    for item in value:
        if not isinstance(item, dict):
            continue
        trade_date = _parse_compact_date(item.get("date"))
        out.append(
            {
                "sector_name": _text(item.get("sector_name")),
                "trade_date": trade_date,
                "main_net": _decimal_or_none(item.get("main_net")),
                "large_order_net": _decimal_or_none(item.get("large_order_net")),
                "medium_order_net": _decimal_or_none(item.get("medium_order_net")),
                "small_order_net": _decimal_or_none(item.get("small_order_net")),
            }
        )
    out.sort(key=lambda item: item.get("trade_date") or date.min)
    return out


def _normalize_code6(value: Any) -> str | None:
    text = _text(value)
    if not text:
        return None
    lowered = text.lower()
    if lowered.startswith(("sh", "sz", "bj")):
        lowered = lowered[2:]
    return lowered if len(lowered) == 6 and lowered.isdigit() else None


def _normalize_full_code(value: Any) -> str | None:
    text = _text(value)
    if not text:
        return None
    lowered = text.lower()
    if len(lowered) == 8 and lowered[:2] in {"sh", "sz", "bj"} and lowered[2:].isdigit():
        return lowered
    if lowered.isdigit() and len(lowered) == 6:
        if lowered.startswith(("6", "5", "9")):
            return f"sh{lowered}"
        if lowered.startswith(("8", "4")):
            return f"bj{lowered}"
        return f"sz{lowered}"
    return None


def _parse_compact_date(value: Any) -> date | None:
    text = _text(value)
    if not text:
        return None
    try:
        if len(text) == 8 and text.isdigit():
            return datetime.strptime(text, "%Y%m%d").date()
        return date.fromisoformat(text)
    except ValueError:
        return None


def _date_text(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, date):
        return value.isoformat()
    return _text(value)


def _decimal_or_none(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _int_or_zero(value: Any) -> int:
    if value in (None, ""):
        return 0
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def _enum_text(value: Any, allowed: set[str]) -> str | None:
    text = _text(value)
    return text if text in allowed else None


def _text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None
