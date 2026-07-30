from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from decimal import Decimal, InvalidOperation
import re
from typing import Any, Iterable, Mapping


FUTURES_EXCHANGES = ("CFFEX", "SHFE", "CZCE", "DCE", "GFEX", "INE")
POSITION_RANK_TYPES = ("VOLUME", "LONG", "SHORT")
_SYNTHETIC_RANKS = {"999", "999.0", "合计", "total"}
_VARIETY_RE = re.compile(r"^[A-Z_]+")


@dataclass(frozen=True)
class PositionRank:
    trade_date: date
    exchange_code: str
    scope_type: str
    instrument_code: str
    variety_code: str
    contract_code: str | None
    rank_type: str
    rank_no: int
    member_name: str | None
    rank_value: Decimal
    rank_change: Decimal | None
    selected_source: str


def normalize_position_rows(
    rows: Iterable[Mapping[str, Any]],
    *,
    trade_date: date,
    exchange_code: str,
    selected_source: str,
    scope_type: str = "CONTRACT",
) -> list[PositionRank]:
    """Expand one exchange row into three independent rank facts.

    Exchange reports put volume, long, and short members in separate columns on
    the same visual row. They are separate rankings and must not share a member
    identity or be joined by rank beyond the source row itself.
    """
    exchange = str(exchange_code).strip().upper()
    scope = str(scope_type).strip().upper()
    if exchange not in FUTURES_EXCHANGES:
        raise ValueError(f"unsupported futures exchange: {exchange}")
    if scope not in {"CONTRACT", "VARIETY"}:
        raise ValueError(f"unsupported futures position scope: {scope}")

    normalized: list[PositionRank] = []
    for raw in rows:
        instrument = _text(raw.get("instrument_code") or raw.get("symbol") or raw.get("合约"))
        variety = _text(raw.get("variety_code") or raw.get("variety") or raw.get("品种"))
        if not instrument and not variety:
            continue
        instrument = (instrument or variety or "").upper()
        variety = (variety or _VARIETY_RE.match(instrument).group(0) if _VARIETY_RE.match(instrument) else instrument).upper()
        contract = instrument if scope == "CONTRACT" else None
        instrument_key = instrument if scope == "CONTRACT" else variety
        rank_no = _rank_no(raw.get("rank") or raw.get("排名"))
        if rank_no is None or str(rank_no) in _SYNTHETIC_RANKS:
            continue
        for rank_type, member_key, value_key, change_key in (
            ("VOLUME", "vol_party_name", "vol", "vol_chg"),
            ("LONG", "long_party_name", "long_open_interest", "long_open_interest_chg"),
            ("SHORT", "short_party_name", "short_open_interest", "short_open_interest_chg"),
        ):
            value = _decimal(raw.get(value_key) or raw.get(_cn_key(rank_type, "value")))
            if value is None:
                continue
            normalized.append(PositionRank(
                trade_date=trade_date,
                exchange_code=exchange,
                scope_type=scope,
                instrument_code=instrument_key,
                variety_code=variety,
                contract_code=contract,
                rank_type=rank_type,
                rank_no=rank_no,
                member_name=_text(raw.get(member_key)),
                rank_value=value,
                rank_change=_decimal(raw.get(change_key)),
                selected_source=selected_source,
            ))
    return normalized


def _cn_key(rank_type: str, suffix: str) -> str:
    return {"VOLUME": "成交量", "LONG": "多头持仓量", "SHORT": "空头持仓量"}[rank_type]


def _rank_no(value: Any) -> int | None:
    if value is None or str(value).strip() in _SYNTHETIC_RANKS:
        return None
    try:
        number = int(float(str(value).replace(",", "").strip()))
    except (TypeError, ValueError):
        return None
    return number if number > 0 else None


def _decimal(value: Any) -> Decimal | None:
    if value in (None, "", "-", "--"):
        return None
    try:
        return Decimal(str(value).replace(",", "").strip())
    except (InvalidOperation, TypeError, ValueError):
        return None


def _text(value: Any) -> str | None:
    if value is None:
        return None
    value = str(value).strip()
    return value or None
