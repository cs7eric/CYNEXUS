from __future__ import annotations

from datetime import datetime
from typing import Any


def build_limit_board_snapshot(
    row: dict[str, Any],
    *,
    previous: dict[str, Any] | None = None,
    source: str = "eltdx.quotes",
) -> dict[str, Any] | None:
    """Normalize one limit-board quote into structured best-quote fields.

    ``row`` is the normalized market-indicator row. A normal quote is only
    retained when the previous tracked board state was sealed, so an opening
    board is recorded once without filling the table with unrelated stocks.
    """
    current_status = str(row.get("limit_status") or "")
    previous_status = str((previous or {}).get("board_status") or "")
    previous_direction = str((previous or {}).get("direction") or "")
    direction = _direction(current_status) or previous_direction
    if direction not in {"up", "down"}:
        return None

    current_base_status = _base_status(current_status)
    if current_base_status is None:
        if previous_status not in {"sealed", "resealed"}:
            return None
        board_status = "opened"
        is_sealed = False
    elif current_base_status == "sealed" and previous_status in {"opened", "touch"}:
        board_status = "resealed"
        is_sealed = True
    elif current_base_status == "touch" and previous_status in {"sealed", "resealed"}:
        board_status = "opened"
        is_sealed = False
    else:
        board_status = current_base_status
        is_sealed = current_base_status == "sealed"

    payload = row.get("raw_payload") if isinstance(row.get("raw_payload"), dict) else {}
    bids = _levels(payload.get("buy_levels") or payload.get("bids"))
    asks = _levels(payload.get("sell_levels") or payload.get("asks"))
    limit_price = _number(row.get("limit_up_price" if direction == "up" else "limit_down_price"))
    queue_side = "buy" if direction == "up" else "sell"
    queue_levels = bids if queue_side == "buy" else asks
    queue = _find_limit_level(queue_levels, limit_price)
    queue_price = queue[0] if queue else None
    queue_volume = queue[1] if queue else None
    sealed_amount = queue_price * queue_volume * 100 if is_sealed and queue_price is not None and queue_volume is not None else None
    captured_at = row.get("captured_at")
    if not isinstance(captured_at, datetime):
        raise ValueError("captured_at must be a datetime")

    result: dict[str, Any] = {
        "trade_date": captured_at.date(),
        "code": str(row.get("code") or "").strip()[-6:].zfill(6),
        "name": row.get("name"),
        "market": row.get("market"),
        "direction": direction,
        "board_status": board_status,
        "is_sealed": is_sealed,
        "last_price": _number(row.get("last_price")),
        "prev_close": _number(row.get("prev_close")),
        "change_pct": _number(row.get("change_pct")),
        "limit_price": limit_price,
        "queue_side": queue_side,
        "queue_price": queue_price,
        "queue_volume": queue_volume,
        "sealed_amount": sealed_amount,
        "provider_time_raw": _integer(payload.get("time_raw") or payload.get("update_time_raw")),
        "source": source,
        "captured_at": captured_at,
        "payload": {"limit_status": current_status},
    }
    for prefix, levels in (("bid", bids), ("ask", asks)):
        level = levels[0] if levels else (None, None)
        result[f"{prefix}_price_1"] = level[0]
        result[f"{prefix}_volume_1"] = level[1]
    return result


