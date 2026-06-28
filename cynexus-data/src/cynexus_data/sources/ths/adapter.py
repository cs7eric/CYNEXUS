from __future__ import annotations

from typing import Any

from cynexus_data.sources.ths.parser import normalize_fund_flow_rows


def adapt_fund_flow_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return normalize_fund_flow_rows(rows)
