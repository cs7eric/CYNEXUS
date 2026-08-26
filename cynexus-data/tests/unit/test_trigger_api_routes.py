from cynexus_data.interfaces.trigger_api.routes import ASYNC_TRIGGER_JOBS


def test_market_factor_snapshot_backfill_is_detached_from_http_timeout() -> None:
    assert "materialize_market_factor_snapshots" in ASYNC_TRIGGER_JOBS
