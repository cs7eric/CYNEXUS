from cynexus_data.domain.market.market_fund_flow import normalize_market_fund_flow_row


def test_normalize_market_fund_flow_row_converts_yuan_to_yi() -> None:
    row = normalize_market_fund_flow_row(
        {
            "trade_date": "2026-07-16",
            "main_net_inflow": -83885834240,
            "super_large_net_inflow": -42946482176,
            "large_net_inflow": -40939352064,
            "medium_net_inflow": 16797376512,
            "small_net_inflow": 67088457728,
            "main_net_inflow_ratio": -3.49,
            "super_large_net_inflow_ratio": -1.79,
            "large_net_inflow_ratio": -1.70,
            "medium_net_inflow_ratio": 0.70,
            "small_net_inflow_ratio": 2.79,
        }
    )

    assert row["trade_date"].isoformat() == "2026-07-16"
    assert row["main_net_inflow"] == -838.8583
    assert row["main_net_inflow_ratio"] == -3.49
