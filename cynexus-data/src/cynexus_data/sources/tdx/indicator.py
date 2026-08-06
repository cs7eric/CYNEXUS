from __future__ import annotations

from datetime import datetime
import random
import time
from typing import Any

from cynexus_data.domain.market.market_indicator import normalize_indicator_quote


def create_eltdx_indicator_client(*, pool_size: int = 8, timeout: float = 8.0, batch_size: int = 80):
    try:
        import eltdx  # type: ignore
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError("eltdx 1.2.0 is required for market indicator refresh") from exc
    client = eltdx.TdxClient(
        pool_size=max(1, int(pool_size)),
        timeout=float(timeout),
        batch_size=max(1, int(batch_size)),
    )
    client.connect()
    return client


def fetch_indicator_snapshots(
    client: Any,
    metadata: dict[str, dict[str, Any]],
    *,
    batch_size: int = 80,
    captured_at: datetime | None = None,
    retry_attempts: int = 2,
    retry_backoff_seconds: float = 0.25,
    request_spacing_seconds: float = 0.02,
    fallback_batch_size: int = 80,
) -> list[dict[str, Any]]:
    source_codes = [_plain_code(value) for value in (client.get_a_share_codes_all() or [])]
    codes = [code for code in source_codes if code in metadata]
    captured = captured_at or datetime.now().astimezone()
    rows: list[dict[str, Any]] = []
    effective_batch_size = max(1, int(batch_size))
    for start in range(0, len(codes), effective_batch_size):
        chunk = [_tdx_code(code, metadata[code].get("market")) for code in codes[start : start + effective_batch_size]]
        quotes = _fetch_quote_batch(
            client,
            chunk,
            retry_attempts=max(0, int(retry_attempts)),
            retry_backoff_seconds=max(0.0, float(retry_backoff_seconds)),
            fallback_batch_size=max(1, int(fallback_batch_size)),
        )
        for quote in quotes:
            raw = _object_to_dict(quote)
            full_code = str(raw.get("full_code") or raw.get("fullCode") or "").lower()
            code = _plain_code(full_code or raw.get("code"))
            info = metadata.get(code)
            if not info:
                continue
            normalized = normalize_indicator_quote(
                raw,
                code=code,
                name=info.get("name"),
                market=info.get("market"),
                captured_at=captured,
                limit_up_threshold=float(info.get("limit_up_threshold") or 10),
                float_shares=_float(info.get("float_shares")),
            )
            if normalized["last_price"] is None:
                continue
            normalized["industry"] = info.get("industry")
            rows.append(normalized)
        if request_spacing_seconds > 0 and start + effective_batch_size < len(codes):
            time.sleep(float(request_spacing_seconds))
    return rows


def _fetch_quote_batch(
    client: Any,
    codes: list[str],
    *,
    retry_attempts: int,
    retry_backoff_seconds: float,
    fallback_batch_size: int,
) -> list[Any]:
    """Fetch one quote batch with bounded retry and payload-size fallback.

    The TDX server is not treated as an HTTP endpoint with a guaranteed rate
    limit contract. A transient disconnect, timeout, or empty page is retried
    with exponential backoff; a repeatedly failing large payload is split once
    into smaller requests so a single oversized batch cannot discard a whole
    capture. Persistent failure still raises and prevents publishing an
    incomplete snapshot.
    """
    last_error: Exception | None = None
    for attempt in range(retry_attempts + 1):
        try:
            rows = _extract_rows(client.get_quote(codes))
            if rows or not codes:
                return rows
            last_error = RuntimeError("TDX quote batch returned no rows")
        except Exception as exc:  # noqa: BLE001 - provider/client exceptions vary by eltdx transport.
            last_error = exc
        if attempt < retry_attempts:
            delay = retry_backoff_seconds * (2**attempt) + random.uniform(0.0, 0.05)
            if delay > 0:
                time.sleep(delay)
    if len(codes) > fallback_batch_size:
        midpoint = len(codes) // 2
        left = _fetch_quote_batch(
            client,
            codes[:midpoint],
            retry_attempts=retry_attempts,
            retry_backoff_seconds=retry_backoff_seconds,
            fallback_batch_size=fallback_batch_size,
        )
        right = _fetch_quote_batch(
            client,
            codes[midpoint:],
            retry_attempts=retry_attempts,
            retry_backoff_seconds=retry_backoff_seconds,
            fallback_batch_size=fallback_batch_size,
        )
        return left + right
    raise RuntimeError(f"TDX quote batch failed after {retry_attempts + 1} attempt(s)") from last_error


def _extract_rows(value: Any) -> list[Any]:
    if value is None:
        return []
    for name in ("items", "rows", "quotes"):
        if hasattr(value, name):
            value = getattr(value, name)
            break
    if isinstance(value, dict):
        value = value.get("items") or value.get("rows") or [value]
    return list(value) if isinstance(value, (list, tuple)) else []


def _object_to_dict(value: Any) -> dict[str, Any]:
    if isinstance(value, dict):
        return dict(value)
    if hasattr(value, "__dataclass_fields__"):
        from dataclasses import asdict

        return asdict(value)
    mapping = dict(getattr(value, "__dict__", {}) or {})
    if mapping:
        return mapping
    return {
        "full_code": getattr(value, "full_code", None),
        "code": getattr(value, "code", None),
        "last_price": getattr(value, "last_price", None),
        "pre_close_price": getattr(value, "pre_close_price", None),
        "open_price": getattr(value, "open_price", None),
        "high_price": getattr(value, "high_price", None),
        "low_price": getattr(value, "low_price", None),
        "change_pct": getattr(value, "change_pct", None),
        "amount": getattr(value, "amount", None),
        "total_hand": getattr(value, "total_hand", None),
        "buy_levels": getattr(value, "buy_levels", None) or getattr(value, "bids", None),
        "sell_levels": getattr(value, "sell_levels", None) or getattr(value, "asks", None),
    }


def _float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(str(value).replace(",", "").replace("%", ""))
    except (TypeError, ValueError):
        return None


def _plain_code(value: Any) -> str:
    text = str(value or "").strip().lower()
    if text.startswith(("sh", "sz", "bj")):
        text = text[2:]
    return text.zfill(6)


def _tdx_code(code: str, market: Any) -> str:
    prefix = {"SH": "sh", "SZ": "sz", "BJ": "bj"}.get(str(market or "").upper())
    if prefix:
        return f"{prefix}{_plain_code(code)}"
    return _plain_code(code)
