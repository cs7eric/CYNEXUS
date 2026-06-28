from __future__ import annotations

from datetime import date

import pytest

from cynexus_data.storage.postgres.percentile import PercentileRepository


class FakeResult:
    def __init__(self, first_row=None, rows=None):
        self.first_row = first_row
        self.rows = rows or []

    def first(self):
        return self.first_row

    def all(self):
        return self.rows


class FakeSession:
    def __init__(self, result):
        self.result = result
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params):
        self.calls.append((str(statement), params))
        return self.result


class FakeClient:
    schema = "cynexus_data"

    def __init__(self, result):
        self.session_obj = FakeSession(result)

    def session(self):
        return self.session_obj


def test_percentile_score_queries_allowlisted_table_and_column() -> None:
    client = FakeClient(FakeResult(first_row=(72.345,)))
    repo = PercentileRepository(client)

    score = repo.percentile_score("turnover_activity_daily", "ratio", date(2026, 6, 26), 1.23)

    sql, params = client.session_obj.calls[0]
    assert score == 72.3
    assert "cynexus_data.msi_turnover_activity_daily" in sql
    assert "ratio" in sql
    assert params["current_value"] == 1.23


def test_percentile_score_rejects_unknown_sources() -> None:
    repo = PercentileRepository(FakeClient(FakeResult(first_row=(50,))))

    with pytest.raises(ValueError):
        repo.percentile_score("bad_table", "ratio", "2026-06-26", 1)

    with pytest.raises(ValueError):
        repo.percentile_score("turnover_activity_daily", "bad_column", "2026-06-26", 1)


def test_percentile_score_returns_fallback_on_empty_or_query_error() -> None:
    assert PercentileRepository(FakeClient(FakeResult(first_row=(None,)))).percentile_score("turnover_activity_daily", "ratio", "2026-06-26", 1) == 50.0

    class ErrorClient(FakeClient):
        def session(self):
            raise RuntimeError("db down")

    assert PercentileRepository(ErrorClient(FakeResult())).percentile_score("turnover_activity_daily", "ratio", "2026-06-26", 1, fallback=44.0) == 44.0


def test_enrich_history_scores_maps_trade_dates() -> None:
    rows = [(date(2026, 6, 25), 60.04), (date(2026, 6, 26), None)]
    repo = PercentileRepository(FakeClient(FakeResult(rows=rows)))
    items = [{"tradeDate": "2026-06-25"}, {"tradeDate": "2026-06-26"}, {"tradeDate": "2026-06-27"}]

    enriched = repo.enrich_history_scores(items, "style_risk_appetite_daily", "spread", "2026-06-26")

    assert [item["score"] for item in enriched] == [60.0, 50.0, 50.0]
