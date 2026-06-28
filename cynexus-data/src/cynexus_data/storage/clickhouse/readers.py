from __future__ import annotations

from datetime import date, datetime, time, timedelta, timezone
import re
from typing import Any, Sequence

from cynexus_data.storage.clickhouse.client import ClickHouseClient

DEFAULT_KLINE_TABLE = "ch_dwd_kline_1m"
_TABLE_NAME_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)?$")
_BEIJING_TZ = timezone(timedelta(hours=8))
_TRADE_TS = time(9, 30)


def read_daily_kline(
    client: ClickHouseClient,
    code: str,
    adjust: str = "qfq",
    start: date | None = None,
    end: date | None = None,
    limit: int | None = None,
) -> list[dict[str, Any]]:
    table = {"none": "daily_raw", "qfq": "daily_qfq", "hfq": "daily_hfq"}.get(adjust)
    if table is None:
        raise ValueError("adjust must be one of: none, qfq, hfq")
    where = ["code = %(code)s"]
    params: dict[str, Any] = {"code": code}
    if start is not None:
        where.append("trade_date >= %(start)s")
        params["start"] = start
    if end is not None:
        where.append("trade_date <= %(end)s")
        params["end"] = end
    sql = f"SELECT trade_date, open, high, low, close, volume, amount FROM {table} WHERE {' AND '.join(where)} ORDER BY trade_date ASC"
    if limit is not None:
        sql += f" LIMIT {int(limit)}"
    return [
        {
            "trade_date": row[0].isoformat() if hasattr(row[0], "isoformat") else str(row[0]),
            "open": float(row[1]),
            "high": float(row[2]),
            "low": float(row[3]),
            "close": float(row[4]),
            "volume": int(row[5]),
            "amount": float(row[6]),
        }
        for row in client.query_rows(sql, params)
    ]


def read_daily_kline_bars(
    client: ClickHouseClient,
    code: str,
    adjust: str = "qfq",
    start: date | str | None = None,
    end: date | str | None = None,
    limit: int | None = None,
) -> list[dict[str, Any]]:
    table = {"none": "daily_raw", "qfq": "daily_qfq", "hfq": "daily_hfq"}.get(adjust)
    if table is None:
        raise ValueError("adjust must be one of: none, qfq, hfq")
    has_adj = table != "daily_raw"
    cols = "trade_date, open, high, low, close, volume, amount, adj_factor" if has_adj else "trade_date, open, high, low, close, volume, amount"
    where = ["code = %(code)s"]
    params: dict[str, Any] = {"code": code}
    if start is not None:
        where.append("trade_date >= %(start)s")
        params["start"] = _to_date(start)
    if end is not None:
        where.append("trade_date <= %(end)s")
        params["end"] = _to_date(end)
    where_sql = " AND ".join(where)
    if limit is not None:
        sql = f"SELECT {cols} FROM (SELECT {cols} FROM {table} WHERE {where_sql} ORDER BY trade_date DESC LIMIT %(limit)s) ORDER BY trade_date ASC"
        params["limit"] = int(limit)
    else:
        sql = f"SELECT {cols} FROM {table} WHERE {where_sql} ORDER BY trade_date ASC"
    return [_daily_row_to_bar(row, has_adj_factor=has_adj) for row in client.query_rows(sql, params)]


def get_latest_daily_trade_date(client: ClickHouseClient, code: str, table: str = "daily_raw") -> date | None:
    row = client.query_one(f"SELECT max(trade_date) FROM {_safe_table_name(table)} WHERE code = %(code)s", {"code": code})
    value = row[0] if row else None
    if value is None:
        return None
    return value.date() if hasattr(value, "date") and not isinstance(value, date) else value


def has_adjusted_daily(client: ClickHouseClient, code: str, adjust: str = "qfq") -> bool:
    table = {"qfq": "daily_qfq", "hfq": "daily_hfq"}.get(adjust)
    if table is None:
        raise ValueError("adjust must be one of: qfq, hfq")
    row = client.query_one(f"SELECT count() > 0 FROM {table} WHERE code = %(code)s LIMIT 1", {"code": code})
    return bool(row[0]) if row else False


def list_codes_with_adjusted_daily(client: ClickHouseClient, adjust: str = "qfq") -> list[str]:
    table = {"qfq": "daily_qfq", "hfq": "daily_hfq"}.get(adjust)
    if table is None:
        raise ValueError("adjust must be one of: qfq, hfq")
    return [str(row[0]) for row in client.query_rows(f"SELECT DISTINCT code FROM {table} ORDER BY code")]


