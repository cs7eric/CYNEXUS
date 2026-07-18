from __future__ import annotations

import json
from datetime import date, datetime
from typing import Any

from sqlalchemy import text

from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


class FundFlowQualityCheckRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        with self.client.engine.begin() as conn:
            conn.execute(
                text(
                    f"""
                    CREATE TABLE IF NOT EXISTS {self.client.schema}.a_stock_fund_flow_daily_qc (
                        id BIGINT PRIMARY KEY,
                        run_id TEXT NOT NULL,
                        sample_date DATE NOT NULL,
                        code TEXT NOT NULL,
                        trade_date DATE,
                        easy_tdx_source TEXT,
                        eastmoney_source TEXT,
                        easy_tdx_main_net NUMERIC,
                        eastmoney_main_net NUMERIC,
                        main_net_diff NUMERIC,
                        main_net_diff_ratio NUMERIC,
                        direction_match BOOLEAN,
                        easy_tdx_payload JSONB,
                        eastmoney_payload JSONB,
                        error TEXT,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        UNIQUE (run_id, code)
                    )
                    """
                )
            )
            conn.execute(
                text(
                    f"""
                    CREATE INDEX IF NOT EXISTS idx_a_stock_fund_flow_daily_qc_sample_date
                      ON {self.client.schema}.a_stock_fund_flow_daily_qc (sample_date DESC, code)
                    """
                )
            )

    def upsert_many(self, rows: list[dict[str, Any]]) -> int:
        if not rows:
            return 0
        with self.client.session() as db:
            for row in rows:
                record = _record(row)
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.a_stock_fund_flow_daily_qc (
                            id, run_id, sample_date, code, trade_date, easy_tdx_source,
                            eastmoney_source, easy_tdx_main_net, eastmoney_main_net,
                            main_net_diff, main_net_diff_ratio, direction_match,
                            easy_tdx_payload, eastmoney_payload, error, updated_at
                        ) VALUES (
                            :id, :run_id, :sample_date, :code, :trade_date, :easy_tdx_source,
                            :eastmoney_source, :easy_tdx_main_net, :eastmoney_main_net,
                            :main_net_diff, :main_net_diff_ratio, :direction_match,
                            CAST(:easy_tdx_payload AS jsonb), CAST(:eastmoney_payload AS jsonb),
                            :error, now()
                        )
                        ON CONFLICT (run_id, code) DO UPDATE SET
                            trade_date = EXCLUDED.trade_date,
                            easy_tdx_source = EXCLUDED.easy_tdx_source,
                            eastmoney_source = EXCLUDED.eastmoney_source,
                            easy_tdx_main_net = EXCLUDED.easy_tdx_main_net,
                            eastmoney_main_net = EXCLUDED.eastmoney_main_net,
                            main_net_diff = EXCLUDED.main_net_diff,
                            main_net_diff_ratio = EXCLUDED.main_net_diff_ratio,
                            direction_match = EXCLUDED.direction_match,
                            easy_tdx_payload = EXCLUDED.easy_tdx_payload,
                            eastmoney_payload = EXCLUDED.eastmoney_payload,
                            error = EXCLUDED.error,
                            updated_at = now()
                        """
                    ),
                    record,
                )
        return len(rows)

    def latest_summary(self, *, limit: int = 5) -> dict[str, Any] | None:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    WITH latest AS (
                        SELECT run_id
                          FROM {self.client.schema}.a_stock_fund_flow_daily_qc
                         ORDER BY created_at DESC
                         LIMIT 1
                    )
                    SELECT code, trade_date, easy_tdx_main_net, eastmoney_main_net,
                           main_net_diff, main_net_diff_ratio, direction_match, error
                      FROM {self.client.schema}.a_stock_fund_flow_daily_qc
                     WHERE run_id = (SELECT run_id FROM latest)
                     ORDER BY code
                    """
                )
            ).mappings().all()
        if not rows:
            return None
        success = [row for row in rows if row["error"] is None and row["direction_match"] is not None]
        matched = [row for row in success if row["direction_match"] is True]
        return {
            "sampleSize": len(rows),
            "successCount": len(success),
            "failedCount": len(rows) - len(success),
            "directionMatchCount": len(matched),
            "directionMatchRate": round(len(matched) / len(success), 4) if success else None,
            "samples": [
                {
                    "code": row["code"],
                    "tradeDate": row["trade_date"].isoformat() if row["trade_date"] else None,
                    "easyTdxMainNet": float(row["easy_tdx_main_net"]) if row["easy_tdx_main_net"] is not None else None,
                    "eastmoneyMainNet": float(row["eastmoney_main_net"]) if row["eastmoney_main_net"] is not None else None,
                    "mainNetDiff": float(row["main_net_diff"]) if row["main_net_diff"] is not None else None,
                    "mainNetDiffRatio": float(row["main_net_diff_ratio"]) if row["main_net_diff_ratio"] is not None else None,
                    "directionMatch": row["direction_match"],
                    "error": row["error"],
                }
                for row in rows[: max(1, limit)]
            ],
        }


def _record(row: dict[str, Any]) -> dict[str, Any]:
    run_id = str(row["run_id"])
    code = str(row["code"]).zfill(6)
    sample_date = _date(row.get("sample_date")) or date.today()
    payload = dict(row)
    payload["id"] = row.get("id") or stable_bigint_id("a_stock_fund_flow_daily_qc", run_id, code)
    payload["run_id"] = run_id
    payload["code"] = code
    payload["sample_date"] = sample_date
    payload["trade_date"] = _date(row.get("trade_date"))
    for key in (
        "easy_tdx_source",
        "eastmoney_source",
        "easy_tdx_main_net",
        "eastmoney_main_net",
        "main_net_diff",
        "main_net_diff_ratio",
        "direction_match",
        "error",
    ):
        payload.setdefault(key, None)
    payload["easy_tdx_payload"] = json.dumps(row.get("easy_tdx_payload"), ensure_ascii=False, default=str, separators=(",", ":"))
    payload["eastmoney_payload"] = json.dumps(row.get("eastmoney_payload"), ensure_ascii=False, default=str, separators=(",", ":"))
    return payload


def _date(value: Any) -> date | None:
    if isinstance(value, date) and not isinstance(value, datetime):
        return value
    if isinstance(value, datetime):
        return value.date()
    if value in (None, ""):
        return None
    return datetime.fromisoformat(str(value)[:10]).date()
