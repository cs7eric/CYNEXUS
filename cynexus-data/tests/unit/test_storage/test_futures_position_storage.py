from contextlib import contextmanager
from datetime import date

from cynexus_data.storage.postgres.futures_position import FuturesPositionRepository


class _Result:
    def scalar_one(self):
        return 123


class _Database:
    def __init__(self):
        self.statements = []

    def execute(self, statement, _params=None):
        self.statements.append(str(statement))
        return _Result()


class _Client:
    schema = "cynexus_appl_market"

    def __init__(self, database):
        self.database = database

    @contextmanager
    def session(self):
        yield self.database


def test_begin_batch_keeps_selected_snapshot_until_replacement_succeeds() -> None:
    database = _Database()
    repository = FuturesPositionRepository(_Client(database))

    assert repository.begin_batch(
        trade_date=date(2026, 7, 30),
        exchange_code="CFFEX",
        provider="akshare.futures_position.cffex",
    ) == 123

    upsert = next(statement for statement in database.statements if "DO UPDATE SET" in statement)
    assert "selected = false" not in upsert


def test_latest_selected_trade_date_reads_only_published_selected_batches() -> None:
    database = _Database()
    repository = FuturesPositionRepository(_Client(database))

    assert repository.latest_selected_trade_date() == 123
    statement = database.statements[-1]
    assert "MAX(trade_date)" in statement
    assert "selected IS TRUE" in statement
    assert "status = 'SUCCEEDED'" in statement