def read_index_daily_bars(
    client: ClickHouseClient,
    code: str,
    *,
    as_of_date: date | str | None = None,
    days: int = 30,
    table: str = "index_daily_raw",
) -> list[dict[str, Any]]:
    limit = max(1, min(int(days), 1500))
    where = ["code = %(code)s"]
    params: dict[str, Any] = {"code": code, "limit": limit}
    if as_of_date is not None:
        where.append("trade_date <= %(as_of_date)s")
        params["as_of_date"] = _to_date(as_of_date)
    sql = f"""
        SELECT code, trade_date, anyLast(open), anyLast(high), anyLast(low), anyLast(close),
               anyLast(volume), anyLast(amount)
          FROM {_safe_table_name(table)}
         WHERE {' AND '.join(where)}
         GROUP BY code, trade_date
         ORDER BY trade_date DESC
         LIMIT %(limit)s
    """
    rows = client.query_rows(sql, params)
    return [
        {
            "code": str(row[0]),
            "trade_date": row[1].isoformat() if hasattr(row[1], "isoformat") else str(row[1]),
            "open": _float_or_none(row[2]),
            "high": _float_or_none(row[3]),
            "low": _float_or_none(row[4]),
            "close": _float_or_none(row[5]),
            "volume": int(row[6] or 0),
            "amount": _float_or_none(row[7]),
        }
        for row in reversed(rows)
    ]


def latest_index_trade_date(
    client: ClickHouseClient,
    code: str = "sh000001",
    *,
    as_of_date: date | str | None = None,
    table: str = "index_daily_raw",
) -> date | None:
    where = ["code = %(code)s"]
    params: dict[str, Any] = {"code": code}
    if as_of_date is not None:
        where.append("trade_date <= %(as_of_date)s")
        params["as_of_date"] = _to_date(as_of_date)
    row = client.query_one(f"SELECT max(trade_date) FROM {_safe_table_name(table)} WHERE {' AND '.join(where)}", params)
    value = row[0] if row else None
    if value is None:
        return None
    return value.date() if hasattr(value, "date") and not isinstance(value, date) else value


def has_index_kline(
    client: ClickHouseClient,
    code: str,
    trade_date: date | str,
    *,
    table: str = "index_daily_raw",
) -> bool:
    row = client.query_one(
        f"SELECT count() > 0 FROM {_safe_table_name(table)} WHERE code = %(code)s AND trade_date = %(trade_date)s",
        {"code": code, "trade_date": _to_date(trade_date)},
    )
    return bool(row[0]) if row else False


def read_daily_bars_by_code(
    client: ClickHouseClient,
    code: str,
    trade_date: date | str,
    *,
    table: str = "daily_raw",
    lookback_days: int = 380,
) -> list[dict[str, Any]]:
    td = date.fromisoformat(str(trade_date)[:10]) if not isinstance(trade_date, date) else trade_date
    start = td - timedelta(days=int(lookback_days) * 2)
    sql = f"""
        SELECT trade_date, open, high, low, close, volume, amount
          FROM {_safe_table_name(table)}
         WHERE code = %(code)s AND trade_date >= %(start)s AND trade_date <= %(trade_date)s
         ORDER BY trade_date DESC
         LIMIT %(limit)s
    """
    rows = client.query_rows(sql, {"code": code, "start": start, "trade_date": td, "limit": int(lookback_days)})
    return [
        {
            "trade_date": row[0].isoformat() if hasattr(row[0], "isoformat") else str(row[0]),
            "open": _float_or_none(row[1]),
            "high": _float_or_none(row[2]),
            "low": _float_or_none(row[3]),
            "close": _float_or_none(row[4]),
            "volume": int(row[5] or 0),
            "amount": _float_or_none(row[6]),
        }
        for row in reversed(rows)
    ]


