from __future__ import annotations

import json
from datetime import datetime
from typing import Any

from sqlalchemy import text

from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


class MarketEventIntradaySnapshotRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def replace_event(self, *, code: str, market: str, trade_date: str, anchor_at: datetime, records: list[dict[str, Any]]) -> int:
        rows = [_record(code, market, trade_date, anchor_at, value) for value in records]
        with self.client.session() as db:
            db.execute(text(f"""
                DELETE FROM {self.client.schema}.market_event_intraday_snapshot
                 WHERE code=:code AND market=:market AND trade_date=:trade_date AND anchor_at=:anchor_at
            """), {"code": code, "market": market, "trade_date": trade_date, "anchor_at": anchor_at})
            if rows:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.market_event_intraday_snapshot
                    (id, code, market, trade_date, anchor_at, minute_at, offset_minutes, scope, membership,
                     industry_code, primitive_code, value, source, coverage, available_at)
                    VALUES (:id, :code, :market, :trade_date, :anchor_at, :minute_at, :offset_minutes, :scope,
                            :membership, :industry_code, :primitive_code, :value, CAST(:source AS jsonb),
                            CAST(:coverage AS jsonb), :available_at)
                """), rows)
        return len(rows)


def _record(code: str, market: str, trade_date: str, anchor_at: datetime, value: dict[str, Any]) -> dict[str, Any]:
    minute_at = value["minute_at"]
    if not isinstance(minute_at, datetime):
        raise ValueError("minute_at must be a datetime")
    primitive_code = str(value["primitive_code"])
    return {
        "id": stable_bigint_id("market_event_intraday_snapshot", code, market, trade_date, anchor_at, minute_at, "STOCK", primitive_code),
        "code": code, "market": market, "trade_date": trade_date, "anchor_at": anchor_at, "minute_at": minute_at,
        "offset_minutes": round((minute_at - anchor_at).total_seconds() / 60), "scope": "STOCK", "membership": None,
        "industry_code": None, "primitive_code": primitive_code, "value": value.get("value"),
        "source": json.dumps({"name": "clickhouse.ch_dwd_kline_1m", "source_trade_date": trade_date}, separators=(",", ":")),
        # A minute observation becomes usable only at that minute.  Using a
        # later job timestamp would make an otherwise valid historical anchor
        # depend on when a backfill happened to run.
        "coverage": json.dumps({"stock_1m": "AVAILABLE"}, separators=(",", ":")), "available_at": minute_at,
    }
