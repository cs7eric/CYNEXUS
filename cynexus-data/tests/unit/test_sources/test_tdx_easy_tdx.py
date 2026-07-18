from datetime import date, datetime
from types import ModuleType
import sys

import pandas as pd

from cynexus_data.sources.tdx import easy_tdx


def test_kline_records_normalize_easy_tdx_columns() -> None:
    rows = easy_tdx._kline_records(
        pd.DataFrame(
            [
                {
                    "datetime": datetime(2026, 7, 17, 9, 31),
                    "open": 10,
                    "high": 11,
                    "low": 9,
                    "close": 10.5,
                    "vol": 100,
                    "amount": 1000,
                }
            ]
        ),
        source="easy_tdx.mac",
    )

    assert rows == [
        {
            "trade_time": datetime(2026, 7, 17, 9, 31),
            "trade_date": date(2026, 7, 17),
            "open": 10.0,
            "high": 11.0,
            "low": 9.0,
            "close": 10.5,
            "volume": 100.0,
            "amount": 1000.0,
            "source": "easy_tdx.mac",
        }
    ]


def test_flow_records_maps_main_net_to_existing_stock_schema() -> None:
    rows = easy_tdx._flow_records(
        pd.DataFrame(
            [
                {
                    "year": 2026,
                    "month": 7,
                    "day": 17,
                    "super_in": 100000,
                    "super_out": 20000,
                    "large_in": 50000,
                    "large_out": 10000,
                    "medium_in": 1000,
                    "medium_out": 2000,
                    "small_in": 500,
                    "small_out": 800,
                }
            ]
        ),
        code="600519",
    )

    assert rows[0]["trade_date"] == date(2026, 7, 17)
    assert rows[0]["main_net"] == 120000.0
    assert rows[0]["active_net_amount_wan"] == 12.0
    assert rows[0]["active_buy_ratio"] == 150000 / 180000
    assert rows[0]["active_sell_ratio"] == 30000 / 180000
    assert rows[0]["buy_big_ratio"] == 50000 / 60000
    assert rows[0]["sell_big_ratio"] == 10000 / 60000
    assert rows[0]["source"] == "easy_tdx.tdx"


def test_easy_tdx_period_and_adjust_validation() -> None:
    class Period:
        MIN_1 = "min1"
        MIN_5 = "min5"
        MIN_15 = "min15"
        MIN_30 = "min30"
        MIN_60 = "min60"
        DAILY = "day"
        WEEKLY = "week"
        MONTHLY = "month"

    class Adjust:
        NONE = "none"
        QFQ = "qfq"
        HFQ = "hfq"

    assert easy_tdx._period("1m", Period) == "min1"
    assert easy_tdx._period("day", Period) == "day"
    assert easy_tdx._adjust("qfq", Adjust) == "qfq"

    try:
        easy_tdx._adjust("minute-qfq", Adjust)
    except ValueError as exc:
        assert "none, qfq, or hfq" in str(exc)
    else:
        raise AssertionError("invalid adjustment must be rejected")


def test_history_fund_flow_switches_hosts_on_empty_data(monkeypatch) -> None:
    saved_hosts: list[str] = []
    connected_hosts: list[str] = []

    class Market:
        SZ = 0
        SH = 1
        BJ = 2

    class KlineCategory:
        DAY = "day"

    class GetSecurityBarsCmd:
        def __init__(self, market, code, category, start, count) -> None:
            self.code = code

    class FakeAsyncTdxClient:
        def __init__(self, host, **kwargs) -> None:
            self.host = host

        async def __aenter__(self):
            connected_hosts.append(self.host)
            return self

        async def __aexit__(self, exc_type, exc, tb) -> None:
            return None

        async def _execute(self, cmd):
            return [object()] if self.host == "good-host" else []

        async def get_history_fund_flow(self, market, code, start, count):
            return pd.DataFrame(
                [
                    {
                        "date": "2026-07-17",
                        "super_in": 100,
                        "super_out": 10,
                        "large_in": 20,
                        "large_out": 5,
                        "medium_in": 0,
                        "medium_out": 0,
                        "small_in": 0,
                        "small_out": 0,
                    }
                ]
            )

    easy_module = ModuleType("easy_tdx")
    easy_module.AsyncTdxClient = FakeAsyncTdxClient
    easy_module.Market = Market
    config_module = ModuleType("easy_tdx.config")
    config_module.get_best_host = lambda: "bad-host"
    config_module.get_known_hosts = lambda: ["bad-host", "good-host"]
    config_module.save_best_host = saved_hosts.append
    security_bars_module = ModuleType("easy_tdx.commands.security_bars")
    security_bars_module.GetSecurityBarsCmd = GetSecurityBarsCmd
    enums_module = ModuleType("easy_tdx.models.enums")
    enums_module.KlineCategory = KlineCategory

    monkeypatch.setitem(sys.modules, "easy_tdx", easy_module)
    monkeypatch.setitem(sys.modules, "easy_tdx.config", config_module)
    monkeypatch.setitem(sys.modules, "easy_tdx.commands.security_bars", security_bars_module)
    monkeypatch.setitem(sys.modules, "easy_tdx.models.enums", enums_module)
    monkeypatch.setattr(easy_tdx, "_PREFERRED_STANDARD_TDX_HOSTS", ())

    rows = easy_tdx.fetch_history_stock_fund_flow("sh600519", count=1)

    assert connected_hosts == ["bad-host", "good-host"]
    assert saved_hosts == ["good-host"]
    assert rows[0]["trade_date"] == date(2026, 7, 17)
    assert rows[0]["main_net"] == 105.0
