from cynexus_data.storage.postgres.market_etf import _json_payload


def test_etf_raw_payload_replaces_non_finite_numbers() -> None:
    payload = _json_payload({"iopv": float("nan"), "levels": [float("inf"), -1.0]})

    assert payload == '{"iopv": null, "levels": [null, -1.0]}'
