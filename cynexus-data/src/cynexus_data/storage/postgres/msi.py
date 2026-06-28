from __future__ import annotations

from datetime import date
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.domain.market.msi import WEIGHTS, sector_breadth_score
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_msi_tables.sql"


class MsiRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def get_index(self, trade_date: date) -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT trade_date, vol_score, turnover_score, price_strength_score, risk_appetite_score,
                           breadth_score, limit_emotion_score, profit_effect_score, sector_breadth_score,
                           style_risk_score, composite_score, component_count, level, source
                      FROM {self.client.schema}.msi_index_daily
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                     LIMIT 1
                    """
                ),
                {"trade_date": trade_date},
            ).first()
        return _row_to_payload(row) if row else None

    def save_index(self, payload: dict[str, Any]) -> int:
        components = payload.get("components") or {}
        params = {
            "trade_date": date.fromisoformat(str(payload["tradeDate"])),
            "vol_score": components.get("vol"),
            "turnover_score": components.get("turnover"),
            "price_strength_score": components.get("price_strength"),
            "risk_appetite_score": components.get("risk_appetite"),
            "breadth_score": components.get("breadth"),
            "limit_emotion_score": components.get("limit_emotion"),
            "profit_effect_score": components.get("profit_effect"),
            "sector_breadth_score": components.get("sector_breadth"),
            "style_risk_score": components.get("style_risk"),
            "composite_score": payload.get("compositeScore"),
            "component_count": int(payload.get("componentCount") or 0),
            "level": payload.get("level") or "normal",
            "source": payload.get("source") or "composite",
        }
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.msi_index_daily (
                        trade_date, vol_score, turnover_score, price_strength_score, risk_appetite_score,
                        breadth_score, limit_emotion_score, profit_effect_score, sector_breadth_score,
                        style_risk_score, composite_score, component_count, level, source, ingested_at, updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :vol_score, :turnover_score, :price_strength_score, :risk_appetite_score,
                        :breadth_score, :limit_emotion_score, :profit_effect_score, :sector_breadth_score,
                        :style_risk_score, :composite_score, :component_count, :level, :source, now(), now(), NULL
                    )
                    ON CONFLICT (trade_date) DO UPDATE SET
                        vol_score = EXCLUDED.vol_score,
                        turnover_score = EXCLUDED.turnover_score,
                        price_strength_score = EXCLUDED.price_strength_score,
                        risk_appetite_score = EXCLUDED.risk_appetite_score,
                        breadth_score = EXCLUDED.breadth_score,
                        limit_emotion_score = EXCLUDED.limit_emotion_score,
                        profit_effect_score = EXCLUDED.profit_effect_score,
                        sector_breadth_score = EXCLUDED.sector_breadth_score,
                        style_risk_score = EXCLUDED.style_risk_score,
                        composite_score = EXCLUDED.composite_score,
                        component_count = EXCLUDED.component_count,
                        level = EXCLUDED.level,
                        source = EXCLUDED.source,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                params,
            )
        return 1

    def read_component_scores(self, trade_date: date) -> dict[str, float | None]:
        scores: dict[str, float | None] = {key: None for key in WEIGHTS}
        for key, table, column, multiplier in _COMPONENT_SOURCES:
            value = self._fetch_optional_column(table, column, trade_date)
            scores[key] = round(float(value) * multiplier, 2) if value is not None else None
        return scores

    def compute_sector_breadth_from_fund_flow(self, trade_date: date) -> dict[str, Any] | None:
        try:
            with self.client.session() as db:
                row = db.execute(
                    text(
                        f"""
                        SELECT COUNT(*) FILTER (WHERE change_pct > 0) AS advancing,
                               COUNT(*) FILTER (WHERE change_pct < 0) AS declining,
                               COUNT(*) FILTER (WHERE change_pct = 0) AS flat,
                               COUNT(*) AS total
                          FROM {self.client.schema}.sector_fund_flow_daily_snapshot
                         WHERE trade_date = :trade_date AND scope = 'industry' AND deleted_at IS NULL
                        """
                    ),
                    {"trade_date": trade_date},
                ).first()
        except Exception:
            return None
        if not row or int(row[3] or 0) <= 0:
            return None
        advancing = int(row[0] or 0)
        declining = int(row[1] or 0)
        flat = int(row[2] or 0)
        total = int(row[3] or 0)
        score = sector_breadth_score(advancing=advancing, total=total)
        return {
            "tradeDate": trade_date.isoformat(),
            "advancing": advancing,
            "declining": declining,
            "flat": flat,
            "total": total,
            "advancePct": round(advancing / total, 6),
            "score": score,
            "source": "postgres.sector_fund_flow_daily_snapshot",
        }

    def save_sector_breadth(self, payload: dict[str, Any]) -> int:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.msi_sector_breadth_daily (
                        trade_date, advancing, declining, flat, total, advance_pct, score, source,
                        ingested_at, updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :advancing, :declining, :flat, :total, :advance_pct, :score, :source,
                        now(), now(), NULL
                    )
                    ON CONFLICT (trade_date) DO UPDATE SET
                        advancing = EXCLUDED.advancing,
                        declining = EXCLUDED.declining,
                        flat = EXCLUDED.flat,
                        total = EXCLUDED.total,
                        advance_pct = EXCLUDED.advance_pct,
                        score = EXCLUDED.score,
                        source = EXCLUDED.source,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "trade_date": date.fromisoformat(str(payload["tradeDate"])),
                    "advancing": int(payload.get("advancing") or 0),
                    "declining": int(payload.get("declining") or 0),
                    "flat": int(payload.get("flat") or 0),
                    "total": int(payload.get("total") or 0),
                    "advance_pct": payload.get("advancePct"),
                    "score": payload.get("score"),
                    "source": payload.get("source") or "postgres.sector_fund_flow_daily_snapshot",
                },
            )
        return 1

    def save_ma_count(self, payload: dict[str, Any]) -> int:
        params = {
            "trade_date": date.fromisoformat(str(payload["tradeDate"])),
            "total_eligible": int(payload.get("totalEligible") or 0),
            "above_ma20": int(payload.get("aboveMa20") or 0),
            "above_ma60": int(payload.get("aboveMa60") or 0),
            "above_both": int(payload.get("aboveBoth") or 0),
            "pct_ma20": payload.get("pctAboveMa20"),
            "pct_ma60": payload.get("pctAboveMa60"),
            "pct_both": payload.get("pctAboveBoth"),
            "up_5d_count": int(payload.get("up5dCount") or 0),
            "up_5d_pct": payload.get("pctUp5d"),
            "new_low_60d_count": int(payload.get("newLow60dCount") or 0),
            "new_low_60d_pct": payload.get("pctNewLow60d"),
            "new_high_252d_count": int(payload.get("newHigh252dCount") or 0),
            "new_high_252d_pct": payload.get("pctNewHigh252d"),
            "advancing_count": int(payload.get("advancingCount") or 0),
            "advancing_pct": payload.get("pctAdvancing"),
            "breadth_raw": payload.get("breadthRaw"),
            "price_strength_score": payload.get("priceStrengthScore"),
            "breadth_score": payload.get("breadthScore"),
            "source": payload.get("source") or "clickhouse.daily_qfq",
        }
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.msi_ma_count_daily (
                        trade_date, total_eligible, above_ma20, above_ma60, above_both,
                        pct_ma20, pct_ma60, pct_both, up_5d_count, up_5d_pct,
                        new_low_60d_count, new_low_60d_pct, new_high_252d_count, new_high_252d_pct,
                        advancing_count, advancing_pct, breadth_raw, price_strength_score, breadth_score,
                        source, ingested_at, updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :total_eligible, :above_ma20, :above_ma60, :above_both,
                        :pct_ma20, :pct_ma60, :pct_both, :up_5d_count, :up_5d_pct,
                        :new_low_60d_count, :new_low_60d_pct, :new_high_252d_count, :new_high_252d_pct,
                        :advancing_count, :advancing_pct, :breadth_raw, :price_strength_score, :breadth_score,
                        :source, now(), now(), NULL
                    )
                    ON CONFLICT (trade_date) DO UPDATE SET
                        total_eligible = EXCLUDED.total_eligible,
                        above_ma20 = EXCLUDED.above_ma20,
                        above_ma60 = EXCLUDED.above_ma60,
                        above_both = EXCLUDED.above_both,
                        pct_ma20 = EXCLUDED.pct_ma20,
                        pct_ma60 = EXCLUDED.pct_ma60,
                        pct_both = EXCLUDED.pct_both,
                        up_5d_count = EXCLUDED.up_5d_count,
                        up_5d_pct = EXCLUDED.up_5d_pct,
                        new_low_60d_count = EXCLUDED.new_low_60d_count,
                        new_low_60d_pct = EXCLUDED.new_low_60d_pct,
                        new_high_252d_count = EXCLUDED.new_high_252d_count,
                        new_high_252d_pct = EXCLUDED.new_high_252d_pct,
                        advancing_count = EXCLUDED.advancing_count,
                        advancing_pct = EXCLUDED.advancing_pct,
                        breadth_raw = EXCLUDED.breadth_raw,
                        price_strength_score = EXCLUDED.price_strength_score,
                        breadth_score = EXCLUDED.breadth_score,
                        source = EXCLUDED.source,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                params,
            )
        return 1

    def get_ma_count(self, trade_date: date) -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT trade_date, total_eligible, above_ma20, above_ma60, above_both,
                           pct_ma20, pct_ma60, pct_both, up_5d_count, up_5d_pct,
                           new_low_60d_count, new_low_60d_pct, new_high_252d_count, new_high_252d_pct,
                           advancing_count, advancing_pct, breadth_raw, price_strength_score, breadth_score, source
                      FROM {self.client.schema}.msi_ma_count_daily
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                     LIMIT 1
                    """
                ),
                {"trade_date": trade_date},
            ).first()
        if not row:
            return None
        return {
            "tradeDate": row[0].isoformat(),
            "totalEligible": int(row[1] or 0),
            "aboveMa20": int(row[2] or 0),
            "aboveMa60": int(row[3] or 0),
            "aboveBoth": int(row[4] or 0),
            "pctAboveMa20": _float_or_none(row[5]),
            "pctAboveMa60": _float_or_none(row[6]),
            "pctAboveBoth": _float_or_none(row[7]),
            "up5dCount": int(row[8] or 0),
            "pctUp5d": _float_or_none(row[9]),
            "newLow60dCount": int(row[10] or 0),
            "pctNewLow60d": _float_or_none(row[11]),
            "newHigh252dCount": int(row[12] or 0),
            "pctNewHigh252d": _float_or_none(row[13]),
            "advancingCount": int(row[14] or 0),
            "pctAdvancing": _float_or_none(row[15]),
            "breadthRaw": _float_or_none(row[16]),
            "priceStrengthScore": _float_or_none(row[17]),
            "breadthScore": _float_or_none(row[18]),
            "source": row[19],
            "fromCache": True,
        }

    def save_profit_effect(self, payload: dict[str, Any]) -> int:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.msi_profit_effect_daily (
                        trade_date, up_5d_pct, new_low_60d_pct, score, source, ingested_at, updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :up_5d_pct, :new_low_60d_pct, :score, :source, now(), now(), NULL
                    )
                    ON CONFLICT (trade_date) DO UPDATE SET
                        up_5d_pct = EXCLUDED.up_5d_pct,
                        new_low_60d_pct = EXCLUDED.new_low_60d_pct,
                        score = EXCLUDED.score,
                        source = EXCLUDED.source,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "trade_date": date.fromisoformat(str(payload["tradeDate"])),
                    "up_5d_pct": payload.get("up5dPct"),
                    "new_low_60d_pct": payload.get("newLow60dPct"),
                    "score": payload.get("score"),
                    "source": payload.get("source") or "postgres.msi_ma_count_daily",
                },
            )
        return 1

    def save_turnover_activity(self, payload: dict[str, Any]) -> int:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.msi_turnover_activity_daily (
                        trade_date, total_amount, sh_amount, sz_amount, avg_20d_amount,
                        ratio, score, sample_count, source, ingested_at, updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :total_amount, :sh_amount, :sz_amount, :avg_20d_amount,
                        :ratio, :score, :sample_count, :source, now(), now(), NULL
                    )
                    ON CONFLICT (trade_date) DO UPDATE SET
                        total_amount = EXCLUDED.total_amount,
                        sh_amount = EXCLUDED.sh_amount,
                        sz_amount = EXCLUDED.sz_amount,
                        avg_20d_amount = EXCLUDED.avg_20d_amount,
                        ratio = EXCLUDED.ratio,
                        score = EXCLUDED.score,
                        sample_count = EXCLUDED.sample_count,
                        source = EXCLUDED.source,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "trade_date": date.fromisoformat(str(payload["tradeDate"])),
                    "total_amount": payload.get("totalAmount"),
                    "sh_amount": payload.get("shAmount"),
                    "sz_amount": payload.get("szAmount"),
                    "avg_20d_amount": payload.get("avg20dAmount"),
                    "ratio": payload.get("ratio"),
                    "score": payload.get("score"),
                    "sample_count": int(payload.get("sampleCount") or 0),
                    "source": payload.get("source") or "clickhouse.daily_raw.999999+399001",
                },
            )
        return 1

    def save_volatility_sentiment(self, payload: dict[str, Any]) -> int:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.msi_volatility_daily (
                        trade_date, underlying_code, underlying_name, close, daily_return_pct,
                        realized_vol_20d, vol_window_days, vol_lookback_days, percentile_1y,
                        sentiment_score, sample_count, source, ingested_at, updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :underlying_code, :underlying_name, :close, :daily_return_pct,
                        :realized_vol_20d, :vol_window_days, :vol_lookback_days, :percentile_1y,
                        :sentiment_score, :sample_count, :source, now(), now(), NULL
                    )
                    ON CONFLICT (trade_date) DO UPDATE SET
                        underlying_code = EXCLUDED.underlying_code,
                        underlying_name = EXCLUDED.underlying_name,
                        close = EXCLUDED.close,
                        daily_return_pct = EXCLUDED.daily_return_pct,
                        realized_vol_20d = EXCLUDED.realized_vol_20d,
                        vol_window_days = EXCLUDED.vol_window_days,
                        vol_lookback_days = EXCLUDED.vol_lookback_days,
                        percentile_1y = EXCLUDED.percentile_1y,
                        sentiment_score = EXCLUDED.sentiment_score,
                        sample_count = EXCLUDED.sample_count,
                        source = EXCLUDED.source,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "trade_date": date.fromisoformat(str(payload["tradeDate"])),
                    "underlying_code": payload.get("underlyingCode"),
                    "underlying_name": payload.get("underlyingName"),
                    "close": payload.get("close"),
                    "daily_return_pct": payload.get("dailyReturnPct"),
                    "realized_vol_20d": payload.get("realizedVol20d"),
                    "vol_window_days": int(payload.get("volWindowDays") or 20),
                    "vol_lookback_days": int(payload.get("volLookbackDays") or 252),
                    "percentile_1y": payload.get("percentile1y"),
                    "sentiment_score": payload.get("sentimentScore"),
                    "sample_count": int(payload.get("sampleCount") or 0),
                    "source": payload.get("source") or "clickhouse.index_daily_raw",
                },
            )
        return 1

    def save_risk_appetite(self, payload: dict[str, Any]) -> int:
        hs = payload.get("hs300") or {}
        treasury = payload.get("treasury") or {}
        t1 = treasury.get("511010") or {}
        t2 = treasury.get("511090") or {}
        spread = payload.get("spread") or {}
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.msi_risk_appetite_daily (
                        trade_date, hs300_close, hs300_base_close, hs300_base_date, hs300_20d_return,
                        treasury_511010_close, treasury_511010_base_close, treasury_511010_20d_return,
                        treasury_511090_close, treasury_511090_base_close, treasury_511090_20d_return,
                        treasury_weighted_20d_return, spread_511010, spread_511090, spread_weighted,
                        score, window_days, source, ingested_at, updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :hs300_close, :hs300_base_close, :hs300_base_date, :hs300_20d_return,
                        :t1_close, :t1_base_close, :t1_return, :t2_close, :t2_base_close, :t2_return,
                        :treasury_weighted_return, :spread_511010, :spread_511090, :spread_weighted,
                        :score, :window_days, :source, now(), now(), NULL
                    )
                    ON CONFLICT (trade_date) DO UPDATE SET
                        hs300_close = EXCLUDED.hs300_close,
                        hs300_base_close = EXCLUDED.hs300_base_close,
                        hs300_base_date = EXCLUDED.hs300_base_date,
                        hs300_20d_return = EXCLUDED.hs300_20d_return,
                        treasury_511010_close = EXCLUDED.treasury_511010_close,
                        treasury_511010_base_close = EXCLUDED.treasury_511010_base_close,
                        treasury_511010_20d_return = EXCLUDED.treasury_511010_20d_return,
                        treasury_511090_close = EXCLUDED.treasury_511090_close,
                        treasury_511090_base_close = EXCLUDED.treasury_511090_base_close,
                        treasury_511090_20d_return = EXCLUDED.treasury_511090_20d_return,
                        treasury_weighted_20d_return = EXCLUDED.treasury_weighted_20d_return,
                        spread_511010 = EXCLUDED.spread_511010,
                        spread_511090 = EXCLUDED.spread_511090,
                        spread_weighted = EXCLUDED.spread_weighted,
                        score = EXCLUDED.score,
                        window_days = EXCLUDED.window_days,
                        source = EXCLUDED.source,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "trade_date": date.fromisoformat(str(payload["tradeDate"])),
                    "hs300_close": hs.get("close"),
                    "hs300_base_close": hs.get("baseClose"),
                    "hs300_base_date": _date_or_none(hs.get("baseDate")),
                    "hs300_20d_return": hs.get("returnPct"),
                    "t1_close": t1.get("close"),
                    "t1_base_close": t1.get("baseClose"),
                    "t1_return": t1.get("returnPct"),
                    "t2_close": t2.get("close"),
                    "t2_base_close": t2.get("baseClose"),
                    "t2_return": t2.get("returnPct"),
                    "treasury_weighted_return": (treasury.get("weighted") or {}).get("returnPct"),
                    "spread_511010": spread.get("511010"),
                    "spread_511090": spread.get("511090"),
                    "spread_weighted": spread.get("weighted"),
                    "score": payload.get("score"),
                    "window_days": int(payload.get("windowDays") or 20),
                    "source": payload.get("source") or "clickhouse.daily_qfq",
                },
            )
        return 1

    def save_style_risk(self, payload: dict[str, Any]) -> int:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.msi_style_risk_daily (
                        trade_date, window_days, hs300_return, csi1000_return,
                        spread, score, source, ingested_at, updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :window_days, :hs300_return, :csi1000_return,
                        :spread, :score, :source, now(), now(), NULL
                    )
                    ON CONFLICT (trade_date) DO UPDATE SET
                        window_days = EXCLUDED.window_days,
                        hs300_return = EXCLUDED.hs300_return,
                        csi1000_return = EXCLUDED.csi1000_return,
                        spread = EXCLUDED.spread,
                        score = EXCLUDED.score,
                        source = EXCLUDED.source,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "trade_date": date.fromisoformat(str(payload["tradeDate"])),
                    "window_days": int(payload.get("windowDays") or 5),
                    "hs300_return": (payload.get("hs300") or {}).get("returnPct"),
                    "csi1000_return": (payload.get("csi1000") or {}).get("returnPct"),
                    "spread": payload.get("spread"),
                    "score": payload.get("score"),
                    "source": payload.get("source") or "clickhouse.daily_qfq",
                },
            )
        return 1

    def save_limit_emotion(self, payload: dict[str, Any]) -> int:
        components = payload.get("components") or {}
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.msi_limit_emotion_daily (
                        trade_date, limit_up_count, limit_down_count, touched_count, broken_count,
                        break_board_rate, limit_up_down_ratio, yesterday_limit_up_count,
                        yesterday_limit_up_avg_return, up_down_score, break_board_score,
                        yesterday_return_score, composite_score, level, source,
                        ingested_at, updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :limit_up_count, :limit_down_count, :touched_count, :broken_count,
                        :break_board_rate, :limit_up_down_ratio, :yesterday_limit_up_count,
                        :yesterday_limit_up_avg_return, :up_down_score, :break_board_score,
                        :yesterday_return_score, :composite_score, :level, :source,
                        now(), now(), NULL
                    )
                    ON CONFLICT (trade_date) DO UPDATE SET
                        limit_up_count = EXCLUDED.limit_up_count,
                        limit_down_count = EXCLUDED.limit_down_count,
                        touched_count = EXCLUDED.touched_count,
                        broken_count = EXCLUDED.broken_count,
                        break_board_rate = EXCLUDED.break_board_rate,
                        limit_up_down_ratio = EXCLUDED.limit_up_down_ratio,
                        yesterday_limit_up_count = EXCLUDED.yesterday_limit_up_count,
                        yesterday_limit_up_avg_return = EXCLUDED.yesterday_limit_up_avg_return,
                        up_down_score = EXCLUDED.up_down_score,
                        break_board_score = EXCLUDED.break_board_score,
                        yesterday_return_score = EXCLUDED.yesterday_return_score,
                        composite_score = EXCLUDED.composite_score,
                        level = EXCLUDED.level,
                        source = EXCLUDED.source,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "trade_date": date.fromisoformat(str(payload["tradeDate"])),
                    "limit_up_count": int(payload.get("limitUpCount") or 0),
                    "limit_down_count": int(payload.get("limitDownCount") or 0),
                    "touched_count": int(payload.get("touchedCount") or 0),
                    "broken_count": int(payload.get("brokenCount") or 0),
                    "break_board_rate": payload.get("breakBoardRate"),
                    "limit_up_down_ratio": payload.get("limitUpDownRatio"),
                    "yesterday_limit_up_count": int(payload.get("yesterdayLimitUpCount") or 0),
                    "yesterday_limit_up_avg_return": payload.get("yesterdayLimitUpAvgReturn"),
                    "up_down_score": components.get("upDownScore"),
                    "break_board_score": components.get("breakBoardScore"),
                    "yesterday_return_score": components.get("yesterdayReturnScore"),
                    "composite_score": payload.get("compositeScore"),
                    "level": payload.get("level") or "normal",
                    "source": payload.get("source") or "clickhouse.daily_raw",
                },
            )
        return 1

    def _fetch_optional_column(self, table: str, column: str, trade_date: date) -> float | None:
        try:
            with self.client.session() as db:
                row = db.execute(
                    text(
                        f"""
                        SELECT {column}
                          FROM {self.client.schema}.{table}
                         WHERE trade_date = :trade_date AND deleted_at IS NULL
                         LIMIT 1
                        """
                    ),
                    {"trade_date": trade_date},
                ).first()
        except Exception:
            return None
        return float(row[0]) if row and row[0] is not None else None


