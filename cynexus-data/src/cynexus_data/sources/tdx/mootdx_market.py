from __future__ import annotations

from typing import Any

from cynexus_data.sources.tdx.mootdx import create_mootdx_client


def fetch_quotes(symbols: list[str], *, server: tuple[str, int] | None = None) -> list[dict[str, Any]]:
    client = create_mootdx_client(server=server)
    try:
        frame = client.quotes(symbol=[_plain_code(symbol) for symbol in symbols])
        return _records(frame)
    finally:
        close = getattr(client, "close", None)
        if callable(close):
            close()


def fetch_transactions(symbol: str, *, trade_date: str | None = None, server: tuple[str, int] | None = None) -> list[dict[str, Any]]:
    client = create_mootdx_client(server=server)
    try:
        kwargs = {"symbol": _plain_code(symbol)}
        if trade_date:
            kwargs["date"] = trade_date.replace("-", "")
        frame = client.transaction(**kwargs)
        return _records(frame)
    finally:
        close = getattr(client, "close", None)
        if callable(close):
            close()


def fetch_order_book(symbol: str, *, server: tuple[str, int] | None = None) -> dict[str, Any]:
    rows = fetch_quotes([symbol], server=server)
    return rows[0] if rows else {}


def _records(value: Any) -> list[dict[str, Any]]:
    if value is None:
        return []
    if hasattr(value, "to_dict"):
        return value.to_dict(orient="records")
    if isinstance(value, list):
        return [item if isinstance(item, dict) else _object_dict(item) for item in value]
    if isinstance(value, dict):
        return [value]
    return [_object_dict(value)]


def _object_dict(value: Any) -> dict[str, Any]:
    raw = getattr(value, "__dict__", None)
    return dict(raw) if isinstance(raw, dict) else {"value": str(value)}


def _plain_code(code: str) -> str:
    value = str(code or "").strip().lower()
    if value.startswith(("sh", "sz", "bj")):
        value = value[2:]
    if "." in value:
        value = value.split(".", 1)[0]
    return value[-6:].zfill(6)
