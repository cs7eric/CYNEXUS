from decimal import Decimal

from cynexus_data.storage.postgres.market_etf import _calculate_share_values, _json_payload


def test_etf_raw_payload_replaces_non_finite_numbers() -> None:
    payload = _json_payload({"iopv": float("nan"), "levels": [float("inf"), -1.0]})

    assert payload == '{"iopv": null, "levels": [null, -1.0]}'


def test_etf_share_calculation_accepts_float_and_decimal_values() -> None:
    delta, proxy = _calculate_share_values(120.5, Decimal("100.25"), 2.0)

    assert delta == Decimal("20.25")
    assert proxy == Decimal("40.50")
