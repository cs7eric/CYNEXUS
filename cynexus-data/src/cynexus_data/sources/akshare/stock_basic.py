from __future__ import annotations

from typing import Any

from cynexus_data.domain.stock.stock_meta import normalize_stock_basic_record


def fetch_stock_code_name(client) -> list[dict[str, Any]]:
    ak = client.require_module()
    df = ak.stock_info_a_code_name()
    return normalize_akshare_stock_basic(df)


def normalize_akshare_stock_basic(df) -> list[dict[str, Any]]:
    if df is None:
        return []
    records = df.to_dict(orient="records")
    out: list[dict[str, Any]] = []
    for row in records:
        code = row.get("code") or row.get("证券代码") or row.get("股票代码")
        name = row.get("name") or row.get("证券简称") or row.get("股票简称")
        if not code:
            continue
        out.append(normalize_stock_basic_record({"code": str(code).zfill(6), "name": name or "", "source": "akshare"}))
    return out
