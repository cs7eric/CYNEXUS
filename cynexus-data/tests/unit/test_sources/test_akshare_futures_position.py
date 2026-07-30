from datetime import date

from cynexus_data.sources.akshare.futures_position import fetch_position_rows


class _Frame:
    def to_dict(self, orient):
        assert orient == "records"
        return [{"symbol": "IF2609", "variety": "IF", "rank": 1, "vol": 1}]


class _Ak:
    def get_cffex_rank_table(self, trade_date, varieties):
        assert trade_date == "20260730"
        assert varieties == ["IF"]
        return {"IF2609": _Frame()}


def test_fetch_position_rows_keeps_exchange_provider_boundary() -> None:
    result = fetch_position_rows(_Ak(), date(2026, 7, 30), exchanges=["CFFEX"], varieties=["IF"])

    assert result == {"CFFEX": [{"symbol": "IF2609", "variety": "IF", "rank": 1, "vol": 1}]}
