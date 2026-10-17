from __future__ import annotations

from datetime import date, datetime, time, timedelta, timezone
import re
from typing import Any, Sequence

from cynexus_data.storage.clickhouse.client import ClickHouseClient

DEFAULT_KLINE_TABLE = "ch_dwd_kline_1m"
_TABLE_NAME_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)?$")
_BEIJING_TZ = timezone(timedelta(hours=8))
_TRADE_TS = time(9, 30)
_RAW_INDEX_TDX_CODE_BY_CODE = {
    "999999": "sh999999",
    "000300": "sh000300",
    "000852": "sh000852",
    "399001": "sz399001",
    "399006": "sz399006",
}


def read_event_minute_bars(
    client: ClickHouseClient,
    *,
    code: str,
    trade_date: date,
    start_at: datetime,
    end_at: datetime,
    table: str = DEFAULT_KLINE_TABLE,
) -> list[dict[str, Any]]:
    """Read a bounded, single-session 1m sequence for a research event."""
    if end_at < start_at:
        raise ValueError("end_at must not be before start_at")
    rows = client.query_rows(
        f"""
        SELECT trade_time, open, high, low, close, volume, amount
          FROM {_safe_table_name(table)} FINAL
         WHERE symbol = %(code)s
           AND trade_date = %(trade_date)s
           AND period = 1
           AND trade_time BETWEEN %(start_at)s AND %(end_at)s
         ORDER BY trade_time ASC
        """,
        {"code": code, "trade_date": trade_date, "start_at": start_at, "end_at": end_at},
    )
    return [
        {
            "minute_at": row[0], "open": _float_or_none(row[1]), "high": _float_or_none(row[2]),
            "low": _float_or_none(row[3]), "close": _float_or_none(row[4]),
            "volume": _float_or_none(row[5]), "amount": _float_or_none(row[6]),
        }
        for row in rows
    ]


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
    sql = f"""
        SELECT trade_date, open, high, low, close_price AS close, volume, amount
          FROM (
            SELECT trade_date,
                   argMax(open, close) AS open,
                   argMax(high, close) AS high,
                   argMax(low, close) AS low,
                   max(close) AS close_price,
                   argMax(volume, close) AS volume,
                   max(amount) AS amount
              FROM {table}
             WHERE {' AND '.join(where)}
             GROUP BY trade_date
          )
         ORDER BY trade_date ASC
    """
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
    grouped_cols = (
        "trade_date, anyIf(open, close = max_close) AS open, anyIf(high, close = max_close) AS high, anyIf(low, close = max_close) AS low, "
        "max_close AS close, anyIf(volume, close = max_close) AS volume, max(amount) AS amount, anyIf(adj_factor, close = max_close) AS adj_factor"
        if has_adj
        else "trade_date, anyIf(open, close = max_close) AS open, anyIf(high, close = max_close) AS high, anyIf(low, close = max_close) AS low, "
        "max_close AS close, anyIf(volume, close = max_close) AS volume, max(amount) AS amount"
    )
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
        sql = f"""
            SELECT {cols}
              FROM (
                SELECT {grouped_cols}
                  FROM (
                    SELECT *, max(close) OVER (PARTITION BY trade_date) AS max_close
                      FROM {table}
                     WHERE {where_sql}
                  )
                 GROUP BY trade_date, max_close
                 ORDER BY trade_date DESC
                 LIMIT %(limit)s
              )
             ORDER BY trade_date ASC
        """
        params["limit"] = int(limit)
    else:
        sql = f"""
            SELECT {cols}
              FROM (
                SELECT {grouped_cols}
                  FROM (
                    SELECT *, max(close) OVER (PARTITION BY trade_date) AS max_close
                      FROM {table}
                     WHERE {where_sql}
                  )
                 GROUP BY trade_date, max_close
              )
             ORDER BY trade_date ASC
        """
    return [_daily_row_to_bar(row, has_adj_factor=has_adj) for row in client.query_rows(sql, params)]


def get_latest_daily_trade_date(client: ClickHouseClient, code: str, table: str = "daily_raw") -> date | None:
    row = client.query_one(f"SELECT max(trade_date) FROM {_safe_table_name(table)} WHERE code = %(code)s", {"code": code})
    value = row[0] if row else None
    if value is None:
        return None
    return value.date() if hasattr(value, "date") and not isinstance(value, date) else value


