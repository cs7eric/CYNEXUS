from __future__ import annotations

import time
from datetime import date
from typing import Any

from cynexus_data.domain.market.etf import normalize_etf_code, numeric


def fetch_etf_spot(ak: Any, *, attempts: int = 3) -> list[dict[str, Any]]:
    frame = _call_with_retry(ak.fund_etf_spot_em, attempts=attempts)
    if frame is None or frame.empty:
        return []
    return [dict(row) for row in frame.to_dict("records")]


def fetch_etf_classification(ak: Any, *, attempts: int = 2) -> tuple[dict[str, dict[str, str]], list[str]]:
    """Load EastMoney's index-fund category catalog as a code-keyed authority hint.

    The catalog is best-effort because ETF quotes must remain available when the
    fund-information endpoint is throttled. More specific industry rows are
    allowed to override the broad stock-index bucket.
    """
    queries = (
        ("大盘指数", "broad"),
        ("中盘指数", "broad"),
        ("小盘指数", "broad"),
        ("股票指数", "broad"),
        ("行业主题", "sector"),
    )
    priority = {"broad": 10, "sector": 20}
    mapping: dict[str, dict[str, str]] = {}
    errors: list[str] = []
    for symbol, category in queries:
        try:
            frame = _call_with_retry(
                lambda symbol=symbol: ak.fund_info_index_em(symbol=symbol, indicator="全部"),
                attempts=attempts,
            )
            if frame is None or frame.empty:
                continue
            for row in frame.to_dict("records"):
                code = normalize_etf_code(row.get("基金代码") or row.get("代码"))
                if not code:
                    continue
                current = mapping.get(code)
                if current and priority.get(current["category"], 0) > priority[category]:
                    continue
                mapping[code] = {
                    "category": category,
                    "source": "eastmoney.fund_index_classification",
                }
        except Exception as exc:  # noqa: BLE001 - optional metadata source.
            errors.append(f"{symbol}: {exc}")
    return mapping, errors


def fetch_etf_share_sse(ak: Any, trade_date: date) -> dict[str, float]:
    frame = ak.fund_etf_scale_sse(date=trade_date.strftime("%Y%m%d"))
    if frame is None or frame.empty:
        return {}
    return {
        normalize_etf_code(row.get("基金代码")): numeric(row.get("基金份额"))
        for row in frame.to_dict("records")
        if normalize_etf_code(row.get("基金代码"))
    }


def fetch_etf_share_szse(ak: Any) -> dict[str, float]:
    frame = ak.fund_etf_scale_szse()
    if frame is None or frame.empty:
        return {}
    return {
        normalize_etf_code(row.get("基金代码")): numeric(row.get("基金份额"))
        for row in frame.to_dict("records")
        if normalize_etf_code(row.get("基金代码"))
    }


def _call_with_retry(func: Any, *, attempts: int = 3, base_sleep: float = 0.8):
    last_error: Exception | None = None
    for attempt in range(max(1, attempts)):
        try:
            return func()
        except Exception as exc:  # noqa: BLE001 - source boundary returns a job error.
            last_error = exc
            if attempt + 1 < attempts:
                time.sleep(base_sleep * (2**attempt))
    if last_error is not None:
        raise last_error
    return None
