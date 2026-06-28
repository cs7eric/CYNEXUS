SELECT
    market,
    symbol,
    min(trade_time) AS min_trade_time,
    max(trade_time) AS max_trade_time,
    count() AS rows,
    uniqExact(trade_time) AS unique_minutes
FROM cynexus.ch_dwd_kline_1m FINAL
WHERE period = 1
GROUP BY
    market,
    symbol
ORDER BY
    market,
    symbol;
