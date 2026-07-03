from __future__ import annotations

from datetime import date

from cynexus_data.storage.postgres.tdx_industry_fund_flow import TdxIndustryFundFlowRepository


class FakeExecuteResult:
    def __init__(self, rows=None):
        self.rows = rows or []

    def mappings(self):
        return self

    def first(self):
        return self.rows[0] if self.rows else None

    def all(self):
        return self.rows


class FakeSession:
    def __init__(self, rows=None):
        self.rows = rows or []
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params):
        self.calls.append((str(statement), params))
        return FakeExecuteResult(self.rows)


class FakeClient:
    schema = "cynexus_appl_market"

    def __init__(self, rows=None):
        self.session_obj = FakeSession(rows)

    def session(self):
        return self.session_obj


def test_replace_snapshot_soft_deletes_and_upserts_rows() -> None:
    client = FakeClient()
    repo = TdxIndustryFundFlowRepository(client)

    result = repo.replace_snapshot(
        trade_date=date(2026, 7, 3),
        rows=[
            {
                "code6": "880471",
                "full_code": "sh880471",
                "tdx_name": "银行",
                "flow_sector_name": "全国性银行",
                "seed_code": "sh601398",
                "flow_trade_date": date(2026, 7, 3),
                "snapshot_status": "ok",
                "selection_mode": "exact",
                "name_match_type": "mismatch",
                "main_net": 12.5,
                "large_order_net": 10.0,
                "medium_order_net": 2.0,
                "small_order_net": 0.5,
                "row_count": 30,
                "extra": {},
            }
        ],
    )

    sqls = [call[0] for call in client.session_obj.calls]
    assert result["rows_written"] == 1
    assert "UPDATE cynexus_appl_market.tdx_industry_fund_flow_snapshot" in sqls[0]
    assert "INSERT INTO cynexus_appl_market.tdx_industry_fund_flow_batch" in sqls[2]
    assert "INSERT INTO cynexus_appl_market.tdx_industry_fund_flow_snapshot" in sqls[3]


def test_get_latest_snapshot_maps_row() -> None:
    client = FakeClient(
        rows=[
            {
                "trade_date": date(2026, 7, 3),
                "code6": "880471",
                "full_code": "sh880471",
                "tdx_name": "银行",
                "flow_sector_name": "全国性银行",
                "snapshot_status": "ok",
                "selection_mode": "exact",
                "name_match_type": "mismatch",
            }
        ]
    )

    row = TdxIndustryFundFlowRepository(client).get_latest_snapshot(code6="880471", max_trade_date=date(2026, 7, 3))

    assert row["code6"] == "880471"
    assert row["flow_sector_name"] == "全国性银行"