def get_latest_table_trade_date(client: ClickHouseClient, table: str = "daily_raw") -> date | None:
    row = client.query_one(f"SELECT max(trade_date) FROM {_safe_table_name(table)}")
    value = row[0] if row else None
    if value is None:
        return None
    return value.date() if hasattr(value, "date") and not isinstance(value, date) else value


def read_stock_screening_fact_rows(
    client: ClickHouseClient,
    *,
    start_date: date | str,
    end_date: date | str,
    table: str = "daily_raw",
) -> list[dict[str, Any]]:
    """Read the A-share daily projection used to materialize Screener facts.

    The aggregation intentionally matches the serving Screener's raw-daily
    semantics: one maximum-value bar per market/symbol/trading date and a
    prior close from the immediately preceding available trading row.  This
    reader is a bounded storage read; it never fetches a market-data provider.
    """
    start = _to_date(start_date)
    end = _to_date(end_date)
    if start > end:
        raise ValueError("start_date must not be after end_date")
    sql = f"""
        WITH daily AS
        (
            SELECT
                market,
                symbol,
                trade_date,
                max(open) AS open_price,
                max(high) AS high_price,
                min(low) AS low_price,
                max(close) AS close_price,
                max(volume) AS volume,
                max(amount) AS amount
            FROM {_safe_table_name(table)}
            WHERE market IN ('SH', 'SZ', 'BJ')
              AND length(symbol) = 6
              AND (
                    (market = 'SH' AND startsWith(symbol, '600'))
                 OR (market = 'SH' AND startsWith(symbol, '601'))
                 OR (market = 'SH' AND startsWith(symbol, '603'))
                 OR (market = 'SH' AND startsWith(symbol, '605'))
                 OR (market = 'SH' AND startsWith(symbol, '688'))
                 OR (market = 'SH' AND startsWith(symbol, '689'))
                 OR (market = 'SZ' AND startsWith(symbol, '000'))
                 OR (market = 'SZ' AND startsWith(symbol, '001'))
                 OR (market = 'SZ' AND startsWith(symbol, '002'))
                 OR (market = 'SZ' AND startsWith(symbol, '003'))
                 OR (market = 'SZ' AND startsWith(symbol, '300'))
                 OR (market = 'SZ' AND startsWith(symbol, '301'))
                 OR (market = 'BJ' AND (startsWith(symbol, '4') OR startsWith(symbol, '8') OR startsWith(symbol, '9')))
              )
              AND trade_date <= %(end_date)s
              AND trade_date >= %(history_start_date)s
            GROUP BY market, symbol, trade_date
        ), with_previous AS
        (
            SELECT
                market,
                symbol,
                trade_date,
                open_price,
                high_price,
                low_price,
                close_price,
                volume,
                amount,
                lagInFrame(toNullable(close_price), 1, NULL)
                    OVER (PARTITION BY market, symbol ORDER BY trade_date
                          ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS prev_close
            FROM daily
        )
        SELECT
            market, symbol, trade_date, open_price, high_price, low_price,
            close_price, prev_close, volume, amount,
            if(prev_close = 0 OR isNull(prev_close), NULL,
               (close_price - prev_close) * 100 / prev_close) AS change_pct,
            if(prev_close = 0 OR isNull(prev_close), NULL,
               (high_price - low_price) * 100 / prev_close) AS amplitude_pct
        FROM with_previous
        WHERE trade_date >= %(history_start_date)s
        ORDER BY trade_date ASC, market ASC, symbol ASC
    """
    history_start = start - timedelta(days=400)
    rows = client.query_rows(sql, {"start_date": start, "end_date": end, "history_start_date": history_start})
    result: list[dict[str, Any]] = []
    history: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for row in rows:
        item = {
            "market": str(row[0]),
            "code": str(row[1]),
            "trade_date": row[2].isoformat() if hasattr(row[2], "isoformat") else str(row[2]),
            "open_price": _float_or_none(row[3]),
            "high_price": _float_or_none(row[4]),
            "low_price": _float_or_none(row[5]),
            "close_price": _float_or_none(row[6]),
            "prev_close": _float_or_none(row[7]),
            "volume": _float_or_none(row[8]),
            "amount": _float_or_none(row[9]),
            "change_pct": _float_or_none(row[10]),
            "amplitude_pct": _float_or_none(row[11]),
        }
        key = (item["market"], item["code"])
        previous = history.setdefault(key, [])
        closes = [value["close_price"] for value in previous if value["close_price"] is not None]
        volumes = [value["volume"] for value in previous if value["volume"] is not None]
        for window in (3, 5, 10, 20, 60, 120, 252):
            item[f"return_{window}d"] = ((item["close_price"] / closes[-window] - 1) * 100
                                          if item["close_price"] is not None and len(closes) >= window and closes[-window] else None)
        for window in (5, 10, 20, 60):
            item[f"ma{window}"] = (sum(closes[-(window - 1):] + [item["close_price"]]) / window
                                    if item["close_price"] is not None and len(closes) >= window - 1 else None)
            item[f"volume_vs_ma{window}"] = (item["volume"] / (sum(volumes[-(window - 1):]) / (window - 1))
                                              if item["volume"] is not None and len(volumes) >= window - 1 and sum(volumes[-(window - 1):]) else None)
        if item["trade_date"] >= start.isoformat():
            result.append(item)
        previous.append(item)
    return result


