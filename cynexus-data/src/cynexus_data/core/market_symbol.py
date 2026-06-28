from __future__ import annotations


def normalize_market_symbol(symbol: str) -> str:
    value = str(symbol).strip().lower()
    if value.startswith(("sh", "sz", "bj")):
        return value
    code = value.zfill(6)
    if code.startswith(("5", "6", "9")):
        return f"sh{code}"
    if code.startswith(("4", "8")):
        return f"bj{code}"
    return f"sz{code}"


def split_market_symbol(symbol: str) -> tuple[str, str]:
    normalized = normalize_market_symbol(symbol)
    return normalized[:2].upper(), normalized[2:]


def eastmoney_secid(symbol: str) -> str:
    market, code = split_market_symbol(symbol)
    prefix = "1" if market == "SH" else "0"
    return f"{prefix}.{code}"
