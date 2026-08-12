from datetime import datetime, timezone
from types import SimpleNamespace

import pytest

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market import limit_board_monitor


class _Cache:
    rows = [
        {
            "code": "000001",
            "name": "平安银行",
            "market": "SZ",
            "direction": "up",
            "board_status": "sealed",
            "is_on_board": True,
            "is_sealed": True,
            "sealed_amount": 1100000,
            "captured_at": datetime(2026, 8, 11, 1, 30, tzinfo=timezone.utc),
        }
    ]

    def __init__(self, *_args, **_kwargs):
        self.deleted = False

    def read_samples(self, **_kwargs):
        return list(self.rows)

    def delete_session(self, **_kwargs):
        self.deleted = True
        return 4


class _Repository:
    def __init__(self, *_args, **_kwargs):
        pass

    def save_summaries(self, rows, **_kwargs):
        self.rows = rows
        return len(rows)


def _context():
    return SimpleNamespace(
        hooks=SimpleNamespace(
            redis=SimpleNamespace(get_client=lambda: object()),
            postgres=SimpleNamespace(get_client=lambda: object()),
        )
    )


def test_finalize_deletes_redis_only_after_summary_write(monkeypatch):
    cache = _Cache()
    monkeypatch.setattr(limit_board_monitor, "MarketLimitBoardSessionCache", lambda *_args, **_kwargs: cache)
    monkeypatch.setattr(limit_board_monitor, "MarketLimitBoardSessionRepository", _Repository)

    result = limit_board_monitor.finalize(
        _context(),
        JobRequest(
            job_key="market_indicator_limit_board_finalize",
            params={"trade_date": "2026-08-11", "trade_session": "morning"},
        ),
    )

    assert result["rows_written"] == 1
    assert result["redis_deleted"] is True
    assert cache.deleted is True


def test_finalize_keeps_redis_when_postgres_write_fails(monkeypatch):
    cache = _Cache()
    monkeypatch.setattr(limit_board_monitor, "MarketLimitBoardSessionCache", lambda *_args, **_kwargs: cache)

    class _FailingRepository(_Repository):
        def save_summaries(self, rows, **_kwargs):
            raise RuntimeError("postgres unavailable")

    monkeypatch.setattr(limit_board_monitor, "MarketLimitBoardSessionRepository", _FailingRepository)

    with pytest.raises(RuntimeError, match="postgres unavailable"):
        limit_board_monitor.finalize(
            _context(),
            JobRequest(
                job_key="market_indicator_limit_board_finalize",
                params={"trade_date": "2026-08-11", "trade_session": "morning"},
            ),
        )

    assert cache.deleted is False
