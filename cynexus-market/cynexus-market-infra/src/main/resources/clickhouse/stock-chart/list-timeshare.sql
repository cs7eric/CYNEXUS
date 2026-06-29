select
    formatDateTime(trade_time, '%%Y-%%m-%%dT%%H:%%i:%%S') as time,
    toUnixTimestamp(trade_time) * 1000 as timestamp,
    toString(trade_date) as tradeDate,
    formatDateTime(trade_time, '%%H:%%i') as timeLabel,
    close as price,
    open,
    if(
        sum(volume) over (order by trade_time asc rows between unbounded preceding and current row) = 0,
        null,
        sum(amount) over (order by trade_time asc rows between unbounded preceding and current row)
        / sum(volume) over (order by trade_time asc rows between unbounded preceding and current row)
    ) as avgPrice,
    volume,
    amount as turnover
  from ch_dwd_kline_1m FINAL
 where market = '%s'
   and symbol = '%s'
   and period = 1
   and trade_date = toDate('%s')
 order by trade_time asc
FORMAT JSONEachRow
