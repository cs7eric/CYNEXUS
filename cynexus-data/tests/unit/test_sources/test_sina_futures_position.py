from datetime import date

from cynexus_data.sources.sina.futures_position import fetch_dce_position_rows


class _Response:
    def __init__(self, content: bytes):
        self.content = content

    def raise_for_status(self):
        return None


class _Session:
    def __init__(self):
        self.calls = []

    def get(self, _url, *, params, headers, timeout):
        self.calls.append(params)
        if len(self.calls) == 1:
            html = "<select><option value='M2609'>M2609</option><option value='RB2609'>RB2609</option></select>"
            return _Response(html.encode("gbk"))
        table = """
        <table><tr><th>排名</th><th>会员</th><th>数值</th><th>变化</th></tr>
        <tr><td>1</td><td>member</td><td>100</td><td>-2</td></tr>
        <tr><td>合计</td><td></td><td>100</td><td></td></tr></table>
        """
        return _Response(("<table><tr><td>nav</td></tr></table>" * 2 + table * 3).encode("gbk"))


def test_fetch_dce_position_rows_reads_contract_ranking_tables() -> None:
    session = _Session()

    rows = fetch_dce_position_rows(session, date(2026, 7, 30), varieties=["M"], base_url="https://example.test")

    assert len(rows) == 1
    assert rows[0] == {
        "symbol": "M2609",
        "variety": "M",
        "rank": 1,
        "vol_party_name": "member",
        "vol": "100",
        "vol_chg": "-2.0",
        "long_party_name": "member",
        "long_open_interest": "100",
        "long_open_interest_chg": "-2.0",
        "short_party_name": "member",
        "short_open_interest": "100",
        "short_open_interest_chg": "-2.0",
    }
