from __future__ import annotations

import pandas as pd

from cynexus_data.sources.akshare.stock_basic import normalize_akshare_stock_basic


def test_normalize_akshare_stock_basic_records() -> None:
    df = pd.DataFrame([{"code": "1", "name": "平安银行"}, {"code": "688001", "name": "华兴源创"}])

    records = normalize_akshare_stock_basic(df)

    assert records[0]["code"] == "000001"
    assert records[0]["market"] == "SZ"
    assert records[1]["board"] == "star"
    assert records[1]["limit_up_threshold"] == 0.20
