from cynexus_data.domain.market.etf import classify_etf, normalize_spot_row


def test_classify_etf_keeps_broad_and_sector_scopes() -> None:
    assert classify_etf("沪深300ETF") == ("broad", "沪深300")
    assert classify_etf("半导体ETF") == ("sector", "半导体")
    assert classify_etf("某债券产品") == ("other", None)
    assert classify_etf("中药ETF") == ("sector", "中药")
    assert classify_etf("上证180ETF") == ("broad", "上证180")
    assert classify_etf("纳斯达克ETF") == ("other", None)
    assert classify_etf("未知产品") == ("other", None)


def test_normalize_spot_row_maps_iopv_flow_and_market_fields() -> None:
    row = normalize_spot_row(
        {
            "代码": "510300",
            "名称": "沪深300ETF",
            "最新价": "4.123",
            "涨跌幅": "1.25",
            "成交额": 123456789,
            "IOPV实时估值": 4.120,
            "基金折价率": 0.07,
            "最新份额": 987654321,
            "主力净流入-净额": 1200000,
            "换手率": 2.4,
        },
        captured_at="2026-07-18T08:00:00+00:00",
    )

    assert row["code"] == "510300"
    assert row["exchange"] == "SSE"
    assert row["category"] == "broad"
    assert row["price"] == 4.123
    assert row["iopv"] == 4.12
    assert row["main_net_inflow"] == 1200000.0
    assert row["classification_source"] == "fallback.name_keyword"
    assert row["raw_payload"]["名称"] == "沪深300ETF"


def test_normalize_spot_row_prefers_code_keyed_authoritative_classification() -> None:
    row = normalize_spot_row(
        {"代码": "510300", "名称": "沪深300ETF"},
        captured_at="2026-07-18T08:00:00+00:00",
        classification_map={
            "510300": {
                "category": "sector",
                "source": "eastmoney.fund_index_classification",
            }
        },
    )

    assert row["category"] == "sector"
    assert row["classification_source"] == "eastmoney.fund_index_classification"
