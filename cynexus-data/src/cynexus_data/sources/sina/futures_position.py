from __future__ import annotations

from datetime import date
from io import StringIO
import re
from typing import Any, Iterable

import pandas as pd
import requests
from html.parser import HTMLParser


DCE_VARIETIES = (
    "C", "CS", "A", "B", "M", "Y", "P", "JD", "L", "V", "PP", "J", "JM",
    "I", "EG", "RR", "EB", "PG", "LH", "PL",
)
_CONTRACT_RE = re.compile(r"^([A-Z]+)(\d{3,4})$")


def fetch_dce_position_rows(
    session: requests.Session,
    trade_date: date,
    *,
    varieties: Iterable[str] | None = None,
    base_url: str = "https://vip.stock.finance.sina.com.cn",
    timeout: float = 15,
) -> list[dict[str, Any]]:
    """Fetch DCE contract member rankings from Sina's daily position page.

    Sina publishes the three rankings in one HTML page per contract. The
    source is intentionally isolated from the AkShare adapter because it is a
    third-party fallback, not an exchange endpoint.
    """
    requested = {str(item).strip().upper() for item in (varieties or ()) if str(item).strip()} or set(DCE_VARIETIES)
    page_url = f"{base_url.rstrip('/')}/q/view/vFutures_Positions_cjcc.php"
    catalogue = session.get(
        page_url,
        params={"t_breed": "M2609", "t_date": trade_date.strftime("%Y-%m-%d")},
        headers=_headers(),
        timeout=timeout,
    )
    catalogue.raise_for_status()
    contracts = _contract_values(catalogue.content, requested)
    rows: list[dict[str, Any]] = []
    for contract in contracts:
        try:
            response = session.get(
                page_url,
                params={"t_breed": contract, "t_date": trade_date.strftime("%Y-%m-%d")},
                headers=_headers(),
                timeout=timeout,
            )
            response.raise_for_status()
            rows.extend(_parse_contract_page(response.content, contract))
        except Exception:  # noqa: BLE001
            # One expired/malformed contract must not discard the other DCE
            # contracts returned for the same publication date.
            continue
    return rows


def _contract_values(content: bytes, requested: set[str]) -> list[str]:
    html = content.decode("gbk", errors="ignore")
    values: list[str] = []
    seen: set[str] = set()
    parser = _OptionParser()
    parser.feed(html)
    for value in parser.values:
        contract = value.strip().upper()
        match = _CONTRACT_RE.fullmatch(contract)
        if not match or (requested and match.group(1) not in requested) or contract in seen:
            continue
        seen.add(contract)
        values.append(contract)
    return values


class _OptionParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.values: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag.lower() != "option":
            return
        for name, value in attrs:
            if name.lower() == "value" and value:
                self.values.append(value)


def _parse_contract_page(content: bytes, contract: str) -> list[dict[str, Any]]:
    html = content.decode("gbk", errors="ignore")
    tables = pd.read_html(StringIO(html))
    if len(tables) < 5:
        return []
    rankings = [_ranking_table(tables[index]) for index in (2, 3, 4)]
    variety_match = _CONTRACT_RE.fullmatch(contract)
    variety = variety_match.group(1) if variety_match else contract
    max_rows = max((len(item) for item in rankings), default=0)
    output: list[dict[str, Any]] = []
    for offset in range(max_rows):
        item: dict[str, Any] = {
            "symbol": contract,
            "variety": variety,
            "rank": offset + 1,
        }
        for prefix, ranking in zip(("vol", "long", "short"), rankings):
            if offset >= len(ranking):
                continue
            member, value, change = ranking[offset]
            item[f"{prefix}_party_name"] = member
            item[f"{prefix if prefix == 'vol' else prefix + '_open_interest'}"] = value
            item[f"{prefix}_chg" if prefix == "vol" else f"{prefix}_open_interest_chg"] = change
        output.append(item)
    return output


def _ranking_table(table: pd.DataFrame) -> list[tuple[str | None, Any, Any]]:
    if table.empty or len(table.columns) < 4:
        return []
    output: list[tuple[str | None, Any, Any]] = []
    for row in table.iloc[:-1].itertuples(index=False, name=None):
        member = _text(row[1])
        if not member or not _rank_number(row[0]):
            continue
        output.append((member, _number(row[2]), _number(row[3])))
    return output


def _rank_number(value: Any) -> int | None:
    try:
        number = int(float(str(value).replace(",", "").strip()))
    except (TypeError, ValueError):
        return None
    return number if number > 0 else None


def _number(value: Any) -> Any:
    text = str(value).replace(",", "").strip()
    return None if text in {"", "-", "--", "nan"} else text


def _text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _headers() -> dict[str, str]:
    return {
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Referer": "https://vip.stock.finance.sina.com.cn/",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124 Safari/537.36",
    }
