from datetime import date

from cynexus_data.pipelines.market.sync_etf import _infer_trade_date
from cynexus_data.sources.akshare.etf import fetch_etf_classification


def test_infer_trade_date_prefers_source_data_date() -> None:
    assert _infer_trade_date([{"数据日期": "2026-07-17 00:00:00"}, {"数据日期": "2026-07-16"}]) == date(2026, 7, 17)


class _ClassificationClient:
    def fund_info_index_em(self, *, symbol: str, indicator: str):
        assert indicator == "全部"
        if symbol == "大盘指数":
            return _Frame([{"基金代码": "510300"}])
        if symbol == "行业主题":
            return _Frame([{"基金代码": "510300"}, {"基金代码": "512480"}])
        return _Frame([])


class _Frame:
    def __init__(self, rows):
        self._rows = rows
        self.empty = not rows

    def to_dict(self, orient):
        assert orient == "records"
        return self._rows


def test_fetch_etf_classification_prefers_more_specific_industry_bucket() -> None:
    mapping, errors = fetch_etf_classification(_ClassificationClient())

    assert errors == []
    assert mapping["510300"]["category"] == "sector"
    assert mapping["512480"]["category"] == "sector"