def read_ma_count_input_rows(client: ClickHouseClient, trade_date: date | str, table: str = "daily_qfq") -> list[dict[str, Any]]:
    sql = f"""
        SELECT code, close, ma20, ma60, close_1d_ago, close_5d_ago, close_59d_ago, min_60d, close_251d_ago, max_252d
        FROM (
            SELECT
                code,
                trade_date,
                close,
                avg(close) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 19 PRECEDING AND CURRENT ROW) AS ma20,
                avg(close) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 59 PRECEDING AND CURRENT ROW) AS ma60,
                lagInFrame(close, 1) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) AS close_1d_ago,
                lagInFrame(close, 5) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) AS close_5d_ago,
                lagInFrame(close, 59) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) AS close_59d_ago,
                min(close) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 59 PRECEDING AND CURRENT ROW) AS min_60d,
                lagInFrame(close, 251) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) AS close_251d_ago,
                max(close) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) AS max_252d
            FROM {_safe_table_name(table)}
            WHERE trade_date <= %(trade_date)s
        )
        WHERE trade_date = %(trade_date)s
    """
    return [
        {
            "code": row[0],
            "close": _float_or_none(row[1]),
            "ma20": _float_or_none(row[2]),
            "ma60": _float_or_none(row[3]),
            "close_1d_ago": _float_or_none(row[4]),
            "close_5d_ago": _float_or_none(row[5]),
            "close_59d_ago": _float_or_none(row[6]),
            "min_60d": _float_or_none(row[7]),
            "close_251d_ago": _float_or_none(row[8]),
            "max_252d": _float_or_none(row[9]),
        }
        for row in client.query_rows(sql, {"trade_date": trade_date})
    ]


def read_close_series(client: ClickHouseClient, code: str, trade_date: date | str, table: str = "daily_qfq", limit: int = 260) -> list[dict[str, Any]]:
    sql = f"""
        SELECT trade_date, close
          FROM {_safe_table_name(table)}
         WHERE code = %(code)s AND trade_date <= %(trade_date)s
         ORDER BY trade_date DESC
         LIMIT %(limit)s
    """
    rows = client.query_rows(sql, {"code": code, "trade_date": trade_date, "limit": int(limit)})
    return [{"trade_date": row[0].isoformat() if hasattr(row[0], "isoformat") else str(row[0]), "close": _float_or_none(row[1])} for row in reversed(rows)]


def read_turnover_activity_rows(client: ClickHouseClient, trade_date: date | str, table: str = "daily_raw", limit_days: int = 40) -> list[dict[str, Any]]:
    sql = f"""
        SELECT trade_date,
               sumIf(amount, code = '999999') / 100000000.0 AS sh_amount,
               sumIf(amount, code = '399001') / 100000000.0 AS sz_amount,
               (sumIf(amount, code = '999999') + sumIf(amount, code = '399001')) / 100000000.0 AS total_amount
          FROM {_safe_table_name(table)}
         WHERE trade_date <= %(trade_date)s AND code IN ('999999', '399001')
         GROUP BY trade_date
        HAVING countDistinct(code) = 2
         ORDER BY trade_date DESC
         LIMIT %(limit)s
    """
    rows = client.query_rows(sql, {"trade_date": trade_date, "limit": int(limit_days)})
    return [
        {
            "trade_date": row[0].isoformat() if hasattr(row[0], "isoformat") else str(row[0]),
            "sh_amount": _float_or_none(row[1]),
            "sz_amount": _float_or_none(row[2]),
            "total_amount": _float_or_none(row[3]),
        }
        for row in reversed(rows)
    ]


def read_limit_emotion_snapshot_rows(client: ClickHouseClient, trade_date: date | str, table: str = "daily_raw") -> list[dict[str, Any]]:
    sql = f"""
        SELECT code, open, high, low, close, pre_close,
               CASE WHEN pre_close > 0 THEN (close - pre_close) / pre_close * 100 ELSE NULL END AS change_pct,
               CASE WHEN pre_close > 0 AND close >= pre_close * (1 + threshold) * 0.9999 THEN 1 ELSE 0 END AS is_limit_up,
               CASE WHEN pre_close > 0 AND close <= pre_close * (1 - threshold) * 1.0001 THEN 1 ELSE 0 END AS is_limit_down,
               CASE WHEN pre_close > 0 AND high >= pre_close * (1 + threshold) * 0.9995 THEN 1 ELSE 0 END AS is_touched,
               CASE WHEN pre_close > 0 AND high >= pre_close * (1 + threshold) * 0.9995
                         AND close < pre_close * (1 + threshold) * 0.9999 THEN 1 ELSE 0 END AS is_broken
          FROM (
            SELECT code, trade_date, open, high, low, close,
                   lagInFrame(close, 1) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS pre_close,
                   multiIf(startsWith(code, '300') OR startsWith(code, '301') OR startsWith(code, '688'), 0.20,
                           startsWith(code, '8') OR startsWith(code, '4') OR startsWith(code, '9'), 0.30,
                           0.10) AS threshold
              FROM {_safe_table_name(table)}
             WHERE trade_date <= %(trade_date)s
          )
         WHERE trade_date = %(trade_date)s
    """
    return [
        {
            "code": str(row[0]),
            "open": _float_or_none(row[1]),
            "high": _float_or_none(row[2]),
            "low": _float_or_none(row[3]),
            "close": _float_or_none(row[4]),
            "preClose": _float_or_none(row[5]),
            "changePct": _float_or_none(row[6]),
            "isLimitUp": bool(row[7]),
            "isLimitDown": bool(row[8]),
            "isTouchedLimitUp": bool(row[9]),
            "isBrokenLimitUp": bool(row[10]),
        }
        for row in client.query_rows(sql, {"trade_date": trade_date})
    ]


