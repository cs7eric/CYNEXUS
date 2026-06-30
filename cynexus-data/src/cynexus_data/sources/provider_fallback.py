from __future__ import annotations

from dataclasses import dataclass, field
from typing import TYPE_CHECKING, Any, Callable, Mapping

from cynexus_data.config.settings import load_yaml_config
from cynexus_data.core.market_symbol import eastmoney_secid, normalize_market_symbol
from cynexus_data.sources.eastmoney.adapter import adapt_kline_payload as adapt_eastmoney_kline_payload
from cynexus_data.sources.sina.adapter import adapt_minute_payload as adapt_sina_minute_payload
from cynexus_data.sources.tdx.mootdx import create_mootdx_client, fetch_mootdx_kline_rows
from cynexus_data.sources.tencent.adapter import adapt_kline_payload as adapt_tencent_kline_payload
from cynexus_data.sources.tencent.adapter import adapt_minute_query_payload as adapt_tencent_minute_query_payload

if TYPE_CHECKING:
    from cynexus_data.runtime.context import HookBundle


FetchCallable = Callable[[], list[dict[str, Any]]]


@dataclass(frozen=True)
class ProviderAttempt:
    provider: str
    ok: bool
    rows: int = 0
    error: str | None = None


@dataclass(frozen=True)
class ProviderFetchResult:
    items: list[dict[str, Any]]
    source: str
    stale: bool = False
    data_quality: str = "ok"
    attempts: list[ProviderAttempt] = field(default_factory=list)

    def meta(self) -> dict[str, Any]:
        return {
            "source": self.source,
            "stale": self.stale,
            "dataQuality": self.data_quality,
            "attempts": [attempt.__dict__ for attempt in self.attempts],
        }


def provider_plan(dataset: str, period: str | None = None, requested: Any = None) -> list[str]:
    if requested not in (None, "", []):
        return _unique(_normalize_list(requested))
    config = load_yaml_config("sources.yaml").get("sources") or {}
    if dataset == "kline_daily":
        return _unique(((config.get("kline") or {}).get("daily") or {}).get("providers") or ["tencent", "eastmoney", "tdx"])
    if dataset == "kline_minute":
        return _unique(((config.get("kline") or {}).get("minute") or {}).get("providers") or ["tdx", "sina", "eastmoney", "tencent"])
    key = str(dataset)
    entry = config.get(key) or {}
    return _unique(entry.get("providers") or [])


def fetch_with_fallback(providers: list[str], fetchers: Mapping[str, FetchCallable]) -> ProviderFetchResult:
    attempts: list[ProviderAttempt] = []
    for provider in providers:
        fetcher = fetchers.get(provider)
        if fetcher is None:
            attempts.append(ProviderAttempt(provider=provider, ok=False, error="provider_not_registered"))
            continue
        try:
            items = fetcher()
        except Exception as exc:  # noqa: BLE001 - error is captured for fallback metadata.
            attempts.append(ProviderAttempt(provider=provider, ok=False, error=str(exc)))
            continue
        attempts.append(ProviderAttempt(provider=provider, ok=bool(items), rows=len(items)))
        if items:
            return ProviderFetchResult(items=items, source=provider, stale=False, data_quality="ok", attempts=attempts)
    return ProviderFetchResult(items=[], source="none", stale=True, data_quality="error", attempts=attempts)


def fetch_daily_kline_with_fallback(hooks: HookBundle, *, symbol: str, providers: list[str], adjust: str = "qfq", limit: int = 2000) -> ProviderFetchResult:
    market_symbol = normalize_market_symbol(symbol)
    fetchers: dict[str, FetchCallable] = {
        "tencent": lambda: _fetch_tencent_daily(hooks, market_symbol, adjust=adjust, limit=limit),
        "eastmoney": lambda: _fetch_eastmoney_daily(hooks, market_symbol, adjust=adjust, limit=limit),
        "mootdx": lambda: _fetch_mootdx_daily(market_symbol, adjust=adjust, limit=limit),
    }
    return fetch_with_fallback([p for p in providers if p != "tdx"], fetchers)


