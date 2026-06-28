select
    formatDateTime(trade_time, '%%Y-%%m-%%dT%%H:%%i:%%S') as time,
    toUnixTimestamp(trade_time) * 1000 as timestamp,
    open,
    high,
    low,
    close,
    volume,
    amount as turnover
  from ch_dwd_kline_1m FINAL
 where market = '%s'
   and symbol = '%s'
   and period = 1
   and trade_date = toDate('%s')
 order by trade_time asc
FORMAT JSONEachRow