def read_limit_emotion_ohlcv_rows(client: ClickHouseClient, trade_date: date | str, table: str = "daily_raw") -> list[dict[str, Any]]:
    sql = f"""
        SELECT code, trade_date, open, high, low, close, pre_close
          FROM (
            SELECT code, trade_date, open, high, low, close,
                   lagInFrame(close, 1) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS pre_close
              FROM {_safe_table_name(table)}
             WHERE trade_date <= %(trade_date)s
          )
         WHERE trade_date = %(trade_date)s
    """
    return [
        {
            "code": str(row[0]),
            "trade_date": row[1].isoformat() if hasattr(row[1], "isoformat") else str(row[1]),
            "open": _float_or_none(row[2]),
            "high": _float_or_none(row[3]),
            "low": _float_or_none(row[4]),
            "close": _float_or_none(row[5]),
            "preClose": _float_or_none(row[6]),
        }
        for row in client.query_rows(sql, {"trade_date": trade_date})
    ]


def read_limit_emotion_history_ohlcv_rows(
    client: ClickHouseClient,
    trade_date: date | str,
    *,
    codes: Sequence[str],
    lookback_days: int = 120,
    table: str = "daily_raw",
) -> list[dict[str, Any]]:
    if not codes:
        return []
    td = date.fromisoformat(str(trade_date)[:10]) if not isinstance(trade_date, date) else trade_date
    start = td - timedelta(days=int(lookback_days))
    sql = f"""
        SELECT code, trade_date, close, pre_close
          FROM (
            SELECT code, trade_date, close,
                   lagInFrame(close, 1) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS pre_close
              FROM {_safe_table_name(table)}
             WHERE trade_date >= %(start)s AND trade_date < %(trade_date)s AND code IN %(codes)s
          )
         ORDER BY code, trade_date
    """
    return [
        {
            "code": str(row[0]),
            "trade_date": row[1].isoformat() if hasattr(row[1], "isoformat") else str(row[1]),
            "close": _float_or_none(row[2]),
            "preClose": _float_or_none(row[3]),
        }
        for row in client.query_rows(sql, {"start": start, "trade_date": td, "codes": list(codes)})
    ]


def count_kline_rows(
    client: ClickHouseClient,
    table: str = DEFAULT_KLINE_TABLE,
    period: int = 1,
    start_date: date | str | None = None,
    end_date: date | str | None = None,
    symbols: Sequence[str] | None = None,
) -> int:
    where, params = _kline_where(period=period, start_date=start_date, end_date=end_date, symbols=symbols)
    sql = f"SELECT count() FROM {_safe_table_name(table)} FINAL WHERE {' AND '.join(where)}"
    row = client.query_one(sql, params)
    return int(row[0]) if row else 0


def count_aggregate_kline_bars(
    client: ClickHouseClient,
    interval_minutes: int,
    source_table: str = DEFAULT_KLINE_TABLE,
    start_date: date | str | None = None,
    end_date: date | str | None = None,
    symbols: Sequence[str] | None = None,
) -> int:
    interval = _validate_interval(interval_minutes)
    where, params = _kline_where(period=1, start_date=start_date, end_date=end_date, symbols=symbols)
    sql = f"""
        SELECT count()
        FROM (
            SELECT
                market,
                symbol,
                toStartOfInterval(trade_time, INTERVAL {interval} minute) AS bucket_time
            FROM {_safe_table_name(source_table)} FINAL
            WHERE {' AND '.join(where)}
            GROUP BY market, symbol, bucket_time
        )
    """
    row = client.query_one(sql, params)
    return int(row[0]) if row else 0


def count_invalid_ohlcv_rows(
    client: ClickHouseClient,
    table: str = DEFAULT_KLINE_TABLE,
    period: int = 1,
    start_date: date | str | None = None,
    end_date: date | str | None = None,
    symbols: Sequence[str] | None = None,
) -> int:
    where, params = _kline_where(period=period, start_date=start_date, end_date=end_date, symbols=symbols)
    where.append(
        "("
        "open <= 0 OR high <= 0 OR low <= 0 OR close <= 0 "
        "OR high < greatest(open, low, close) "
        "OR low > least(open, high, close) "
        "OR volume < 0 OR amount < 0"
        ")"
    )
    sql = f"SELECT count() FROM {_safe_table_name(table)} FINAL WHERE {' AND '.join(where)}"
    row = client.query_one(sql, params)
    return int(row[0]) if row else 0


