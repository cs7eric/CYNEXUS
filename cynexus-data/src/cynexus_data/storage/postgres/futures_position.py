from __future__ import annotations

import json
from datetime import date
from pathlib import Path
from typing import Any, Iterable

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.domain.futures.position import PositionRank
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260730_create_futures_position_tables.sql"


class FuturesPositionRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def latest_selected_trade_date(self) -> date | None:
        with self.client.session() as db:
            value = db.execute(text(f"""
                SELECT MAX(trade_date)
                  FROM {self.client.schema}.futures_position_capture_batch
                 WHERE selected IS TRUE AND status = 'SUCCEEDED' AND deleted_at IS NULL
            """)).scalar_one()
        return value

    def begin_batch(self, *, trade_date: date, exchange_code: str, provider: str, expected_instrument_count: int = 0) -> int:
        with self.client.session() as db:
            batch_id = int(db.execute(text(f"SELECT nextval('{self.client.schema}.futures_position_id_seq')")).scalar_one())
            db.execute(text(f"""
                INSERT INTO {self.client.schema}.futures_position_capture_batch (
                    id, trade_date, exchange_code, provider, status, selected,
                    expected_instrument_count, started_at, updated_at
                ) VALUES (:id, :trade_date, :exchange_code, :provider, 'RUNNING', false,
                          :expected_instrument_count, now(), now())
            ON CONFLICT (trade_date, exchange_code, provider) WHERE deleted_at IS NULL
                DO UPDATE SET status = 'RUNNING',
                              expected_instrument_count = EXCLUDED.expected_instrument_count,
                              started_at = now(), completed_at = NULL, error = NULL,
                              warnings = '[]'::jsonb, updated_at = now()
                RETURNING id
            """), {
                "id": batch_id, "trade_date": trade_date, "exchange_code": exchange_code,
                "provider": provider, "expected_instrument_count": expected_instrument_count,
            })
            actual = db.execute(text(f"""
                SELECT id FROM {self.client.schema}.futures_position_capture_batch
                 WHERE trade_date = :trade_date AND exchange_code = :exchange_code
                   AND provider = :provider AND deleted_at IS NULL
            """), {"trade_date": trade_date, "exchange_code": exchange_code, "provider": provider}).scalar_one()
        return int(actual)

    def finish_batch(self, batch_id: int, *, status: str, instrument_count: int, row_count: int,
                     warnings: Iterable[str] = (), error: str | None = None) -> None:
        with self.client.session() as db:
            db.execute(text(f"""
                UPDATE {self.client.schema}.futures_position_capture_batch
                   SET status = :status, actual_instrument_count = :instrument_count,
                       row_count = :row_count, warnings = CAST(:warnings AS jsonb),
                       error = :error, completed_at = now(), updated_at = now()
                 WHERE id = :id
            """), {"id": batch_id, "status": status, "instrument_count": instrument_count,
                   "row_count": row_count, "warnings": json.dumps(list(warnings), ensure_ascii=False), "error": error})

    def replace_selected_snapshot(self, *, batch_id: int, rows: list[PositionRank]) -> int:
        if not rows:
            return 0
        first = rows[0]
        with self.client.session() as db:
            db.execute(text(f"""
                UPDATE {self.client.schema}.futures_position_capture_batch
                   SET selected = false, updated_at = now()
                 WHERE trade_date = :trade_date AND exchange_code = :exchange_code
                   AND selected IS TRUE AND id <> :batch_id AND deleted_at IS NULL
            """), {"trade_date": first.trade_date, "exchange_code": first.exchange_code, "batch_id": batch_id})
            db.execute(text(f"""
                UPDATE {self.client.schema}.futures_position_member_rank
                   SET deleted_at = now(), updated_at = now()
                 WHERE trade_date = :trade_date AND exchange_code = :exchange_code
                   AND deleted_at IS NULL
            """), {"trade_date": first.trade_date, "exchange_code": first.exchange_code})
            for row in rows:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.futures_position_member_rank (
                        id, trade_date, exchange_code, scope_type, instrument_code, variety_code,
                        contract_code, rank_type, rank_no, member_name, rank_value, rank_change,
                        selected_source, capture_batch_id, extra, updated_at, deleted_at
                    ) VALUES (
                        nextval('{self.client.schema}.futures_position_id_seq'), :trade_date, :exchange_code, :scope_type, :instrument_code, :variety_code,
                        :contract_code, :rank_type, :rank_no, :member_name, :rank_value, :rank_change,
                        :selected_source, :capture_batch_id, CAST(:extra AS jsonb), now(), NULL
                    )
                """), {
                    "trade_date": row.trade_date, "exchange_code": row.exchange_code,
                    "scope_type": row.scope_type, "instrument_code": row.instrument_code,
                    "variety_code": row.variety_code, "contract_code": row.contract_code,
                    "rank_type": row.rank_type, "rank_no": row.rank_no, "member_name": row.member_name,
                    "rank_value": row.rank_value, "rank_change": row.rank_change,
                    "selected_source": row.selected_source, "capture_batch_id": batch_id, "extra": "{}",
                })
            db.execute(text(f"""
                UPDATE {self.client.schema}.futures_position_capture_batch
                   SET selected = true, status = 'SUCCEEDED', actual_instrument_count = :instrument_count,
                       row_count = :row_count, completed_at = now(), updated_at = now()
                 WHERE id = :batch_id
            """), {"batch_id": batch_id,
                   "instrument_count": len({row.instrument_code for row in rows}), "row_count": len(rows)})
        return len(rows)

    def upsert_contract_catalog(self, rows: list[dict[str, Any]], *, selected_source: str) -> int:
        if not rows:
            return 0
        with self.client.session() as db:
            for row in rows:
                exchange = str(row["exchange_code"]).upper()
                contract = str(row["contract_code"]).upper()
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.futures_contract_catalog (
                        id, exchange_code, variety_code, contract_code, contract_name,
                        selected_source, extra, updated_at, deleted_at
                    ) VALUES (:id, :exchange_code, :variety_code, :contract_code, :contract_name,
                              :selected_source, CAST(:extra AS jsonb), now(), NULL)
                    ON CONFLICT (exchange_code, contract_code) WHERE deleted_at IS NULL
                    DO UPDATE SET variety_code = EXCLUDED.variety_code,
                                  contract_name = EXCLUDED.contract_name,
                                  selected_source = EXCLUDED.selected_source,
                                  extra = EXCLUDED.extra, updated_at = now(), deleted_at = NULL
                """), {
                    "id": stable_bigint_id("futures_contract", exchange, contract),
                    "exchange_code": exchange, "variety_code": row["variety_code"],
                    "contract_code": contract, "contract_name": row.get("contract_name"),
                    "selected_source": selected_source,
                    "extra": json.dumps(row.get("extra") or {}, ensure_ascii=False, default=str),
                })
        return len(rows)
