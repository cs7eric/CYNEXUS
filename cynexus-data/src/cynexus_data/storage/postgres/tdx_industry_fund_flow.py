from __future__ import annotations

import json
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any
from uuid import uuid4

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260703_create_tdx_industry_fund_flow_tables.sql"
BATCH_TABLE = "tdx_industry_fund_flow_batch"
SNAPSHOT_TABLE = "tdx_industry_fund_flow_snapshot"
DEFAULT_SOURCE = "eltdx.f10.theme_market.200742"
DEFAULT_REQ_ID = "200742"


class TdxIndustryFundFlowRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_snapshot(
        self,
        *,
        trade_date: date,
        rows: list[dict[str, Any]],
        source: str = DEFAULT_SOURCE,
        req_id: str = DEFAULT_REQ_ID,
        fetched_at: datetime | None = None,
    ) -> dict[str, Any]:
        captured_at = fetched_at or datetime.now(timezone.utc)
        batch_id = next_bigint_id()
        batch_key = f"tdx_industry_fund_flow_{trade_date.isoformat()}_{uuid4().hex[:12]}"
        ok_count = sum(1 for row in rows if row.get("snapshot_status") == "ok")
        empty_count = sum(1 for row in rows if row.get("snapshot_status") == "empty")
        error_count = sum(1 for row in rows if row.get("snapshot_status") == "error")
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.{SNAPSHOT_TABLE}
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date
                       AND source = :source
                       AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date, "source": source},
            )
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.{BATCH_TABLE}
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date
                       AND source = :source
                       AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date, "source": source},
            )
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.{BATCH_TABLE} (
                        id, batch_key, trade_date, source, req_id, row_count, ok_count, empty_count,
                        error_count, fetched_at, extra, remark, created_at, updated_at, deleted_at
                    ) VALUES (
                        :id, :batch_key, :trade_date, :source, :req_id, :row_count, :ok_count, :empty_count,
                        :error_count, :fetched_at, CAST(:extra AS jsonb), :remark, now(), now(), NULL
                    )
                    """
                ),
                {
                    "id": batch_id,
                    "batch_key": batch_key,
                    "trade_date": trade_date,
                    "source": source,
                    "req_id": req_id,
                    "row_count": len(rows),
                    "ok_count": ok_count,
                    "empty_count": empty_count,
                    "error_count": error_count,
                    "fetched_at": captured_at,
                    "extra": json.dumps({"trade_date": trade_date.isoformat()}, ensure_ascii=False),
                    "remark": "tdx 56 industry fund flow daily snapshot",
                },
            )
            for row in rows:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.{SNAPSHOT_TABLE} (
                            id, batch_id, trade_date, code6, full_code, tdx_name, flow_sector_name, seed_code,
                            flow_trade_date, snapshot_status, selection_mode, name_match_type, main_net,
                            large_order_net, medium_order_net, small_order_net, row_count, source, extra,
                            generated_at, created_at, updated_at, deleted_at
                        ) VALUES (
                            :id, :batch_id, :trade_date, :code6, :full_code, :tdx_name, :flow_sector_name, :seed_code,
                            :flow_trade_date, :snapshot_status, :selection_mode, :name_match_type, :main_net,
                            :large_order_net, :medium_order_net, :small_order_net, :row_count, :source, CAST(:extra AS jsonb),
                            :generated_at, now(), now(), NULL
                        )
                        ON CONFLICT (trade_date, code6, source) WHERE deleted_at IS NULL DO UPDATE SET
                            batch_id = EXCLUDED.batch_id,
                            full_code = EXCLUDED.full_code,
                            tdx_name = EXCLUDED.tdx_name,
                            flow_sector_name = EXCLUDED.flow_sector_name,
                            seed_code = EXCLUDED.seed_code,
                            flow_trade_date = EXCLUDED.flow_trade_date,
                            snapshot_status = EXCLUDED.snapshot_status,
                            selection_mode = EXCLUDED.selection_mode,
                            name_match_type = EXCLUDED.name_match_type,
                            main_net = EXCLUDED.main_net,
                            large_order_net = EXCLUDED.large_order_net,
                            medium_order_net = EXCLUDED.medium_order_net,
                            small_order_net = EXCLUDED.small_order_net,
                            row_count = EXCLUDED.row_count,
                            extra = EXCLUDED.extra,
                            generated_at = EXCLUDED.generated_at,
                            updated_at = now(),
                            deleted_at = NULL
                        """
                    ),
                    {
                        "id": next_bigint_id(),
                        "batch_id": batch_id,
                        "trade_date": trade_date,
                        "code6": row.get("code6"),
                        "full_code": row.get("full_code"),
                        "tdx_name": row.get("tdx_name"),
                        "flow_sector_name": row.get("flow_sector_name"),
                        "seed_code": row.get("seed_code"),
                        "flow_trade_date": row.get("flow_trade_date"),
                        "snapshot_status": row.get("snapshot_status"),
                        "selection_mode": row.get("selection_mode"),
                        "name_match_type": row.get("name_match_type"),
                        "main_net": row.get("main_net"),
                        "large_order_net": row.get("large_order_net"),
                        "medium_order_net": row.get("medium_order_net"),
                        "small_order_net": row.get("small_order_net"),
                        "row_count": row.get("row_count") or 0,
                        "source": source,
                        "extra": json.dumps(row.get("extra") or {}, ensure_ascii=False, default=str),
                        "generated_at": captured_at,
                    },
                )
        return {
            "batch_id": batch_id,
            "trade_date": trade_date.isoformat(),
            "rows_written": len(rows),
            "ok_count": ok_count,
            "empty_count": empty_count,
            "error_count": error_count,
            "source": source,
        }

    def get_latest_snapshot(self, *, code6: str, max_trade_date: date | None = None, source: str = DEFAULT_SOURCE) -> dict[str, Any] | None:
        where = [
            "code6 = :code6",
            "source = :source",
            "deleted_at IS NULL",
        ]
        params: dict[str, Any] = {"code6": code6, "source": source}
        if max_trade_date is not None:
            where.append("trade_date <= :max_trade_date")
            params["max_trade_date"] = max_trade_date
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT trade_date, code6, full_code, tdx_name, flow_sector_name, seed_code, flow_trade_date,
                           snapshot_status, selection_mode, name_match_type, main_net, large_order_net,
                           medium_order_net, small_order_net, row_count, source, extra, generated_at
                      FROM {self.client.schema}.{SNAPSHOT_TABLE}
                     WHERE {' AND '.join(where)}
                     ORDER BY trade_date DESC
                     LIMIT 1
                    """
                ),
                params,
            ).mappings().first()
        return dict(row) if row else None

    def list_history(self, *, code6: str, max_trade_date: date | None = None, limit_days: int = 30, source: str = DEFAULT_SOURCE) -> list[dict[str, Any]]:
        normalized_limit = max(1, min(limit_days, 252))
        where = [
            "code6 = :code6",
            "source = :source",
            "deleted_at IS NULL",
        ]
        params: dict[str, Any] = {"code6": code6, "source": source, "limit_days": normalized_limit}
        if max_trade_date is not None:
            where.append("trade_date <= :max_trade_date")
            params["max_trade_date"] = max_trade_date
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT trade_date, code6, full_code, tdx_name, flow_sector_name, seed_code, flow_trade_date,
                           snapshot_status, selection_mode, name_match_type, main_net, large_order_net,
                           medium_order_net, small_order_net, row_count, source, extra, generated_at
                      FROM {self.client.schema}.{SNAPSHOT_TABLE}
                     WHERE {' AND '.join(where)}
                     ORDER BY trade_date DESC
                     LIMIT :limit_days
                    """
                ),
                params,
            ).mappings().all()
        history = [dict(row) for row in rows]
        history.reverse()
        return history
