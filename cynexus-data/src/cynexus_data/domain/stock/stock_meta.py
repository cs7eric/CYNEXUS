from __future__ import annotations


def market_from_code(code: str) -> str:
    c = code.strip()
    if c.startswith(("5", "6", "9")):
        return "SH"
    if c.startswith(("8", "4", "920")):
        return "BJ"
    return "SZ"


def board_from_code(code: str) -> str:
    c = code.strip()
    if c.startswith(("600", "601", "603", "605")):
        return "main_sh"
    if c.startswith(("688", "689")):
        return "star"
    if c.startswith(("300", "301")):
        return "chinext"
    if c.startswith(("000", "001", "002", "003")):
        return "main_sz"
    if c.startswith(("8", "4", "920")):
        return "bse"
    return "unknown"


def is_st_name(name: str | None) -> bool:
    if not name:
        return False
    value = name.strip().upper()
    return value.startswith("ST") or value.startswith("*ST") or "退" in name


def limit_up_threshold(code: str, name: str | None = None) -> float:
    if is_st_name(name):
        return 0.05
    board = board_from_code(code)
    if board in {"chinext", "star"}:
        return 0.20
    if board == "bse":
        return 0.30
    return 0.10


def cap_style_from_market_cap(float_market_cap: float | None) -> str | None:
    if float_market_cap is None:
        return None
    if float_market_cap > 500e8:
        return "large"
    if float_market_cap > 100e8:
        return "mid"
    if float_market_cap > 30e8:
        return "small"
    if float_market_cap > 0:
        return "micro"
    return None


def normalize_stock_basic_record(record: dict) -> dict:
    code = str(record.get("code") or record.get("symbol") or "").strip()[-6:]
    name = str(record.get("name") or "").strip()
    market = str(record.get("market") or market_from_code(code)).upper()
    float_market_cap = _optional_float(record.get("float_market_cap") or record.get("circ_market_cap") or record.get("circMarketCap"))
    return {
        "code": code,
        "name": name,
        "market": market,
        "board": board_from_code(code),
        "is_st": is_st_name(name),
        "limit_up_threshold": limit_up_threshold(code, name),
        "is_active": bool(record.get("is_active", True)),
        "source": str(record.get("source") or "unknown"),
        "industry": _optional_text(record.get("industry")),
        "total_market_cap": _optional_float(record.get("total_market_cap") or record.get("totalMarketCap")),
        "float_market_cap": float_market_cap,
        "cap_style": record.get("cap_style") or record.get("capStyle") or cap_style_from_market_cap(float_market_cap),
        "sector_index_symbol": _optional_text(record.get("sector_index_symbol") or record.get("sectorIndexSymbol")),
        "sector_index_name": _optional_text(record.get("sector_index_name") or record.get("sectorIndexName")),
        "industry_query": _optional_text(record.get("industry_query") or record.get("industryQuery")),
    }


def _optional_float(value) -> float | None:
    if value in (None, "", "--"):
        return None
    try:
        return float(str(value).replace(",", "").strip())
    except (TypeError, ValueError):
        return None


def _optional_text(value) -> str | None:
    if value in (None, ""):
        return None
    text = str(value).strip()
    return text or None
