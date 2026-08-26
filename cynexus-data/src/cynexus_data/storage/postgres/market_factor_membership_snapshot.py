from __future__ import annotations

import json
from datetime import date, datetime, timezone
from typing import Any

from sqlalchemy import text

from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


class MarketFactorMembershipSnapshotRepository:
    MEMBERSHIP = "ths"

    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def replace_effective_date(self, *, effective_trade_date: date, records: list[dict[str, Any]]) -> int:
        values = [_record(effective_trade_date, value) for value in records]
        with self.client.session() as db:
            db.execute(
                text(f"""DELETE FROM {self.client.schema}.market_factor_membership_snapshot
                           WHERE effective_trade_date=:effective_trade_date AND membership=:membership"""),
                {"effective_trade_date": effective_trade_date, "membership": self.MEMBERSHIP},
            )
            if values:
                db.execute(
                    text(f"""
                        INSERT INTO {self.client.schema}.market_factor_membership_snapshot
                        (id, code, market, effective_trade_date, membership, industry_code, industry_name,
                         source_snapshot_date, source, coverage, available_at)
                        VALUES (:id, :code, :market, :effective_trade_date, :membership, :industry_code, :industry_name,
                                :source_snapshot_date, CAST(:source AS jsonb), CAST(:coverage AS jsonb), :available_at)
                    """),
                    values,
                )
        return len(values)


def _record(effective_trade_date: date, value: dict[str, Any]) -> dict[str, Any]:
    code = str(value.get("stock_code") or "").strip()[-6:].zfill(6)
    industry_code = str(value.get("industry_code") or "").strip()
    source_date = value.get("source_snapshot_date")
    if not code.isdigit() or len(code) != 6 or not industry_code or not isinstance(source_date, date):
        raise ValueError(f"invalid point-in-time THS membership row: {value}")
    return {
        "id": stable_bigint_id("market_factor_membership_snapshot", effective_trade_date, "ths", industry_code, code),
        "code": code,
        "market": "SH" if code.startswith("6") else "SZ",
        "effective_trade_date": effective_trade_date,
        "membership": "ths",
        "industry_code": industry_code,
        "industry_name": value.get("industry_name"),
        "source_snapshot_date": source_date,
        "source": json.dumps({"name": value.get("source") or "q.10jqka.com.cn", "source_type": value.get("source_type")}, default=str, separators=(",", ":")),
        "coverage": json.dumps({"membership": "AVAILABLE", "as_of": source_date.isoformat()}, separators=(",", ":")),
        "available_at": datetime.now(timezone.utc),
    }
