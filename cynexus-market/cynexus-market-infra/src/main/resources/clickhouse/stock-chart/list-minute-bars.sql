select
    formatDateTime(bucket_time, '%%Y-%%m-%%dT%%H:%%i:%%S') as time,
    toUnixTimestamp(bucket_time) * 1000 as timestamp,
    toString(toDate(bucket_time)) as tradeDate,
    argMin(open, trade_time) as open,
    max(high) as high,
    min(low) as low,
    argMax(close, trade_time) as close,
    sum(volume) as volume,
    sum(amount) as turnover
  from
  (
    select
        trade_time,
        toStartOfInterval(trade_time, INTERVAL %d minute) as bucket_time,
        open,
        high,
        low,
        close,
        volume,
        amount
      from ch_dwd_kline_1m FINAL
     where market = '%s'
       and symbol = '%s'
       and period = 1
       and trade_date = toDate('%s')
  )
 group by bucket_time
 order by bucket_time asc
FORMAT JSONEachRow
