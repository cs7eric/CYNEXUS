from datetime import date
from types import SimpleNamespace

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.futures import sync_position


class _Repository:
    def __init__(self, _client):
        self.batches = []
        self.replacements = []

    def begin_batch(self, **kwargs):
        self.batches.append(kwargs)
        return len(self.batches)

    def finish_batch(self, batch_id, **kwargs):
        self.batches[batch_id - 1]["batch_id"] = batch_id
        self.batches[-1]["finished"] = kwargs

    def replace_selected_snapshot(self, **kwargs):
        self.replacements.append(kwargs)
        return len(kwargs["rows"])

    def upsert_contract_catalog(self, rows, **kwargs):
        return len(rows)

    def latest_selected_trade_date(self):
        return date(2026, 7, 30)


class _Akshare:
    def fetch_latest_futures_position_trade_date(self, *, end_date, exchanges, varieties, lookback_days):
        assert end_date == date.today()
        assert exchanges == ["CFFEX", "DCE"]
        assert varieties == []
        assert lookback_days == 10
        return date(2026, 7, 30)

    def fetch_futures_position_rows(self, trade_date, *, exchanges, varieties):
        assert trade_date == date(2026, 7, 30)
        assert exchanges == ["CFFEX", "DCE"]
        assert varieties == []
        return {
            "CFFEX": [{
                "symbol": "IF2609", "variety": "IF", "rank": 1,
                "vol_party_name": "member", "vol": 100,
                "long_party_name": "long", "long_open_interest": 90,
                "short_party_name": "short", "short_open_interest": 110,
            }],
            "DCE": [],
        }


class _Sina:
    def fetch_latest_futures_position_trade_date(self, *, end_date, varieties, lookback_days):
        assert end_date == date.today()
        assert varieties == []
        assert lookback_days == 10
        return None

    def fetch_dce_position_rows(self, trade_date, *, varieties):
        assert trade_date == date(2026, 7, 30)
        assert varieties == []
        return []


class _GapAkshare(_Akshare):
    def fetch_futures_position_rows(self, trade_date, *, exchanges, varieties):
        assert trade_date in {date(2026, 7, 29), date(2026, 7, 30)}
        return {
            "CFFEX": [{
                "symbol": "IF2609", "variety": "IF", "rank": 1,
                "vol_party_name": "member", "vol": 100,
                "long_party_name": "long", "long_open_interest": 90,
                "short_party_name": "short", "short_open_interest": 110,
            }],
            "DCE": [],
        }


class _GapSina(_Sina):
    def fetch_dce_position_rows(self, trade_date, *, varieties):
        return []


class _NoLatestAkshare(_Akshare):
    def fetch_latest_futures_position_trade_date(self, *, end_date, exchanges, varieties, lookback_days):
        return None


def test_sync_position_replaces_successful_exchange_and_preserves_empty_exchange(monkeypatch) -> None:
    repository = _Repository(object())
    monkeypatch.setattr(sync_position, "FuturesPositionRepository", lambda client: repository)
    hooks = SimpleNamespace(
        postgres=SimpleNamespace(get_client=lambda: object()),
        akshare=SimpleNamespace(get_client=lambda: _Akshare()),
        sina_futures=SimpleNamespace(get_client=lambda: _Sina()),
    )
    context = SimpleNamespace(hooks=hooks)

    result = sync_position.run(context, JobRequest(
        job_key="sync_futures_position",
        params={"trade_date": "20260730", "exchanges": ["CFFEX", "DCE"]},
    ))

    assert result["status"] == "PARTIAL"
    assert result["success"] is True
    assert [item["exchange_code"] for item in repository.batches] == ["CFFEX", "DCE"]
    assert len(repository.replacements) == 1
    assert repository.batches[1]["finished"]["status"] == "EMPTY"


def test_sync_position_without_trade_date_uses_latest_source_date(monkeypatch) -> None:
    repository = _Repository(object())
    monkeypatch.setattr(sync_position, "FuturesPositionRepository", lambda client: repository)
    hooks = SimpleNamespace(
        postgres=SimpleNamespace(get_client=lambda: object()),
        akshare=SimpleNamespace(get_client=lambda: _Akshare()),
        sina_futures=SimpleNamespace(get_client=lambda: _Sina()),
    )

    result = sync_position.run(
        SimpleNamespace(hooks=hooks),
        JobRequest(job_key="sync_futures_position", params={"exchanges": ["CFFEX", "DCE"]}),
    )

    assert result["trade_date"] == "2026-07-30"
    assert result["latest_source_data"] is True
    assert [item["trade_date"] for item in repository.batches] == [date(2026, 7, 30), date(2026, 7, 30)]


def test_sync_position_without_latest_source_data_does_not_write_today(monkeypatch) -> None:
    repository = _Repository(object())
    monkeypatch.setattr(sync_position, "FuturesPositionRepository", lambda client: repository)
    hooks = SimpleNamespace(
        postgres=SimpleNamespace(get_client=lambda: object()),
        akshare=SimpleNamespace(get_client=lambda: _NoLatestAkshare()),
        sina_futures=SimpleNamespace(get_client=lambda: _Sina()),
    )

    result = sync_position.run(
        SimpleNamespace(hooks=hooks),
        JobRequest(job_key="sync_futures_position", params={"exchanges": ["CFFEX", "DCE"]}),
    )

    assert result["trade_date"] is None
    assert result["status"] == "EMPTY"
    assert result["success"] is False
    assert repository.batches == []


def test_sync_position_latest_source_fills_a_short_persisted_date_gap(monkeypatch) -> None:
    repository = _Repository(object())
    repository.latest_selected_trade_date = lambda: date(2026, 7, 28)
    monkeypatch.setattr(sync_position, "FuturesPositionRepository", lambda client: repository)
    hooks = SimpleNamespace(
        postgres=SimpleNamespace(get_client=lambda: object()),
        akshare=SimpleNamespace(get_client=lambda: _GapAkshare()),
        sina_futures=SimpleNamespace(get_client=lambda: _GapSina()),
    )

    result = sync_position.run(
        SimpleNamespace(hooks=hooks),
        JobRequest(job_key="sync_futures_position", params={"exchanges": ["CFFEX", "DCE"]}),
    )

    assert result["trade_dates"] == ["2026-07-29", "2026-07-30"]
    assert result["backfilled_trade_dates"] == ["2026-07-29"]
    assert len(result["trade_date_results"]) == 2
