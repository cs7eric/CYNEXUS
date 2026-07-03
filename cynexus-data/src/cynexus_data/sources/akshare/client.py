from __future__ import annotations

from typing import Any


class AkshareClient:
    def require_module(self):
        import akshare as ak

        return ak

    def fetch_market_overview(self) -> dict[str, Any]:
        ak = self.require_module()
        out: dict[str, Any] = {}
        spot = ak.stock_zh_a_spot_em()
        if spot is not None and not spot.empty:
            out.update(_summarize_spot(spot))
        flow = ak.stock_market_fund_flow()
        if flow is not None and not flow.empty:
            out.update(_summarize_market_fund_flow(flow))
        if not out:
            raise RuntimeError("akshare returned empty market overview datasets")
        return out

    def fetch_market_pulse_industry(self) -> list[dict[str, Any]]:
        ak = self.require_module()
        frame = ak.stock_fund_flow_industry()
        if frame is None or frame.empty:
            return []
        out: list[dict[str, Any]] = []
        for _, row in frame.iterrows():
            name = _clean_text(row.get("行业"))
            if not name:
                continue
            out.append({
                "sector_name": name,
                "sector_index": _clean_text(row.get("行业指数")),
                "change_pct": _single_numeric(row.get("行业-涨跌幅")),
                "inflow": _single_numeric(row.get("流入资金")),
                "outflow": _single_numeric(row.get("流出资金")),
                "main_net": _single_numeric(row.get("净额")),
                "stock_count": _single_int(row.get("公司家数")),
                "leading_stock": _clean_text(row.get("领涨股")),
                "leading_change_pct": _single_numeric(row.get("领涨股-涨跌幅")),
                "leading_price": _single_numeric(row.get("当前价")),
                "extra": {
                    "source": "akshare.stock_fund_flow_industry",
                },
            })
        return out


def _summarize_spot(frame) -> dict[str, Any]:
    out: dict[str, Any] = {}
    change_col = _find_column(frame, "涨跌幅")
    amount_col = _find_column(frame, "成交额")
    volume_col = _find_column(frame, "成交量", exclude="额")
    if amount_col:
        out["totalAmount"] = round(float(_numeric(frame[amount_col]).sum()) / 1e8, 2)
    if volume_col:
        out["totalVolume"] = round(float(_numeric(frame[volume_col]).sum()) / 1e4, 2)
    if change_col:
        changes = _numeric(frame[change_col])
        out["risingCount"] = int((changes >= 0.5).sum())
        out["fallingCount"] = int((changes <= -0.5).sum())
        out["flatCount"] = int(((changes > -0.5) & (changes < 0.5)).sum())
        out["limitUpCount"] = int((changes >= 9.5).sum())
        out["limitDownCount"] = int((changes <= -9.5).sum())
    out["stockCount"] = int(len(frame))
    return out


def _summarize_market_fund_flow(frame) -> dict[str, Any]:
    latest = frame.tail(1).iloc[0]
    super_large = _money_yi(frame, latest, "超大单净流入-净额")
    large = _money_yi(frame, latest, "大单净流入-净额")
    medium = _money_yi(frame, latest, "中单净流入-净额")
    small = _money_yi(frame, latest, "小单净流入-净额")
    out = {
        "superLargeNetInflow": super_large,
        "largeNetInflow": large,
        "mediumNetInflow": medium,
        "smallNetInflow": small,
        "mainNetInflowRatio": _ratio(frame, latest, "主力净流入-净占比"),
        "superLargeNetInflowRatio": _ratio(frame, latest, "超大单净流入-净占比"),
        "largeNetInflowRatio": _ratio(frame, latest, "大单净流入-净占比"),
        "mediumNetInflowRatio": _ratio(frame, latest, "中单净流入-净占比"),
        "smallNetInflowRatio": _ratio(frame, latest, "小单净流入-净占比"),
    }
    values = [value for value in (super_large, large) if value is not None]
    out["mainNetInflow"] = round(sum(values), 2) if values else None
    return out


def _find_column(frame, include: str, *, exclude: str | None = None):
    for column in frame.columns:
        text = str(column)
        if include in text and (exclude is None or exclude not in text):
            return column
    return None


def _numeric(series):
    import pandas as pd

    return pd.to_numeric(series, errors="coerce").fillna(0)


def _money_yi(frame, row, suffix: str) -> float | None:
    column = _find_suffix_column(frame, suffix, exclude="占比")
    if column is None:
        return None
    value = _single_numeric(row[column])
    return round(value / 1e8, 2) if value is not None else None


def _ratio(frame, row, suffix: str) -> float | None:
    column = _find_suffix_column(frame, suffix)
    if column is None:
        return None
    value = _single_numeric(row[column])
    return round(value, 2) if value is not None else None


def _find_suffix_column(frame, suffix: str, *, exclude: str | None = None):
    for column in frame.columns:
        text = str(column)
        if text == suffix and (exclude is None or exclude not in text):
            return column
    for column in frame.columns:
        text = str(column)
        if text.endswith(suffix) and (exclude is None or exclude not in text):
            if suffix.startswith("大单") and text.startswith("超大单"):
                continue
            return column
    return None


def _single_numeric(value: Any) -> float | None:
    import pandas as pd

    parsed = pd.to_numeric(pd.Series([value]), errors="coerce").iloc[0]
    if pd.isna(parsed):
        return None
    return float(parsed)


def _single_int(value: Any) -> int | None:
    parsed = _single_numeric(value)
    return int(parsed) if parsed is not None else None


def _clean_text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None