def aggregate_kline_period_to_clickhouse(
    client: ClickHouseClient,
    interval_minutes: int,
    source_table: str = DEFAULT_KLINE_TABLE,
    target_table: str = DEFAULT_KLINE_TABLE,
    start_date: date | str | None = None,
    end_date: date | str | None = None,
    symbols: Sequence[str] | None = None,
) -> int:
    expected_rows = count_aggregate_kline_bars(
        client,
        interval_minutes=interval_minutes,
        source_table=source_table,
        start_date=start_date,
        end_date=end_date,
        symbols=symbols,
    )
    if expected_rows == 0:
        return 0

    interval = _validate_interval(interval_minutes)
    where, params = _kline_where(period=1, start_date=start_date, end_date=end_date, symbols=symbols)
    sql = f"""
        INSERT INTO {_safe_table_name(target_table)}
            (
                market,
                symbol,
                tdx_code,
                period,
                trade_time,
                trade_date,
                open,
                high,
                low,
                close,
                amount,
                volume,
                adj_factor,
                source,
                source_file,
                source_file_mtime,
                ingested_at
            )
        SELECT
            market,
            symbol,
            any(tdx_code) AS tdx_code,
            {interval} AS period,
            bucket_time AS trade_time,
            toDate(bucket_time) AS trade_date,
            argMin(open, trade_time) AS open,
            max(high) AS high,
            min(low) AS low,
            argMax(close, trade_time) AS close,
            sum(amount) AS amount,
            sum(volume) AS volume,
            any(adj_factor) AS adj_factor,
            'aggregate_1m' AS source,
            '' AS source_file,
            toDateTime64(0, 3) AS source_file_mtime,
            now64(3) AS ingested_at
        FROM (
            SELECT
                *,
                toStartOfInterval(trade_time, INTERVAL {interval} minute) AS bucket_time
            FROM {_safe_table_name(source_table)} FINAL
            WHERE {' AND '.join(where)}
        )
        GROUP BY market, symbol, bucket_time
    """
    client.command(sql, params)
    return expected_rows


def _kline_where(
    period: int,
    start_date: date | str | None = None,
    end_date: date | str | None = None,
    symbols: Sequence[str] | None = None,
) -> tuple[list[str], dict[str, Any]]:
    where = ["period = %(period)s"]
    params: dict[str, Any] = {"period": int(period)}
    if start_date is not None:
        where.append("trade_date >= %(start_date)s")
        params["start_date"] = start_date
    if end_date is not None:
        where.append("trade_date <= %(end_date)s")
        params["end_date"] = end_date
    if symbols:
        where.append("symbol IN %(symbols)s")
        params["symbols"] = list(symbols)
    return where, params


def _validate_interval(interval_minutes: int) -> int:
    interval = int(interval_minutes)
    if interval <= 1:
        raise ValueError("aggregate interval must be greater than 1 minute")
    if interval > 240:
        raise ValueError("aggregate interval must be <= 240 minutes")
    return interval


def _safe_table_name(table: str) -> str:
    if not _TABLE_NAME_RE.match(table):
        raise ValueError(f"unsafe ClickHouse table name: {table}")
    return table


def _float_or_none(value: Any) -> float | None:
    return float(value) if value is not None else None


def _to_date(value: date | str) -> date:
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def _epoch_ms(value: date) -> int:
    return int(datetime.combine(value, _TRADE_TS, tzinfo=_BEIJING_TZ).timestamp() * 1000)


def _daily_row_to_bar(row: tuple, *, has_adj_factor: bool) -> dict[str, Any]:
    trade_date = row[0]
    if hasattr(trade_date, "date") and not isinstance(trade_date, date):
        trade_date = trade_date.date()
    out: dict[str, Any] = {
        "timestamp": _epoch_ms(trade_date),
        "trade_date": trade_date.isoformat(),
        "open": float(row[1]),
        "high": float(row[2]),
        "low": float(row[3]),
        "close": float(row[4]),
        "volume": int(row[5]),
        "amount": float(row[6]),
    }
    if has_adj_factor:
        out["adj_factor"] = float(row[7]) if row[7] is not None else 1.0
    return out
