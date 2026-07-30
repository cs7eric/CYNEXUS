from __future__ import annotations

from datetime import date
from typing import Any, Callable, Iterable


AKSHARE_EXCHANGE_VARIETIES: dict[str, tuple[str, ...]] = {
    "CFFEX": ("IF", "IC", "IM", "IH", "T", "TF", "TS", "TL"),
    "SHFE": ("CU", "AL", "ZN", "PB", "NI", "SN", "AU", "AG", "RB", "WR", "HC", "FU", "BU", "RU", "SP", "SS"),
    "CZCE": ("CF", "SR", "TA", "OI", "MA", "FG", "RM", "SA", "PK", "AP", "UR", "PF", "PX", "SH", "SM", "SF"),
    "DCE": ("C", "CS", "A", "B", "M", "Y", "P", "JD", "L", "V", "PP", "J", "JM", "I", "EG", "RR", "EB", "PG", "LH"),
    "GFEX": ("PG", "SI", "LC"),
    "INE": ("SC", "NR", "LU", "BC", "EC"),
}


def fetch_position_rows(
    ak: Any,
    trade_date: date,
    *,
    exchanges: Iterable[str] | None = None,
    varieties: Iterable[str] | None = None,
) -> dict[str, list[dict[str, Any]]]:
    """Fetch raw member-ranking rows grouped by exchange.

    AkShare exposes exchange-specific wrappers but normalizes no source-health
    metadata. This adapter keeps the call boundary explicit so the pipeline can
    record per-exchange failures and select only one provider later.
    """
    selected_exchanges = [str(item).strip().upper() for item in (exchanges or AKSHARE_EXCHANGE_VARIETIES)]
    requested_varieties = {str(item).strip().upper() for item in (varieties or []) if str(item).strip()}
    output: dict[str, list[dict[str, Any]]] = {}
    for exchange in selected_exchanges:
        if exchange not in AKSHARE_EXCHANGE_VARIETIES:
            raise ValueError(f"unsupported AkShare futures exchange: {exchange}")
        exchange_varieties = [item for item in AKSHARE_EXCHANGE_VARIETIES[exchange] if not requested_varieties or item in requested_varieties]
        if exchange == "CFFEX":
            raw = ak.get_cffex_rank_table(_date_arg(trade_date), exchange_varieties)
        elif exchange == "SHFE":
            raw = ak.get_shfe_rank_table(_date_arg(trade_date), exchange_varieties)
        elif exchange == "CZCE":
            raw = ak.get_rank_table_czce(_date_arg(trade_date))
        elif exchange == "DCE":
            raw = ak.futures_dce_position_rank(_date_arg(trade_date), exchange_varieties)
        elif exchange == "GFEX":
            raw = ak.futures_gfex_position_rank(_date_arg(trade_date), exchange_varieties)
        else:
            # AkShare's SHFE report can contain INE contracts, but not with a
            # stable INE exchange identity. The caller may map these rows only
            # after the exchange-specific identity has been validated.
            raw = ak.get_shfe_rank_table(_date_arg(trade_date), exchange_varieties)
        output[exchange] = _records(raw, requested_varieties)
    return output


def _records(raw: Any, requested_varieties: set[str]) -> list[dict[str, Any]]:
    if raw is None or raw is False:
        return []
    if hasattr(raw, "to_dict"):
        rows = raw.to_dict("records")
    elif isinstance(raw, dict):
        rows = []
        for symbol, frame in raw.items():
            if hasattr(frame, "to_dict"):
                rows.extend(frame.to_dict("records"))
            elif isinstance(frame, list):
                rows.extend(frame)
    elif isinstance(raw, list):
        rows = raw
    else:
        return []
    output = []
    for row in rows:
        item = dict(row)
        variety = str(item.get("variety") or item.get("variety_code") or "").strip().upper()
        if requested_varieties and variety and variety not in requested_varieties:
            continue
        output.append(item)
    return output


def _date_arg(value: date) -> str:
    return value.strftime("%Y%m%d")
