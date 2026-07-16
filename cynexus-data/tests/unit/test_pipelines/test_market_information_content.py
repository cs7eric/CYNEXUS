from cynexus_data.pipelines.a_stock.market_information_content import identities_from_batches


def test_collects_only_market_information_identities() -> None:
    values = identities_from_batches({
        "a_stock_news": [
            {"source": "cls.telegraph", "news_id": "n1", "title": "宏观消息", "summary": "正文摘要", "url": "https://www.cls.cn/detail/1"},
            {"source": "eastmoney.search", "news_id": "n2", "title": "个股消息", "summary": "", "url": None},
        ],
        "a_stock_announcement": [
            {"source": "cninfo.regulation", "announcement_id": "a1", "title": "监管公告", "url": "https://static.cninfo.com.cn/a.pdf"},
        ],
    })

    assert [(item.source, item.source_record_id) for item in values] == [("cls.telegraph", "n1"), ("cninfo.regulation", "a1")]
