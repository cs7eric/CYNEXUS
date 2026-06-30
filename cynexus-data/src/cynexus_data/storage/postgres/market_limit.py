from __future__ import annotations

import json
from datetime import datetime, timezone
from datetime import date
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_market_limit_tables.sql"
STOCK_TABLE = "market_limit_daily_stocks"
SNAPSHOT_TABLE = "market_limit_daily_snapshots"


class MarketLimitRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_stock_snapshots(
        self,
        *,
        trade_date: date,
        rows: list[dict[str, Any]],
        source: str = "clickhouse.daily_raw",
    ) -> int:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.{STOCK_TABLE}
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date
                       AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date},
            )
            count = 0
            for row in rows:
                code = str(row.get("code") or "").zfill(6)
                category = str(row.get("category") or "")
                if not code or code == "000000" or not category:
                    continue
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.{STOCK_TABLE} (
                            id, trade_date, code, name, category, streak, change_pct,
                            limit_up_price, limit_down_price, updated_at, deleted_at
                        ) VALUES (
                            :id, :trade_date, :code, :name, :category, :streak, :change_pct,
                            :limit_up_price, :limit_down_price, now(), NULL
                        )
                        ON CONFLICT (trade_date, code) WHERE deleted_at IS NULL DO UPDATE SET
                            name = EXCLUDED.name,
                            category = EXCLUDED.category,
                            streak = EXCLUDED.streak,
                            change_pct = EXCLUDED.change_pct,
                            limit_up_price = EXCLUDED.limit_up_price,
                            limit_down_price = EXCLUDED.limit_down_price,
                            updated_at = now(),
                            deleted_at = NULL
                        """
                    ),
                    {
                        "id": next_bigint_id(),
                        "trade_date": trade_date,
                        "code": code,
                        "name": row.get("name"),
                        "category": category,
                        "streak": row.get("streak"),
                        "change_pct": row.get("changePct"),
                        "limit_up_price": row.get("limitUpPrice"),
                        "limit_down_price": row.get("limitDownPrice"),
                    },
                )
                count += 1
        return count

    def upsert_daily_snapshot(
        self,
        *,
        payload: dict[str, Any],
        stock_rows: list[dict[str, Any]],
        source: str = "clickhouse.daily_raw",
    ) -> int:
        trade_date = date.fromisoformat(str(payload["tradeDate"])[:10])
        streak_summary = payload.get("streakSummary") or {}
        extra = _limit_emotion_extra(payload, stock_rows)
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.{SNAPSHOT_TABLE} (
                        id, trade_date, limit_up_count, limit_down_count, touched_count, broken_count,
                        break_board_rate, max_streak_height, promotion_overall_rate, sentiment_level,
                        sentiment_text, stock_count, market_status, data_status, source, extra,
                        updated_at, deleted_at
                    ) VALUES (
                        :id, :trade_date, :limit_up_count, :limit_down_count, :touched_count, :broken_count,
                        :break_board_rate, :max_streak_height, :promotion_overall_rate, :sentiment_level,
                        :sentiment_text, :stock_count, :market_status, :data_status, :source, CAST(:extra AS jsonb),
                        now(), NULL
                    )
                    ON CONFLICT (trade_date) WHERE deleted_at IS NULL DO UPDATE SET
                        limit_up_count = EXCLUDED.limit_up_count,
                        limit_down_count = EXCLUDED.limit_down_count,
                        touched_count = EXCLUDED.touched_count,
                        broken_count = EXCLUDED.broken_count,
                        break_board_rate = EXCLUDED.break_board_rate,
                        max_streak_height = EXCLUDED.max_streak_height,
                        promotion_overall_rate = EXCLUDED.promotion_overall_rate,
                        sentiment_level = EXCLUDED.sentiment_level,
                        sentiment_text = EXCLUDED.sentiment_text,
                        stock_count = EXCLUDED.stock_count,
                        market_status = EXCLUDED.market_status,
                        data_status = EXCLUDED.data_status,
                        source = EXCLUDED.source,
                        extra = EXCLUDED.extra,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "id": next_bigint_id(),
                    "trade_date": trade_date,
                    "limit_up_count": int(payload.get("limitUpCount") or 0),
                    "limit_down_count": int(payload.get("limitDownCount") or 0),
                    "touched_count": int(payload.get("touchedCount") or 0),
                    "broken_count": int(payload.get("brokenCount") or 0),
                    "break_board_rate": payload.get("breakBoardRate"),
                    "max_streak_height": int(streak_summary.get("maxHeight") or 0),
                    "promotion_overall_rate": (streak_summary.get("promoted") or {}).get("overallRate"),
                    "sentiment_level": payload.get("level") or "normal",
                    "sentiment_text": _sentiment_text(payload.get("level")),
                    "stock_count": len(stock_rows),
                    "market_status": "closed",
                    "data_status": "normal",
                    "source": source,
                    "extra": json.dumps(extra, ensure_ascii=False, default=str),
                },
            )
        return 1

    def get_stock_snapshots(
        self,
        *,
        trade_date: date,
        category: str | None = None,
        source: str = "clickhouse.daily_raw",
    ) -> list[dict[str, Any]]:
        category_sql = "AND category = :category" if category else ""
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT trade_date, code, name, category, streak, change_pct,
                           limit_up_price, limit_down_price
                      FROM {self.client.schema}.{STOCK_TABLE}
                     WHERE trade_date = :trade_date
                       AND deleted_at IS NULL
                       {category_sql}
                     ORDER BY streak DESC NULLS LAST, code
                    """
                ),
                {"trade_date": trade_date, "category": category},
            ).mappings().all()
        return [dict(row) for row in rows]


