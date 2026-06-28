from __future__ import annotations

from cynexus_data.core.market_symbol import eastmoney_secid, normalize_market_symbol
from cynexus_data.sources.provider_fallback import fetch_with_fallback, provider_plan


def test_fetch_with_fallback_records_attempts() -> None:
    result = fetch_with_fallback(
        ["bad", "empty", "ok"],
        {
            "bad": lambda: (_ for _ in ()).throw(RuntimeError("boom")),
            "empty": lambda: [],
            "ok": lambda: [{"close": 1}],
        },
    )
    assert result.source == "ok"
    assert result.items == [{"close": 1}]
    assert [attempt.provider for attempt in result.attempts] == ["bad", "empty", "ok"]
    assert result.attempts[0].ok is False


def test_provider_plan_and_symbol_helpers() -> None:
    assert provider_plan("kline_daily", requested="eastmoney,tencent") == ["eastmoney", "tencent"]
    assert normalize_market_symbol("600000") == "sh600000"
    assert normalize_market_symbol("000001") == "sz000001"
    assert eastmoney_secid("600000") == "1.600000"