_COMPONENT_SOURCES = (
    ("vol", "msi_volatility_daily", "sentiment_score", 1.0),
    ("turnover", "msi_turnover_activity_daily", "score", 1.0),
    ("price_strength", "msi_ma_count_daily", "price_strength_score", 1.0),
    ("risk_appetite", "msi_risk_appetite_daily", "score", 1.0),
    ("breadth", "msi_ma_count_daily", "breadth_score", 1.0),
    ("limit_emotion", "msi_limit_emotion_daily", "composite_score", 1.0),
    ("profit_effect", "msi_profit_effect_daily", "score", 1.0),
    ("sector_breadth", "msi_sector_breadth_daily", "advance_pct", 100.0),
    ("style_risk", "msi_style_risk_daily", "score", 1.0),
)


def _row_to_payload(row: Any) -> dict[str, Any]:
    return {
        "tradeDate": row[0].isoformat(),
        "components": {
            "vol": _float_or_none(row[1]),
            "turnover": _float_or_none(row[2]),
            "price_strength": _float_or_none(row[3]),
            "risk_appetite": _float_or_none(row[4]),
            "breadth": _float_or_none(row[5]),
            "limit_emotion": _float_or_none(row[6]),
            "profit_effect": _float_or_none(row[7]),
            "sector_breadth": _float_or_none(row[8]),
            "style_risk": _float_or_none(row[9]),
        },
        "compositeScore": _float_or_none(row[10]),
        "componentCount": int(row[11] or 0),
        "level": str(row[12] or "normal"),
        "source": str(row[13] or "composite"),
        "fromCache": True,
    }


def _float_or_none(value: Any) -> float | None:
    return float(value) if value is not None else None


def _date_or_none(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])
