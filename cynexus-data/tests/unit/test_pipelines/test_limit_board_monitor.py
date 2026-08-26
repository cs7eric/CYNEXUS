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


def test_session_continues_after_a_transient_capture_failure(monkeypatch):
    calls = 0

    def flaky_capture(*_args, **_kwargs):
        nonlocal calls
        calls += 1
        if calls == 1:
            raise RuntimeError("provider disconnected")
        return {"success": True, "rows_read": 1, "rows_written": 1}

    monkeypatch.setattr(limit_board_monitor, "capture", flaky_capture)
    monkeypatch.setattr(limit_board_monitor, "current_trade_session", lambda *_args: "morning")
    monkeypatch.setattr(
        limit_board_monitor,
        "MarketIndicatorRepository",
        lambda *_args, **_kwargs: SimpleNamespace(list_stock_metadata=lambda: {}),
    )
    monkeypatch.setattr(
        limit_board_monitor,
        "session_bounds",
        lambda *_args: (
            datetime(2026, 8, 11, 1, 30, tzinfo=timezone.utc),
            datetime(2100, 8, 11, 3, 30, tzinfo=timezone.utc),
        ),
    )
    monkeypatch.setattr(limit_board_monitor.MarketLimitBoardSessionCache, "set_session_state", lambda *_args, **_kwargs: None)

    result = limit_board_monitor.run_session(
        _context(),
        JobRequest(
            job_key="market_indicator_limit_board_session",
            params={"duration_seconds": 60, "max_cycles": 2, "target_cadence_seconds": 1, "failure_backoff_seconds": 0},
        ),
    )

    assert calls == 2
    assert result["failed_cycles"] == 1
    assert result["success"] is False
    assert result["cycle_results"][1]["success"] is True


def test_session_defaults_to_three_second_rolling_cadence():
    result = limit_board_monitor.run_session(
        _context(),
        JobRequest(job_key="market_indicator_limit_board_session", dry_run=True),
    )

    assert result["target_cadence_seconds"] == 3


def test_finalize_skips_while_capture_session_is_active(monkeypatch):
    cache = _Cache()
    cache.is_session_active = lambda **_kwargs: True
    monkeypatch.setattr(limit_board_monitor, "MarketLimitBoardSessionCache", lambda *_args, **_kwargs: cache)

    result = limit_board_monitor.finalize(
        _context(),
        JobRequest(
            job_key="market_indicator_limit_board_finalize",
            params={"trade_date": "2026-08-11", "trade_session": "morning"},
        ),
    )

    assert result["status"] == "skipped"
    assert result["reason"] == "capture_session_active"
    assert cache.deleted is False


def test_capture_waits_for_whole_market_discovery(monkeypatch):
    class _EmptyCache(_Cache):
        def seen_directions(self, **_kwargs): return {}
        def seen_codes(self, **_kwargs): return set()

    cache = _EmptyCache()
    monkeypatch.setattr(limit_board_monitor, "MarketLimitBoardSessionCache", lambda *_args, **_kwargs: cache)
    monkeypatch.setattr(
        limit_board_monitor,
        "MarketIndicatorRepository",
        lambda *_args, **_kwargs: SimpleNamespace(list_stock_metadata=lambda: {"000001": {"market": "SZ"}}),
    )
    monkeypatch.setattr(limit_board_monitor, "current_trade_session", lambda *_args: "morning")

    result = limit_board_monitor.capture(
        _context(),
        JobRequest(job_key="market_indicator_limit_board_session"),
    )

    assert result["status"] == "waiting_for_discovery"
    assert result["success"] is True