def build_limit_board_sample(
    row: dict[str, Any],
    *,
    prior_direction: str | None = None,
    source: str = "eltdx.quotes",
) -> dict[str, Any] | None:
    """Build one transient best-quote sample for a tracked board stock.

    Unlike ``build_limit_board_snapshot``, this keeps a normal quote for a
    previously discovered board stock so the session aggregator can observe
    an opening and a later reseal without persisting provider payloads.
    """
    status = str(row.get("limit_status") or "normal")
    live_direction = last_price_limit_direction(row)
    direction = live_direction or _direction(status) or str(prior_direction or "")
    if direction not in {"up", "down"}:
        return None
    payload = row.get("raw_payload") if isinstance(row.get("raw_payload"), dict) else {}
    bids = _levels(payload.get("buy_levels") or payload.get("bids"))
    asks = _levels(payload.get("sell_levels") or payload.get("asks"))
    limit_price = _number(row.get("limit_up_price" if direction == "up" else "limit_down_price"))
    queue_side = "buy" if direction == "up" else "sell"
    queue = _find_limit_level(bids if queue_side == "buy" else asks, limit_price)
    queue_price = queue[0] if queue else None
    queue_volume = queue[1] if queue else None
    # High/low touching the limit is not a current board. The monitor only
    # treats last price at the limit as on-board; otherwise this is an open.
    is_on_board = live_direction is not None
    is_sealed = is_on_board and status.endswith("sealed")
    captured_at = row.get("captured_at")
    if not isinstance(captured_at, datetime):
        raise ValueError("captured_at must be a datetime")
    return {
        "captured_at": captured_at,
        "code": str(row.get("code") or "").strip()[-6:].zfill(6),
        "name": row.get("name"),
        "market": row.get("market"),
        "direction": direction,
        "limit_status": status,
        "board_status": "sealed" if is_sealed else "touch" if is_on_board else "opened",
        "is_on_board": is_on_board,
        "is_sealed": is_sealed,
        "last_price": _number(row.get("last_price")),
        "limit_price": limit_price,
        "bid_price_1": bids[0][0] if bids else None,
        "bid_volume_1": bids[0][1] if bids else None,
        "ask_price_1": asks[0][0] if asks else None,
        "ask_volume_1": asks[0][1] if asks else None,
        "sealed_amount": queue_price * queue_volume * 100 if is_sealed and queue_price is not None and queue_volume is not None else None,
        "turnover_amount": _number(row.get("amount")),
        "source": source,
    }


def last_price_limit_direction(row: dict[str, Any]) -> str | None:
    """Return up/down only when last price is at the limit price.

    Market-indicator ``limit_status`` also flags a session high/low that once
    touched the limit. That is useful for "touched today" counts, but it must
    not enroll a stock into the limit-board monitor after the last price has
    left the board.
    """
    last = _number(row.get("last_price"))
    if last is None:
        return None
    up = _number(row.get("limit_up_price"))
    down = _number(row.get("limit_down_price"))
    at_up = up is not None and _near_price(last, up)
    at_down = down is not None and _near_price(last, down)
    if at_up and not at_down:
        return "up"
    if at_down and not at_up:
        return "down"
    if at_up and at_down:
        return _direction(str(row.get("limit_status") or ""))
    return None


def _direction(status: str) -> str | None:
    if status.startswith("limit_up_"):
        return "up"
    if status.startswith("limit_down_"):
        return "down"
    return None


def _near_price(value: float, target: float) -> bool:
    return abs(value - target) <= max(0.011, abs(target) * 0.0005)


def _base_status(status: str) -> str | None:
    if status.endswith("sealed"):
        return "sealed"
    if status.endswith("touch"):
        return "touch"
    return None


def _levels(value: Any) -> list[tuple[float | None, float | None]]:
    if isinstance(value, dict):
        value = value.get("items") or value.get("levels") or list(value.values())
    if not isinstance(value, (list, tuple)):
        return []
    result: list[tuple[float | None, float | None]] = []
    for item in value[:5]:
        if isinstance(item, dict):
            price = _number(item.get("price"))
            volume = _number(_first_present(item, "volume", "vol", "quantity"))
        elif isinstance(item, (list, tuple)):
            price = _number(item[0]) if len(item) > 0 else None
            volume = _number(item[1]) if len(item) > 1 else None
        else:
            price = _number(getattr(item, "price", None))
            volume = _number(getattr(item, "volume", None))
        result.append((price, volume))
    return result


def _first_present(value: dict[str, Any], *names: str) -> Any:
    for name in names:
        if value.get(name) not in (None, ""):
            return value[name]
    return None


def _find_limit_level(levels: list[tuple[float | None, float | None]], limit_price: float | None):
    if not levels:
        return None
    if limit_price is not None:
        for level in levels:
            if level[0] is not None and abs(level[0] - limit_price) <= max(0.011, abs(limit_price) * 0.0005):
                return level
    return levels[0]


def _number(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", ""))
    except (TypeError, ValueError):
        return None


def _integer(value: Any) -> int | None:
    if value in (None, ""):
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None
