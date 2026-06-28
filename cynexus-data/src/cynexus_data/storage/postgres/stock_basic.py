from __future__ import annotations

from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_stock_basic_tables.sql"


class StockBasicRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def upsert_many(self, records: list[dict[str, Any]]) -> int:
        if not records:
            return 0
        with self.client.session() as db:
            for record in records:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.sec_stock_basic (
                            code, name, market, board, is_st, limit_up_threshold, is_active, source,
                            industry, total_market_cap, float_market_cap, cap_style, sector_index_symbol,
                            sector_index_name, industry_query, fetched_at, updated_at
                        ) VALUES (
                            :code, :name, :market, :board, :is_st, :limit_up_threshold, :is_active, :source,
                            :industry, :total_market_cap, :float_market_cap, :cap_style, :sector_index_symbol,
                            :sector_index_name, :industry_query, now(), now()
                        )
                        ON CONFLICT (code) DO UPDATE SET
                            name = EXCLUDED.name,
                            market = EXCLUDED.market,
                            board = EXCLUDED.board,
                            is_st = EXCLUDED.is_st,
                            limit_up_threshold = EXCLUDED.limit_up_threshold,
                            is_active = EXCLUDED.is_active,
                            source = EXCLUDED.source,
                            industry = COALESCE(EXCLUDED.industry, {self.client.schema}.sec_stock_basic.industry),
                            total_market_cap = COALESCE(EXCLUDED.total_market_cap, {self.client.schema}.sec_stock_basic.total_market_cap),
                            float_market_cap = COALESCE(EXCLUDED.float_market_cap, {self.client.schema}.sec_stock_basic.float_market_cap),
                            cap_style = COALESCE(EXCLUDED.cap_style, {self.client.schema}.sec_stock_basic.cap_style),
                            sector_index_symbol = COALESCE(EXCLUDED.sector_index_symbol, {self.client.schema}.sec_stock_basic.sector_index_symbol),
                            sector_index_name = COALESCE(EXCLUDED.sector_index_name, {self.client.schema}.sec_stock_basic.sector_index_name),
                            industry_query = COALESCE(EXCLUDED.industry_query, {self.client.schema}.sec_stock_basic.industry_query),
                            fetched_at = EXCLUDED.fetched_at,
                            updated_at = now()
                        """
                    ),
                    _record_params(record),
                )
        return len(records)

    def list_codes(self, active_only: bool = True) -> list[str]:
        where = "WHERE is_active IS TRUE" if active_only else ""
        with self.client.session() as db:
            rows = db.execute(text(f"SELECT code FROM {self.client.schema}.sec_stock_basic {where} ORDER BY code")).all()
        return [str(row[0]) for row in rows]

    def list_limit_meta(self, active_only: bool = True) -> dict[str, dict[str, Any]]:
        where = "WHERE is_active IS TRUE" if active_only else ""
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT code, is_st, limit_up_threshold
                      FROM {self.client.schema}.sec_stock_basic
                      {where}
                     ORDER BY code
                    """
                )
            ).all()
        return {
            str(row[0]).zfill(6): {
                "is_st": bool(row[1]),
                "limit_up_threshold": float(row[2]) if row[2] is not None else None,
            }
            for row in rows
        }


def _record_params(record: dict[str, Any]) -> dict[str, Any]:
    payload = dict(record)
    for key in [
        "industry",
        "total_market_cap",
        "float_market_cap",
        "cap_style",
        "sector_index_symbol",
        "sector_index_name",
        "industry_query",
    ]:
        payload.setdefault(key, None)
    return payload
