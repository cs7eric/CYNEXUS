from __future__ import annotations

from typing import Any

from cynexus_data.core.market_symbol import normalize_market_symbol


def create_eltdx_stock_profile_client(pool_size: int = 4, timeout: float = 8.0):
    try:
        import eltdx  # type: ignore
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError("eltdx is required for stock profile share lookup") from exc
    client = eltdx.TdxClient(pool_size=pool_size, timeout=timeout)
    client.connect()
    return client


def fetch_stock_share_profile(client: Any, symbol: str) -> dict[str, Any]:
    full_code = _to_eltdx_code(symbol)
    table = client.helpers.stock_profile_table(full_code)
    rows = getattr(table, "rows", None) or ()
    profile = rows[0] if rows else None
    return {
        "symbol": str(symbol),
        "full_code": full_code,
        "circulating_shares": _float_or_none(_get(profile, "circulating_shares")),
        "total_shares": _float_or_none(_get(profile, "total_shares")),
        "source": "eltdx.helpers.stock_profile_table",
    }


def _to_eltdx_code(symbol: str) -> str:
    prefixed = normalize_market_symbol(symbol)
    if prefixed.startswith(("sh", "sz", "bj")):
        return prefixed
    return str(symbol).strip().lower()


def _get(obj: Any, name: str) -> Any:
    if obj is None:
        return None
    if isinstance(obj, dict):
        return obj.get(name)
    return getattr(obj, name, None)


def _float_or_none(value: Any) -> float | None:
    if value in (None, ""):
        return None
    return float(value)
