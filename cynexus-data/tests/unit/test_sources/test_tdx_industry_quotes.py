from __future__ import annotations

from cynexus_data.sources.tdx.industry_quotes import build_tdx_industry_market_payload, fetch_tdx_industry_snapshots, resolve_tdx_industry_full_codes


class FakeClient:
    def get_index_codes_all(self):
        return ["sh880471", "sh880493", "sz000001"]

    def get_quote(self, codes):
        return [
            {"full_code": code, "last_price": 10 + index, "pre_close_price": 9, "change": 1, "change_pct": 2 - index, "amount": 1000 + index}
            for index, code in enumerate(codes)
        ]


def test_resolve_tdx_industry_full_codes_uses_client_index_list() -> None:
    resolved = resolve_tdx_industry_full_codes(FakeClient(), mapping={"880471": "银行", "880493": "软件服务", "880999": "不存在"})

    assert resolved == {
        "880471": {"name": "银行", "full_code": "sh880471", "exchange": "sh"},
        "880493": {"name": "软件服务", "full_code": "sh880493", "exchange": "sh"},
    }


def test_fetch_tdx_industry_snapshots_sorts_by_change_pct() -> None:
    rows = fetch_tdx_industry_snapshots(FakeClient(), mapping={"880471": "银行", "880493": "软件服务"}, batch_size=1)

    assert [row["code6"] for row in rows] == ["880471", "880493"]
    assert rows[0]["full_code"] == "sh880471"
    assert rows[0]["change_pct"] == 2.0


def test_build_tdx_industry_market_payload_matches_heatmap_shape() -> None:
    payload = build_tdx_industry_market_payload(
        [{"code6": "880471", "name": "银行", "full_code": "sh880471", "last_price": 10, "change_pct": 1.2, "amount": 100}],
        fetched_at="2026-06-26T15:00:00",
    )

    assert payload["kind"] == "industries"
    assert payload["count"] == 1
    assert payload["items"][0]["sectorCode"] == "sh880471"