def count_stock_screening_fact_source_rows(
    client: ClickHouseClient,
    *,
    start_date: date | str,
    end_date: date | str,
    table: str = "daily_raw",
) -> int:
    """Count the deduplicated A-share source rows expected in the projection."""
    start = _to_date(start_date)
    end = _to_date(end_date)
    if start > end:
        raise ValueError("start_date must not be after end_date")
    sql = f"""
        SELECT count()
        FROM
        (
            SELECT market, symbol, trade_date
            FROM {_safe_table_name(table)}
            WHERE market IN ('SH', 'SZ', 'BJ')
              AND length(symbol) = 6
              AND (
                    (market = 'SH' AND startsWith(symbol, '600'))
                 OR (market = 'SH' AND startsWith(symbol, '601'))
                 OR (market = 'SH' AND startsWith(symbol, '603'))
                 OR (market = 'SH' AND startsWith(symbol, '605'))
                 OR (market = 'SH' AND startsWith(symbol, '688'))
                 OR (market = 'SH' AND startsWith(symbol, '689'))
                 OR (market = 'SZ' AND startsWith(symbol, '000'))
                 OR (market = 'SZ' AND startsWith(symbol, '001'))
                 OR (market = 'SZ' AND startsWith(symbol, '002'))
                 OR (market = 'SZ' AND startsWith(symbol, '003'))
                 OR (market = 'SZ' AND startsWith(symbol, '300'))
                 OR (market = 'SZ' AND startsWith(symbol, '301'))
                 OR (market = 'BJ' AND (startsWith(symbol, '4') OR startsWith(symbol, '8') OR startsWith(symbol, '9')))
              )
              AND trade_date BETWEEN %(start_date)s AND %(end_date)s
            GROUP BY market, symbol, trade_date
        )
    """
    row = client.query_one(sql, {"start_date": start, "end_date": end})
    return int(row[0]) if row else 0


def get_latest_effective_kline_trade_date(
    client: ClickHouseClient,
    *,
    expected_symbols: int,
    source: str = "tdx_local",
    period: int = 1,
    table: str = DEFAULT_KLINE_TABLE,
) -> date | None:
    """Find the newest date whose local-TDX coverage is effectively complete."""
    if expected_symbols <= 0:
        return None
    minimum_symbols = max(1, int(expected_symbols * 0.95 + 0.999999))
    row = client.query_one(
        f"""
        SELECT max(trade_date)
          FROM (
                SELECT trade_date
                  FROM {_safe_table_name(table)}
                 WHERE source = %(source)s
                   AND period = %(period)s
                 GROUP BY trade_date
                HAVING uniqExact(tdx_code) >= %(minimum_symbols)s
               )
        """,
        {"source": source, "period": period, "minimum_symbols": minimum_symbols},
    )
    value = row[0] if row else None
    if value is None:
        return None
    return value.date() if hasattr(value, "date") and not isinstance(value, date) else value


