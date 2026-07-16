from cynexus_data.sources.market_information.client import _clean_text, _decode_html, _is_candidate_title
from cynexus_data.sources.eastmoney.a_stock import global_news


class Response:
    content = "<html><a>政策发布</a></html>".encode("utf-8")
    apparent_encoding = "utf-8"


def test_official_source_text_is_cleaned_and_utf8_is_preferred():
    assert _decode_html(Response()).startswith("<html>")
    assert _clean_text("  国务院\u00a0发布通知 ") == "国务院 发布通知"


def test_official_source_title_filter_excludes_navigation_but_keeps_headlines():
    assert _is_candidate_title("国务院关于扩大消费规划的批复")
    assert not _is_candidate_title("首页")


class JsonResponse:
    def __init__(self, payload):
        self.payload = payload

    def json(self):
        return self.payload


class CursorClient:
    def __init__(self):
        self.calls = []

    def get(self, _url, *, params, headers):
        self.calls.append((params, headers))
        start = 0 if not params["sortEnd"] else 50
        size = int(params["pageSize"])
        rows = [
            {
                "code": f"news-{index}",
                "title": f"headline {index}",
                "summary": f"summary {index}",
                "showTime": "2026-07-13 12:00:00",
                "realSort": str(1000 - index),
                "stockList": [],
            }
            for index in range(start, start + size)
        ]
        return JsonResponse({"data": {"sortEnd": f"cursor-{start + size}", "fastNewsList": rows}})


def test_eastmoney_global_news_uses_sort_end_cursor_for_history():
    client = CursorClient()

    rows = global_news(client, page_size=55)

    assert len(rows) == 55
    assert [call[0]["pageSize"] for call in client.calls] == ["50", "5"]
    assert client.calls[1][0]["sortEnd"] == "cursor-50"
    assert rows[-1]["id"] == "news-54"