def _limit_emotion_extra(payload: dict[str, Any], stock_rows: list[dict[str, Any]]) -> dict[str, Any]:
    trade_date = str(payload.get("tradeDate") or "")[:10]
    limit_up = [_stock_payload(row) for row in stock_rows if row.get("category") == "limit_up"]
    limit_down = [_stock_payload(row) for row in stock_rows if row.get("category") == "limit_down"]
    broken = [_stock_payload(row) for row in stock_rows if row.get("category") == "broken"]
    streak_summary = payload.get("streakSummary") or {}
    max_height = streak_summary.get("maxHeight") or 0
    distribution = [
        {
            "streak": item.get("streak"),
            "height": item.get("streak"),
            "count": item.get("count"),
            "stocks": [stock for stock in limit_up if stock.get("streak") == item.get("streak")],
        }
        for item in streak_summary.get("distribution") or []
    ]
    promoted = streak_summary.get("promoted") or {}
    broken_summary = streak_summary.get("broken") or {}
    return {
        "ok": True,
        "source": payload.get("source") or "clickhouse.daily_raw",
        "tradeDate": trade_date,
        "updateTime": datetime.now(timezone.utc).isoformat(),
        "marketStatus": "closed",
        "dataStatus": "normal",
        "limitUp": {"count": payload.get("limitUpCount"), "label": "涨停", "stocks": limit_up},
        "limitDown": {"count": payload.get("limitDownCount"), "label": "跌停", "stocks": limit_down},
        "breakBoard": {
            "rate": payload.get("breakBoardRate"),
            "label": "炸板率",
            "status": "ready",
            "touchedCount": payload.get("touchedCount"),
            "brokenCount": payload.get("brokenCount"),
            "brokenStocks": broken,
        },
        "streak": {
            "maxHeight": max_height,
            "label": "连板高度",
            "leaders": [stock for stock in limit_up if stock.get("streak") == max_height],
            "distribution": distribution,
            "promotion": {
                "overallRate": promoted.get("overallRate"),
                "levels": promoted.get("levels") or [],
            },
            "broken": {
                "count": broken_summary.get("count") or len(broken),
                "highStreakBrokenCount": broken_summary.get("highStreakBrokenCount") or 0,
                "stocks": broken,
            },
            "sentiment": {
                "level": payload.get("level") or "normal",
                "text": _sentiment_text(payload.get("level")),
            },
        },
    }


def _stock_payload(row: dict[str, Any]) -> dict[str, Any]:
    return {
        "code": str(row.get("code") or "").zfill(6),
        "name": row.get("name"),
        "streak": row.get("streak") or row.get("limitUpStreak"),
        "previousStreak": row.get("previousStreak") or row.get("previousLimitUpStreak"),
        "changePct": row.get("changePct") or row.get("change_pct"),
        "latestPrice": row.get("latestPrice") or row.get("close"),
        "limitUpPrice": row.get("limitUpPrice") or row.get("limit_up_price"),
        "limitDownPrice": row.get("limitDownPrice") or row.get("limit_down_price"),
    }


def _sentiment_text(level: Any) -> str:
    return {
        "hot": "情绪高涨",
        "active": "情绪活跃",
        "normal": "情绪正常",
        "weak": "情绪偏弱",
        "ice": "情绪冰点",
    }.get(str(level or "normal"), "情绪正常")