def get_previous_table_trade_date(client: ClickHouseClient, trade_date: date | str, table: str = "daily_raw") -> date | None:
    row = client.query_one(
        f"SELECT max(trade_date) FROM {_safe_table_name(table)} WHERE trade_date < %(trade_date)s",
        {"trade_date": _to_date(trade_date)},
    )
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
    storage_code = _index_storage_code(code, table)
    where = ["code = %(code)s"]
    params: dict[str, Any] = {"code": storage_code, "limit": limit}
    if as_of_date is not None:
        where.append("trade_date <= %(as_of_date)s")
        params["as_of_date"] = _to_date(as_of_date)
    sql = f"""
        SELECT code, trade_date, open, high, low, close_price, volume, amount
          FROM (
            SELECT code,
                   trade_date,
                   argMax(open, close) AS open,
                   argMax(high, close) AS high,
                   argMax(low, close) AS low,
                   max(close) AS close_price,
                   argMax(volume, close) AS volume,
                   max(amount) AS amount
              FROM {_safe_table_name(table)}
             WHERE {' AND '.join(where)}
             GROUP BY code, trade_date
             ORDER BY trade_date DESC
             LIMIT %(limit)s
          )
    """
    rows = client.query_rows(sql, params)
    price_scale = 10.0 if table == "daily_raw" else 1.0
    return [
        {
            "code": str(row[0]),
            "trade_date": row[1].isoformat() if hasattr(row[1], "isoformat") else str(row[1]),
            "open": _scaled_float_or_none(row[2], price_scale),
            "high": _scaled_float_or_none(row[3], price_scale),
            "low": _scaled_float_or_none(row[4], price_scale),
            "close": _scaled_float_or_none(row[5], price_scale),
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
        SELECT trade_date, open, high, low, close_price AS close, volume, amount
          FROM (
            SELECT trade_date,
                   argMax(open, close) AS open,
                   argMax(high, close) AS high,
                   argMax(low, close) AS low,
                   max(close) AS close_price,
                   argMax(volume, close) AS volume,
                   max(amount) AS amount
              FROM {_safe_table_name(table)}
             WHERE code = %(code)s AND trade_date >= %(start)s AND trade_date <= %(trade_date)s
             GROUP BY trade_date
          )
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
    target_date = _to_date(trade_date)
    # The largest window is 252 trading days.  A calendar-day buffer keeps
    # enough rows for weekends and holidays without scanning the full history.
    history_start_date = target_date - timedelta(days=400)
    sql = f"""
        SELECT code, close, ma20, ma60, close_1d_ago, close_5d_ago, close_59d_ago, min_60d, close_251d_ago, max_252d
        FROM (
            SELECT
                code,
                trade_date,
                close_price AS close,
                avg(close) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 19 PRECEDING AND CURRENT ROW) AS ma20,
                avg(close) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 59 PRECEDING AND CURRENT ROW) AS ma60,
                lagInFrame(close, 1) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) AS close_1d_ago,
                lagInFrame(close, 5) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) AS close_5d_ago,
                lagInFrame(close, 59) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) AS close_59d_ago,
                min(close) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 59 PRECEDING AND CURRENT ROW) AS min_60d,
                lagInFrame(close, 251) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) AS close_251d_ago,
                max(close) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) AS max_252d
            FROM (
                SELECT code, trade_date, max(close) AS close_price
                 FROM {_safe_table_name(table)}
                 WHERE trade_date >= %(history_start_date)s
                   AND trade_date <= %(trade_date)s
                 GROUP BY code, trade_date
            )
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
        for row in client.query_rows(sql, {"trade_date": target_date, "history_start_date": history_start_date})
    ]


def read_close_series(client: ClickHouseClient, code: str, trade_date: date | str, table: str = "daily_qfq", limit: int = 260) -> list[dict[str, Any]]:
    identity_where, params = _daily_identity_filter(code, table)
    params.update({"trade_date": trade_date, "limit": int(limit)})
    sql = f"""
        SELECT trade_date, max(close) AS close_price
          FROM {_safe_table_name(table)}
         WHERE {identity_where} AND trade_date <= %(trade_date)s
         GROUP BY trade_date
         ORDER BY trade_date DESC
         LIMIT %(limit)s
    """
    rows = client.query_rows(sql, params)
    return [{"trade_date": row[0].isoformat() if hasattr(row[0], "isoformat") else str(row[0]), "close": _float_or_none(row[1])} for row in reversed(rows)]


def read_turnover_activity_rows(client: ClickHouseClient, trade_date: date | str, table: str = "daily_raw", limit_days: int = 40) -> list[dict[str, Any]]:
    has_raw_identity = _is_wide_daily_raw_table(table)
    sh_filter = "tdx_code = 'sh999999'" if has_raw_identity else "code = '999999'"
    sz_filter = "tdx_code = 'sz399001'" if has_raw_identity else "code = '399001'"
    source_filter = "tdx_code IN ('sh999999', 'sz399001')" if has_raw_identity else "code IN ('999999', '399001')"
    group_key = "tdx_code" if has_raw_identity else "code"
    sql = f"""
        SELECT trade_date,
               sumIf(amount, {sh_filter}) / 100000000.0 AS sh_amount,
               sumIf(amount, {sz_filter}) / 100000000.0 AS sz_amount,
               (sumIf(amount, {sh_filter}) + sumIf(amount, {sz_filter})) / 100000000.0 AS total_amount
          FROM (
            SELECT {group_key}, trade_date, max(amount) AS amount
              FROM {_safe_table_name(table)}
             WHERE trade_date <= %(trade_date)s AND {source_filter}
             GROUP BY {group_key}, trade_date
          )
         GROUP BY trade_date
        HAVING countDistinct({group_key}) = 2
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


def read_market_overview_fact_row(client: ClickHouseClient, trade_date: date | str, table: str = "daily_qfq") -> dict[str, Any] | None:
    target_date = _to_date(trade_date)
    previous_date = get_previous_table_trade_date(client, target_date, table)
    if previous_date is None:
        return None
    sql = f"""
        SELECT trade_date,
               sum(current_amount) / 100000000.0 AS total_amount,
               sum(current_volume) AS total_volume,
               count() AS stock_count,
               sum(current_close > previous_close) AS rising_count,
               sum(current_close < previous_close) AS falling_count,
               sum(current_close = previous_close) AS flat_count,
               sum((current_close - previous_close) / previous_close * 100 >= 9.5) AS limit_up_count,
               sum((current_close - previous_close) / previous_close * 100 <= -9.5) AS limit_down_count
          FROM (
            SELECT current.code,
                   current.trade_date,
                   current.close_price AS current_close,
                   current.amount_value AS current_amount,
                   current.volume_value AS current_volume,
                   previous.close_price AS previous_close
              FROM (
                SELECT code, trade_date, max(close) AS close_price,
                       max(amount) AS amount_value, argMax(volume, close) AS volume_value
                  FROM {_safe_table_name(table)}
                 WHERE trade_date = %(trade_date)s
                 GROUP BY code, trade_date
              ) AS current
              INNER JOIN (
                SELECT code, max(close) AS close_price
                  FROM {_safe_table_name(table)}
                 WHERE trade_date = %(previous_trade_date)s
                 GROUP BY code
              ) AS previous ON current.code = previous.code
             WHERE previous.close_price > 0
          )
         GROUP BY trade_date
    """
    row = client.query_one(sql, {"trade_date": target_date, "previous_trade_date": previous_date})
    if not row:
        return None
    return {
        "trade_date": row[0].isoformat() if hasattr(row[0], "isoformat") else str(row[0]),
        "totalAmount": round(float(row[1] or 0), 2),
        "totalVolume": float(row[2] or 0),
        "stockCount": int(row[3] or 0),
        "risingCount": int(row[4] or 0),
        "fallingCount": int(row[5] or 0),
        "flatCount": int(row[6] or 0),
        "limitUpCount": int(row[7] or 0),
        "limitDownCount": int(row[8] or 0),
    }


def read_daily_change_pct_by_codes(
    client: ClickHouseClient,
    codes: Sequence[str],
    trade_date: date | str,
    table: str = "daily_qfq",
) -> dict[str, dict[str, Any]]:
    normalized_codes = sorted({str(code)[-6:].zfill(6) for code in codes if str(code or "").strip()})
    if not normalized_codes:
        return {}
    sql = f"""
        SELECT code, close_price, pre_close,
               CASE WHEN pre_close > 0 THEN (close_price - pre_close) / pre_close * 100 ELSE NULL END AS change_pct
          FROM (
            SELECT code,
                   trade_date,
                   close_price,
                   lagInFrame(close_price, 1) OVER (
                       PARTITION BY code
                       ORDER BY trade_date
                       ROWS BETWEEN 1 PRECEDING AND CURRENT ROW
                   ) AS pre_close
              FROM (
                SELECT code, trade_date, max(close) AS close_price
                  FROM {_safe_table_name(table)}
                 WHERE code IN %(codes)s
                   AND trade_date <= %(trade_date)s
                 GROUP BY code, trade_date
              )
          )
         WHERE trade_date = %(trade_date)s
    """
    rows = client.query_rows(sql, {"codes": normalized_codes, "trade_date": _to_date(trade_date)})
    return {
        str(row[0]).zfill(6): {
            "code": str(row[0]).zfill(6),
            "close": _float_or_none(row[1]),
            "pre_close": _float_or_none(row[2]),
            "change_pct": _float_or_none(row[3]),
        }
        for row in rows
    }


def read_limit_emotion_snapshot_rows(client: ClickHouseClient, trade_date: date | str, table: str = "daily_raw") -> list[dict[str, Any]]:
    target_date = _to_date(trade_date)
    previous_date = get_previous_table_trade_date(client, target_date, table)
    if previous_date is None:
        return []
    sql = f"""
        SELECT current.code, current.open, current.high, current.low, current.close_price, previous.close_price AS pre_close,
               CASE WHEN previous.close_price > 0 THEN (current.close_price - previous.close_price) / previous.close_price * 100 ELSE NULL END AS change_pct,
               CASE WHEN previous.close_price > 0 AND current.close_price >= previous.close_price * (1 + threshold) * 0.9999 THEN 1 ELSE 0 END AS is_limit_up,
               CASE WHEN previous.close_price > 0 AND current.close_price <= previous.close_price * (1 - threshold) * 1.0001 THEN 1 ELSE 0 END AS is_limit_down,
               CASE WHEN previous.close_price > 0 AND current.high >= previous.close_price * (1 + threshold) * 0.9995 THEN 1 ELSE 0 END AS is_touched,
               CASE WHEN previous.close_price > 0 AND current.high >= previous.close_price * (1 + threshold) * 0.9995
                         AND current.close_price < previous.close_price * (1 + threshold) * 0.9999 THEN 1 ELSE 0 END AS is_broken
          FROM (
            SELECT code, argMax(open, close) AS open, argMax(high, close) AS high,
                   argMax(low, close) AS low, max(close) AS close_price,
                   multiIf(startsWith(code, '300') OR startsWith(code, '301') OR startsWith(code, '688'), 0.20,
                           startsWith(code, '8') OR startsWith(code, '4') OR startsWith(code, '9'), 0.30,
                           0.10) AS threshold
              FROM {_safe_table_name(table)}
             WHERE trade_date = %(trade_date)s
             GROUP BY code
          ) AS current
          INNER JOIN (
            SELECT code, max(close) AS close_price
              FROM {_safe_table_name(table)}
             WHERE trade_date = %(previous_trade_date)s
             GROUP BY code
          ) AS previous ON current.code = previous.code
         WHERE previous.close_price > 0
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
        for row in client.query_rows(sql, {"trade_date": target_date, "previous_trade_date": previous_date})
    ]


def read_limit_emotion_ohlcv_rows(client: ClickHouseClient, trade_date: date | str, table: str = "daily_raw") -> list[dict[str, Any]]:
    target_date = _to_date(trade_date)
    previous_date = get_previous_table_trade_date(client, target_date, table)
    if previous_date is None:
        return []
    sql = f"""
        SELECT current.market, current.code, current.tdx_code, current.trade_date,
               current.open, current.high, current.low, current.close_price, previous.close_price AS pre_close
          FROM (
            SELECT market, code, tdx_code, trade_date,
                   argMax(open, close) AS open, argMax(high, close) AS high,
                   argMax(low, close) AS low, max(close) AS close_price
              FROM {_safe_table_name(table)}
             WHERE trade_date = %(trade_date)s
             GROUP BY market, code, tdx_code, trade_date
          ) AS current
          INNER JOIN (
            SELECT tdx_code, max(close) AS close_price
              FROM {_safe_table_name(table)}
             WHERE trade_date = %(previous_trade_date)s
             GROUP BY tdx_code
          ) AS previous ON current.tdx_code = previous.tdx_code
         WHERE previous.close_price > 0
    """
    return [
        {
            "market": str(row[0]),
            "code": str(row[1]),
            "tdxCode": str(row[2]),
            "trade_date": row[3].isoformat() if hasattr(row[3], "isoformat") else str(row[3]),
            "open": _float_or_none(row[4]),
            "high": _float_or_none(row[5]),
            "low": _float_or_none(row[6]),
            "close": _float_or_none(row[7]),
            "preClose": _float_or_none(row[8]),
        }
        for row in client.query_rows(sql, {"trade_date": target_date, "previous_trade_date": previous_date})
    ]


def read_limit_emotion_history_ohlcv_rows(
    client: ClickHouseClient,
    trade_date: date | str,
    *,
    codes: Sequence[str],
    tdx_codes: Sequence[str] | None = None,
    lookback_days: int = 120,
    table: str = "daily_raw",
) -> list[dict[str, Any]]:
    if not codes and not tdx_codes:
        return []
    td = date.fromisoformat(str(trade_date)[:10]) if not isinstance(trade_date, date) else trade_date
    start = td - timedelta(days=int(lookback_days))
    if table == "daily_raw" and tdx_codes:
        identity_column = "tdx_code"
        identity_filter = "tdx_code IN %(identities)s"
        identity_select = "code, tdx_code"
        identity_group_by = "code, tdx_code, trade_date"
        identities = list(tdx_codes)
    else:
        identity_column = "code"
        identity_filter = "code IN %(identities)s"
        identity_select = "code"
        identity_group_by = "code, trade_date"
        identities = list(codes)
    sql = f"""
        SELECT code, trade_date, close, pre_close
          FROM (
              SELECT code, trade_date, close_price AS close,
                     lagInFrame(close_price, 1) OVER (PARTITION BY {identity_column} ORDER BY trade_date ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS pre_close
                FROM (
                    SELECT {identity_select},
                           trade_date,
                           raw_close AS close_price
                       FROM (
                        SELECT {identity_select},
                               trade_date,
                               max(close) AS raw_close
                           FROM {_safe_table_name(table)}
                          WHERE trade_date >= %(start)s AND trade_date < %(trade_date)s AND {identity_filter}
                          GROUP BY {identity_group_by}
                       )
                )
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
        for row in client.query_rows(sql, {"start": start, "trade_date": td, "identities": identities})
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


def count_duplicate_daily_keys(
    client: ClickHouseClient,
    table: str = "daily_raw",
    start_date: date | str | None = None,
    end_date: date | str | None = None,
) -> int:
    where, params = _daily_where(start_date=start_date, end_date=end_date)
    key_columns = "market, symbol, trade_date" if _is_wide_daily_raw_table(table) else "code, trade_date"
    sql = f"""
        SELECT count()
          FROM (
            SELECT {key_columns}, count() AS row_count
              FROM {_safe_table_name(table)}
             WHERE {' AND '.join(where)}
             GROUP BY {key_columns}
            HAVING row_count > 1
          )
    """
    row = client.query_one(sql, params)
    return int(row[0]) if row else 0


def count_scaled_duplicate_daily_keys(
    client: ClickHouseClient,
    table: str = "daily_raw",
    start_date: date | str | None = None,
    end_date: date | str | None = None,
    ratio_threshold: float = 100.0,
) -> int:
    where, params = _daily_where(start_date=start_date, end_date=end_date)
    params["ratio_threshold"] = float(ratio_threshold)
    key_columns = "market, symbol, trade_date" if _is_wide_daily_raw_table(table) else "code, trade_date"
    sql = f"""
        SELECT count()
          FROM (
            SELECT {key_columns}, count() AS row_count,
                   min(close) AS min_close,
                   max(close) AS max_close
              FROM {_safe_table_name(table)}
             WHERE {' AND '.join(where)}
             GROUP BY {key_columns}
            HAVING row_count > 1
               AND min_close > 0
               AND max_close / min_close > %(ratio_threshold)s
          )
    """
    row = client.query_one(sql, params)
    return int(row[0]) if row else 0


def sample_daily_rows_for_source_validation(
    client: ClickHouseClient,
    *,
    table: str = "daily_raw",
    trade_date: date | str,
    sample_every: int = 100,
    limit: int | None = None,
    markets: Sequence[str] | None = None,
    sample_scope: str = "all",
) -> list[dict[str, Any]]:
    every = max(1, int(sample_every))
    params: dict[str, Any] = {"trade_date": _to_date(trade_date), "sample_every": every}
    where = ["trade_date = %(trade_date)s"]
    if markets and _is_wide_daily_raw_table(table):
        params["markets"] = [str(market).strip().upper() for market in markets if str(market).strip()]
        if params["markets"]:
            where.append("market IN %(markets)s")
    if sample_scope == "stock" and _is_wide_daily_raw_table(table):
        where.append(
            "("
            "(market = 'SH' AND (startsWith(symbol, '600') OR startsWith(symbol, '601') OR startsWith(symbol, '603') OR startsWith(symbol, '605') OR startsWith(symbol, '688') OR startsWith(symbol, '689'))) "
            "OR (market = 'SZ' AND (startsWith(symbol, '000') OR startsWith(symbol, '001') OR startsWith(symbol, '002') OR startsWith(symbol, '003') OR startsWith(symbol, '300') OR startsWith(symbol, '301'))) "
            "OR (market = 'BJ' AND (startsWith(symbol, '4') OR startsWith(symbol, '8') OR startsWith(symbol, '9')))"
            ")"
        )
    limit_sql = ""
    if limit is not None:
        params["limit"] = max(1, int(limit))
        limit_sql = "LIMIT %(limit)s"
    if _is_wide_daily_raw_table(table):
        identity_select = "market, symbol, code, tdx_code"
        group_cols = "market, symbol, code, tdx_code"
        order_cols = "market, symbol"
    else:
        identity_select = "'' AS market, code AS symbol, code, code AS tdx_code"
        group_cols = "code"
        order_cols = "code"
    sql = f"""
        SELECT market, symbol, code, tdx_code, trade_date, open, high, low, close_price AS close, volume, amount
          FROM (
            SELECT *,
                   row_number() OVER (ORDER BY {order_cols}) AS rn
              FROM (
                SELECT {identity_select},
                   trade_date,
                   argMax(open, close) AS open,
                   argMax(high, close) AS high,
                   argMax(low, close) AS low,
                   max(close) AS close_price,
                   argMax(volume, close) AS volume,
                   max(amount) AS amount
                  FROM {_safe_table_name(table)}
                 WHERE {' AND '.join(where)}
                 GROUP BY {group_cols}, trade_date
              )
          )
         WHERE modulo(rn - 1, %(sample_every)s) = 0
         ORDER BY {order_cols}
         {limit_sql}
    """
    return [
        {
            "market": str(row[0]) if row[0] is not None else None,
            "symbol": str(row[1]),
            "code": str(row[2]),
            "tdx_code": str(row[3]),
            "trade_date": row[4].isoformat() if hasattr(row[4], "isoformat") else str(row[4]),
            "open": _float_or_none(row[5]),
            "high": _float_or_none(row[6]),
            "low": _float_or_none(row[7]),
            "close": _float_or_none(row[8]),
            "volume": int(row[9] or 0),
            "amount": _float_or_none(row[10]),
        }
        for row in client.query_rows(sql, params)
    ]


def aggregate_kline_period_to_clickhouse(
    client: ClickHouseClient,
    interval_minutes: int,
    source_table: str = DEFAULT_KLINE_TABLE,
    target_table: str = DEFAULT_KLINE_TABLE,
    start_date: date | str | None = None,
    end_date: date | str | None = None,
    symbols: Sequence[str] | None = None,
    *,
    allow_full_history: bool = False,
) -> int:
    if not allow_full_history and start_date is None and end_date is None and not symbols:
        raise ValueError(
            "aggregate kline requires trade_date/start_date/end_date or symbols; "
            "set allow_full_history=true only for an intentional maintenance backfill"
        )
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
    target_where, target_params = _kline_where(period=interval, start_date=start_date, end_date=end_date, symbols=symbols)
    target_where.append("source = %(target_source)s")
    target_params["target_source"] = "aggregate_1m"
    client.command(
        f"ALTER TABLE {_safe_table_name(target_table)} DELETE WHERE {' AND '.join(target_where)}",
        target_params,
        settings={"mutations_sync": 1},
    )
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
            argMax(close, trade_time) AS close_price,
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


def _daily_where(
    start_date: date | str | None = None,
    end_date: date | str | None = None,
) -> tuple[list[str], dict[str, Any]]:
    where = ["1 = 1"]
    params: dict[str, Any] = {}
    if start_date is not None:
        where.append("trade_date >= %(start_date)s")
        params["start_date"] = _to_date(start_date)
    if end_date is not None:
        where.append("trade_date <= %(end_date)s")
        params["end_date"] = _to_date(end_date)
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


def _daily_identity_filter(code: str, table: str) -> tuple[str, dict[str, Any]]:
    normalized = str(code).strip()
    if _is_wide_daily_raw_table(table):
        if normalized.lower().startswith(("sh", "sz", "bj")):
            return "tdx_code = %(tdx_code)s", {"tdx_code": normalized.lower()}
        if normalized in _RAW_INDEX_TDX_CODE_BY_CODE:
            return "tdx_code = %(tdx_code)s", {"tdx_code": _RAW_INDEX_TDX_CODE_BY_CODE[normalized]}
    return "code = %(code)s", {"code": normalized}


def _is_wide_daily_raw_table(table: str) -> bool:
    return table.split(".")[-1] in {"daily_raw", "daily_raw_rebuild"}


def _float_or_none(value: Any) -> float | None:
    return float(value) if value is not None else None


def _scaled_float_or_none(value: Any, scale: float) -> float | None:
    parsed = _float_or_none(value)
    return parsed * scale if parsed is not None else None


def _to_date(value: date | str) -> date:
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def _index_storage_code(code: str, table: str) -> str:
    value = str(code or "").strip().lower()
    if table != "daily_raw":
        return value
    short_code = value[2:] if value.startswith(("sh", "sz", "bj")) else value
    short_code = short_code[-6:].zfill(6)
    if value == "sh000001" or short_code == "999999":
        return "999999"
    return short_code


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
