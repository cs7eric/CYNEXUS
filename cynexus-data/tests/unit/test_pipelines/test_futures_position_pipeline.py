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


class _Akshare:
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
    def fetch_dce_position_rows(self, trade_date, *, varieties):
        assert trade_date == date(2026, 7, 30)
        assert varieties == []
        return []


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
