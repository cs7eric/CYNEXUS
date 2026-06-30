from __future__ import annotations

from datetime import date, timedelta
from typing import Any

from sqlalchemy import text

from cynexus_data.storage.postgres.client import PostgresClient


DEFAULT_LOOKBACK_DAYS = 1060

ALLOWED_COLUMNS: dict[str, set[str]] = {
    "ma_count_daily": {"new_high_252d_pct", "breadth_raw", "up_5d_pct", "new_low_60d_pct"},
    "risk_appetite_daily": {"spread_weighted", "spread_511010", "spread_511090"},
    "turnover_activity_daily": {"ratio", "score"},
    "profit_effect_daily": {"score"},
    "style_risk_appetite_daily": {"spread"},
    "limit_emotion_summary_daily": {"limit_up_down_ratio", "break_board_rate", "yesterday_limit_up_avg_return", "composite_score"},
    "volatility_sentiment_daily": {"sentiment_score", "realized_vol_20d", "percentile_1y"},
}

TABLE_MAP = {
    "ma_count_daily": "msi_ma_count_daily",
    "risk_appetite_daily": "msi_risk_appetite_daily",
    "turnover_activity_daily": "msi_turnover_activity_daily",
    "profit_effect_daily": "msi_profit_effect_daily",
    "style_risk_appetite_daily": "msi_style_risk_daily",
    "limit_emotion_summary_daily": "msi_limit_emotion_daily",
    "volatility_sentiment_daily": "msi_volatility_daily",
}


class PercentileRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def percentile_score(
        self,
        table: str,
        column: str,
        target_date: str | date,
        current_value: float | int | None,
        *,
        lookback_days: int = DEFAULT_LOOKBACK_DAYS,
        fallback: float = 50.0,
    ) -> float | None:
        if current_value is None:
            return None
        trade_date = _ensure_date(target_date)
        lookback_start = trade_date - timedelta(days=int(lookback_days))
        try:
            table_name, column_name = self._resolve(table, column)
            with self.client.session() as db:
                row = db.execute(
                    text(
                        f"""
                        SELECT COUNT(*) FILTER (WHERE {column_name} < :current_value) * 100.0
                               / NULLIF(COUNT(*), 0) AS score
                          FROM {table_name}
                         WHERE trade_date >= :lookback_start AND trade_date < :target_date
                           AND {column_name} IS NOT NULL
                           AND deleted_at IS NULL
                        """
                    ),
                    {"current_value": float(current_value), "lookback_start": lookback_start, "target_date": trade_date},
                ).first()
        except Exception:
            return fallback
        if not row or row[0] is None:
            return fallback
        return round(float(row[0]), 1)

    def enrich_history_scores(
        self,
        items: list[dict[str, Any]],
        table: str,
        column: str,
        end_date: str | date,
        *,
        lookback_days: int = DEFAULT_LOOKBACK_DAYS,
        score_key: str = "score",
        fallback: float = 50.0,
    ) -> list[dict[str, Any]]:
        if not items:
            return items
        end = _ensure_date(end_date)
        lookback_start = end - timedelta(days=int(lookback_days))
        try:
            table_name, column_name = self._resolve(table, column)
            with self.client.session() as db:
                rows = db.execute(
                    text(
                        f"""
                        WITH t AS (
                          SELECT trade_date, {column_name} AS val
                            FROM {table_name}
                           WHERE trade_date >= :lookback_start AND trade_date <= :end_date
                             AND {column_name} IS NOT NULL
                             AND deleted_at IS NULL
                        )
                        SELECT cur.trade_date,
                               100.0 * SUM(CASE WHEN prev.val < cur.val THEN 1 ELSE 0 END)
                                   / NULLIF(COUNT(prev.val), 0) AS score
                          FROM t cur
                          LEFT JOIN t prev
                            ON prev.trade_date < cur.trade_date
                           AND prev.trade_date >= cur.trade_date - (:lookback_days * INTERVAL '1 day')
                         GROUP BY cur.trade_date
                         ORDER BY cur.trade_date ASC
                        """
                    ),
                    {"lookback_start": lookback_start, "end_date": end, "lookback_days": int(lookback_days)},
                ).all()
            score_map = {row[0].isoformat(): round(float(row[1]), 1) if row[1] is not None else fallback for row in rows}
        except Exception:
            score_map = {}
        for item in items:
            trade_date_key = item.get("tradeDate") or item.get("trade_date")
            item[score_key] = score_map.get(str(trade_date_key), fallback) if trade_date_key else fallback
        return items

    def _resolve(self, table: str, column: str) -> tuple[str, str]:
        allowed = ALLOWED_COLUMNS.get(table)
        if not allowed or column not in allowed:
            raise ValueError(f"unsupported percentile source: {table}.{column}")
        return f"{self.client.schema}.{TABLE_MAP[table]}", column


def _ensure_date(value: str | date) -> date:
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])
