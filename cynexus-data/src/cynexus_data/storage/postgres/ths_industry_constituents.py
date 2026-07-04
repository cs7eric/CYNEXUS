from __future__ import annotations

import json
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_ths_industry_constituent_tables.sql"


class ThsIndustryConstituentRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_snapshot(
        self,
        *,
        snapshot_date: date,
        records: list[dict[str, Any]],
        requested_industry_codes: list[str],
        source: str = "q.10jqka.com.cn",
        source_type: str = "crawler",
        page_row_counts: dict[str, list[int]] | None = None,
        fetched_at: datetime | None = None,
        extra: dict[str, Any] | None = None,
        status: str = "success",
    ) -> int:
        captured_at = fetched_at or datetime.now(timezone.utc)
        industry_codes = [str(code).strip() for code in requested_industry_codes if str(code).strip()]
        if not industry_codes:
            industry_codes = sorted({str(record.get("industry_code")) for record in records if record.get("industry_code")})
        with self.client.session() as db:
            for industry_code in industry_codes:
                db.execute(
                    text(
                        f"""
                        UPDATE {self.client.schema}.ths_industry_constituent_snapshot
                           SET deleted_at = now(), updated_at = now()
                         WHERE snapshot_date = :snapshot_date
                           AND source = :source
                           AND industry_code = :industry_code
                           AND deleted_at IS NULL
                        """
                    ),
                    {"snapshot_date": snapshot_date, "source": source, "industry_code": industry_code},
                )
            for industry_code in industry_codes:
                db.execute(
                    text(
                        f"""
                        UPDATE {self.client.schema}.ths_industry_constituent_capture_batch
                           SET deleted_at = now(), updated_at = now()
                         WHERE snapshot_date = :snapshot_date
                           AND source = :source
                           AND requested_industry_codes @> CAST(:one_code AS jsonb)
                           AND deleted_at IS NULL
                        """
                    ),
                    {
                        "snapshot_date": snapshot_date,
                        "source": source,
                        "one_code": json.dumps([industry_code], ensure_ascii=False),
                    },
                )
            batch_stmt = text(
                f"""
                    INSERT INTO {self.client.schema}.ths_industry_constituent_capture_batch (
                        id, snapshot_date, source_type, status, source, requested_industry_codes,
                        industry_count, row_count, page_row_counts, fetched_at, extra, updated_at
                    ) VALUES (
                        :id, :snapshot_date, :source_type, :status, :source, CAST(:requested_industry_codes AS jsonb),
                        :industry_count, :row_count, CAST(:page_row_counts AS jsonb), :fetched_at, CAST(:extra AS jsonb), now()
                    )
                    ON CONFLICT (id) DO NOTHING
                    RETURNING id
                    """
            )
            batch_id = _execute_generated_id_insert(
                db,
                batch_stmt,
                {
                    "snapshot_date": snapshot_date,
                    "source_type": source_type,
                    "status": status,
                    "source": source,
                    "requested_industry_codes": json.dumps(industry_codes, ensure_ascii=False),
                    "industry_count": len(industry_codes),
                    "row_count": len(records),
                    "page_row_counts": json.dumps(page_row_counts or {}, ensure_ascii=False),
                    "fetched_at": captured_at,
                    "extra": json.dumps(extra or {}, ensure_ascii=False, default=str),
                },
                label="ths_industry_constituent_capture_batch",
            )
            snapshot_stmt = text(
                f"""
                INSERT INTO {self.client.schema}.ths_industry_constituent_snapshot (
                    id, batch_id, snapshot_date, industry_code, stock_code, stock_name, rank,
                    price, change_pct, change, speed_pct, turnover_pct, volume_ratio,
                    amplitude_pct, amount_text, float_shares_text, float_market_cap_text,
                    pe, source, source_type, captured_at, extra, updated_at
                ) VALUES (
                    :id, :batch_id, :snapshot_date, :industry_code, :stock_code, :stock_name, :rank,
                    :price, :change_pct, :change, :speed_pct, :turnover_pct, :volume_ratio,
                    :amplitude_pct, :amount_text, :float_shares_text, :float_market_cap_text,
                    :pe, :source, :source_type, :captured_at, CAST(:extra AS jsonb), now()
                )
                ON CONFLICT (id) DO NOTHING
                RETURNING id
                """
            )
            for record in records:
                _execute_generated_id_insert(
                    db,
                    snapshot_stmt,
                    _record_params(record, batch_id=batch_id, captured_at=captured_at),
                    label="ths_industry_constituent_snapshot",
                )
        return len(records)

    def get_snapshot(self, snapshot_date: date, *, industry_code: str, source: str = "q.10jqka.com.cn") -> list[dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT snapshot_date, industry_code, stock_code, stock_name, rank, price,
                           change_pct, turnover_pct, amount_text, float_market_cap_text,
                           source, source_type, captured_at
                      FROM {self.client.schema}.ths_industry_constituent_snapshot
                     WHERE snapshot_date = :snapshot_date
                       AND industry_code = :industry_code
                       AND source = :source
                       AND deleted_at IS NULL
                     ORDER BY rank ASC NULLS LAST, stock_code
                    """
                ),
                {"snapshot_date": snapshot_date, "industry_code": industry_code, "source": source},
            ).mappings().all()
        return [dict(row) for row in rows]

    def list_latest_records_before(
        self,
        *,
        snapshot_date: date,
        industry_code: str,
        source: str = "q.10jqka.com.cn",
    ) -> list[dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT snapshot_date, industry_code, stock_code, stock_name, rank, price,
                           change_pct, change, speed_pct, turnover_pct, volume_ratio,
                           amplitude_pct, amount_text, float_shares_text, float_market_cap_text,
                           pe, source, source_type, captured_at, extra
                      FROM {self.client.schema}.ths_industry_constituent_snapshot
                     WHERE snapshot_date = (
                           SELECT max(snapshot_date)
                             FROM {self.client.schema}.ths_industry_constituent_snapshot
                            WHERE snapshot_date < :snapshot_date
                              AND industry_code = :industry_code
                              AND source = :source
                              AND deleted_at IS NULL
                       )
                       AND industry_code = :industry_code
                       AND source = :source
                       AND deleted_at IS NULL
                     ORDER BY rank ASC NULLS LAST, stock_code
                    """
                ),
                {"snapshot_date": snapshot_date, "industry_code": industry_code, "source": source},
            ).mappings().all()
        return [dict(row) for row in rows]

    def list_latest_industry_codes(self, *, source: str = "q.10jqka.com.cn") -> list[str]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    WITH latest AS (
                        SELECT max(snapshot_date) AS snapshot_date
                          FROM {self.client.schema}.ths_industry_constituent_snapshot
                         WHERE source = :source
                           AND deleted_at IS NULL
                    )
                    SELECT DISTINCT s.industry_code
                      FROM {self.client.schema}.ths_industry_constituent_snapshot s
                      JOIN latest l ON s.snapshot_date = l.snapshot_date
                     WHERE s.source = :source
                       AND s.deleted_at IS NULL
                       AND s.industry_code IS NOT NULL
                       AND s.industry_code <> ''
                     ORDER BY s.industry_code
                    """
                ),
                {"source": source},
            ).scalars().all()
        return [str(code).strip() for code in rows if str(code).strip()]


def _record_params(record: dict[str, Any], *, batch_id: Any, captured_at: datetime) -> dict[str, Any]:
    payload = dict(record)
    payload["batch_id"] = batch_id
    payload["captured_at"] = payload.get("captured_at") or captured_at
    payload["extra"] = json.dumps(payload.get("extra") or {}, ensure_ascii=False, default=str)
    return payload


def _execute_generated_id_insert(
    db: Any,
    statement: Any,
    params: dict[str, Any],
    *,
    label: str,
    max_attempts: int = 16,
) -> int:
    for _ in range(max_attempts):
        candidate = dict(params)
        candidate["id"] = next_bigint_id()
        inserted_id = db.execute(statement, candidate).scalar_one_or_none()
        if inserted_id is not None:
            return int(inserted_id)
    raise RuntimeError(f"failed to allocate unique id for {label} after {max_attempts} attempts")
