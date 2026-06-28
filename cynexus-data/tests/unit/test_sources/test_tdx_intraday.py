from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime

from cynexus_data.sources.tdx.intraday import (
    fetch_stock_auction,
    fetch_stock_history_timeshare,
    normalize_auction_points,
    normalize_timeshare_rows,
)


@dataclass
class Point:
    time_label: str
    price: float
    matched_volume: int


class Response:
    def __init__(self, points):
        self.points = tuple(points)


class FakeClient:
    def get_quote(self, code):
        return [{"last_price": 10.2}]

    def get_call_auction(self, code):
        return Response([Point("9:25:00", 10.1, 100), Point("14:57:00", 10.4, 200)])

    def get_auction_0925(self, symbol, trade_date):
        return {"has_auction_0925": True, "price": 10.1, "volume": 1000, "amount": 10100}

    def get_history_minute(self, *args, **kwargs):
        return Response(
            [
                {"time": "2026-06-26 09:30:00", "price": 10.2, "avg_price": 10.15, "volume": 100, "amount": 1020},
                {"time": datetime(2026, 6, 26, 9, 31), "price": 10.3, "volume": 200, "turnover": 2060},
            ]
        )


def test_normalize_auction_points_splits_opening_closing_and_0925() -> None:
    rows = normalize_auction_points(
        symbol="600000",
        tdx_code="sh600000",
        trade_date=date(2026, 6, 26),
        points=[{"time_label": "9:25:00", "price": 10.1}, {"time_label": "14:57", "price": 10.4}],
        auction_0925={"has_auction_0925": True, "price": 10.1, "volume": 1000},
    )

    assert [row["phase"] for row in rows] == ["opening", "closing", "auction_0925"]
    assert rows[0]["time_label"] == "09:25:00"
    assert rows[0]["trade_time"] == datetime(2026, 6, 26, 9, 25)


def test_normalize_timeshare_rows_emits_volume_ratio() -> None:
    rows = normalize_timeshare_rows(
        symbol="000001",
        tdx_code="sz000001",
        rows=[
            {"time": "2026-06-26 09:30", "price": 10.0, "volume": 100},
            {"time": "2026-06-26 09:31:00", "price": 10.1, "volume": 200},
        ],
    )

    assert len(rows) == 2
    assert rows[0]["volume_ratio"] is None
    assert rows[1]["volume_ratio"] == 2.0
    assert rows[0]["time_label"] == "09:30"


def test_fetch_stock_auction_uses_eltdx_client_variants() -> None:
    payload = fetch_stock_auction(FakeClient(), "600000", "2026-06-26")

    assert payload["tdx_code"] == "sh600000"
    assert len(payload["points"]) == 3


def test_fetch_stock_history_timeshare_extracts_response_points() -> None:
    rows = fetch_stock_history_timeshare(FakeClient(), "000001", "2026-06-26")

    assert len(rows) == 2
    assert rows[0]["symbol"] == "000001"
    assert rows[1]["volume_ratio"] == 2.0