def fetch_minute_kline_with_fallback(hooks: HookBundle, *, symbol: str, providers: list[str], period: int = 1, limit: int = 500) -> ProviderFetchResult:
    market_symbol = normalize_market_symbol(symbol)
    fetchers: dict[str, FetchCallable] = {
        "sina": lambda: adapt_sina_minute_payload(hooks.sina.get_client().fetch_minute_kline_raw(market_symbol, scale=period, datalen=limit)),
        "eastmoney": lambda: _fetch_eastmoney_minute(hooks, market_symbol, period=period, limit=limit),
        "tencent": lambda: _fetch_tencent_minute(hooks, market_symbol, period=period, limit=limit),
        "mootdx": lambda: _fetch_mootdx_minute(market_symbol, period=period, limit=limit),
    }
    return fetch_with_fallback([p for p in providers if p != "tdx"], fetchers)


def _fetch_tencent_daily(hooks: HookBundle, symbol: str, *, adjust: str, limit: int) -> list[dict[str, Any]]:
    adjust_key = {"qfq": "qfqday", "hfq": "hfqday", "none": "day"}.get(adjust, "qfqday")
    payload = hooks.tencent.get_client().fetch_kline_raw(symbol, unit="day", limit=limit, adjust=adjust)
    return adapt_tencent_kline_payload(payload, symbol, key=adjust_key)


def _fetch_tencent_minute(hooks: HookBundle, symbol: str, *, period: int, limit: int) -> list[dict[str, Any]]:
    payload = hooks.tencent.get_client().fetch_kline_raw(symbol, unit=f"m{int(period)}", limit=limit, adjust="none")
    rows = adapt_tencent_kline_payload(payload, symbol, key=f"m{int(period)}")
    if rows:
        return rows
    if int(period) != 1:
        return rows
    minute_payload = hooks.tencent.get_client().fetch_minute_query_raw(symbol)
    return adapt_tencent_minute_query_payload(minute_payload, symbol)


def _fetch_eastmoney_daily(hooks: HookBundle, symbol: str, *, adjust: str, limit: int) -> list[dict[str, Any]]:
    fqt = {"none": "0", "qfq": "1", "hfq": "2"}.get(adjust, "1")
    payload = hooks.eastmoney.get_client().fetch_kline_raw(eastmoney_secid(symbol), klt="101", fqt=fqt, limit=limit)
    return adapt_eastmoney_kline_payload(payload)


def _fetch_eastmoney_minute(hooks: HookBundle, symbol: str, *, period: int, limit: int) -> list[dict[str, Any]]:
    payload = hooks.eastmoney.get_client().fetch_kline_raw(eastmoney_secid(symbol), klt=str(int(period)), fqt="0", limit=limit)
    return adapt_eastmoney_kline_payload(payload)


def _fetch_mootdx_daily(symbol: str, *, adjust: str, limit: int) -> list[dict[str, Any]]:
    client = create_mootdx_client()
    return fetch_mootdx_kline_rows(client, symbol=symbol, period="day", adjust=adjust, limit=limit)


def _fetch_mootdx_minute(symbol: str, *, period: int, limit: int) -> list[dict[str, Any]]:
    client = create_mootdx_client()
    return fetch_mootdx_kline_rows(client, symbol=symbol, period=f"{int(period)}m", adjust="none", limit=limit)


def _normalize_list(value: Any) -> list[str]:
    if isinstance(value, str):
        return [item.strip().lower() for item in value.split(",") if item.strip()]
    return [str(item).strip().lower() for item in value if str(item).strip()]


def _unique(values: list[str]) -> list[str]:
    out: list[str] = []
    for value in values:
        if value and value not in out:
            out.append(value)
    return out
