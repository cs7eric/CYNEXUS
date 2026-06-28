from __future__ import annotations

from cynexus_data.sources.eastmoney.parser import parse_kline_rows
from cynexus_data.sources.sina.parser import parse_sina_minute_rows
from cynexus_data.sources.ths.client import ThsClient
from cynexus_data.sources.ths.parser import extract_constituents_total_pages, extract_total_pages, parse_industry_constituents_html, parse_industry_fund_flow_html
from cynexus_data.sources.tencent.parser import parse_tencent_kline_rows


def test_parse_eastmoney_kline_rows() -> None:
    rows = parse_kline_rows(["2026-06-26,10,11,12,9,100,1000"])
    assert rows[0]["trade_date"].isoformat() == "2026-06-26"
    assert rows[0]["close"] == 11.0


def test_parse_tencent_kline_rows() -> None:
    rows = parse_tencent_kline_rows([["2026-06-26", "10", "11", "12", "9", "100"]])
    assert rows[0]["source"] == "tencent"
    assert rows[0]["volume"] == 100.0


def test_parse_sina_minute_rows() -> None:
    rows = parse_sina_minute_rows(
        [
            {"day": "bad", "open": "x"},
            {"day": "2026-06-26 09:31:00", "open": "10", "high": "11", "low": "9", "close": "10.5", "volume": "20", "amount": "200"},
            {"day": "2026-06-26 09:32", "open": "10.5", "high": "12", "low": "10", "close": "11", "volume": "40", "amount": "500"},
        ]
    )
    assert rows[0]["trade_time"].hour == 9
    assert rows[0]["close"] == 10.5
    assert rows[0]["volume_ratio"] is None
    assert rows[1]["volume_ratio"] == 2.0


def test_parse_ths_industry_fund_flow_html() -> None:
    html = """
    <html><body><span class="page_info">1 / 2</span>
    <table>
      <thead><tr><th>序号</th><th>行业</th><th>行业指数</th><th>涨跌幅</th><th>流入资金(亿)</th><th>流出资金(亿)</th><th>净额(亿)</th><th>公司家数</th><th>领涨股</th><th>涨跌幅</th><th>当前价(元)</th></tr></thead>
      <tbody>
        <tr><td>1</td><td>软件服务</td><td>1000</td><td>1.2%</td><td>10</td><td>4</td><td>6</td><td>120</td><td>样例A</td><td>5%</td><td>10.5</td></tr>
        <tr><td>2</td><td>银行</td><td>900</td><td>-0.2%</td><td>2</td><td>5</td><td>-3</td><td>42</td><td>样例B</td><td>-1%</td><td>8.1</td></tr>
      </tbody>
    </table></body></html>
    """
    rows = parse_industry_fund_flow_html(html)
    assert extract_total_pages(html) == 2
    assert rows[0]["行业"] == "软件服务"
    assert rows[0]["行业指数涨跌幅"] == "1.2%"
    assert "行业指数" not in rows[0]
    assert rows[0]["领涨股涨跌幅"] == "5%"


def test_parse_ths_industry_constituents_html_pads_codes() -> None:
    html = """
    <html><body><span class="page_info">1 / 3</span>
    <table>
      <thead><tr><th>序号</th><th>代码</th><th>名称</th><th>现价</th><th>涨跌幅(%)</th><th>成交额</th></tr></thead>
      <tbody>
        <tr><td>1</td><td>3030</td><td>祖名股份</td><td>23.42</td><td>10.01</td><td>0.97亿</td></tr>
        <tr><td>2</td><td>600597</td><td>光明乳业</td><td>8.10</td><td>-1.2</td><td>1.20亿</td></tr>
      </tbody>
    </table></body></html>
    """
    rows = parse_industry_constituents_html(html)

    assert extract_constituents_total_pages(html) == 3
    assert rows[0]["代码"] == "003030"
    assert rows[1]["代码"] == "600597"


def test_ths_constituents_client_uses_q_host_for_live_pages() -> None:
    class Response:
        content = """
        <html><body><span class="page_info">1 / 1</span>
        <table><tr><th>序号</th><th>代码</th><th>名称</th></tr><tr><td>1</td><td>3030</td><td>祖名股份</td></tr></table>
        </body></html>
        """.encode("gbk")

        def raise_for_status(self):
            return None

    class Session:
        urls = []

        def get(self, url, headers=None, timeout=None):
            self.urls.append(url)
            return Response()

    session = Session()
    client = ThsClient(session=session, base_url="https://data.10jqka.com.cn")

    payload = client.fetch_industry_constituents_page("881103", hexin_v="token")

    assert session.urls == ["https://q.10jqka.com.cn/thshy/detail/code/881103/page/1/"]
    assert payload["rows"][0]["代码"] == "003030"
