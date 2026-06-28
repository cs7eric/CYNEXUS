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
            batch_id = db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.ths_industry_constituent_capture_batch (
                        id, snapshot_date, source_type, status, source, requested_industry_codes,
                        industry_count, row_count, page_row_counts, fetched_at, extra, updated_at
                    ) VALUES (
                        :id, :snapshot_date, :source_type, :status, :source, CAST(:requested_industry_codes AS jsonb),
                        :industry_count, :row_count, CAST(:page_row_counts AS jsonb), :fetched_at, CAST(:extra AS jsonb), now()
                    )
                    RETURNING id
                    """
                ),
                {
                    "id": next_bigint_id(),
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
            ).scalar_one()
            for record in records:
                db.execute(
                    text(
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
                        """
                    ),
                    _record_params(record, batch_id=batch_id, captured_at=captured_at),
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


def _record_params(record: dict[str, Any], *, batch_id: Any, captured_at: datetime) -> dict[str, Any]:
    payload = dict(record)
    payload["id"] = next_bigint_id()
    payload["batch_id"] = batch_id
    payload["captured_at"] = payload.get("captured_at") or captured_at
    payload["extra"] = json.dumps(payload.get("extra") or {}, ensure_ascii=False, default=str)
    return payload
