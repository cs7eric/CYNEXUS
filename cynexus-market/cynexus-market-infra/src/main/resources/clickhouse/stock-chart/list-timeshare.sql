select
    formatDateTime(trade_time, '%%Y-%%m-%%dT%%H:%%i:%%S') as time,
    toUnixTimestamp(trade_time) * 1000 as timestamp,
    toString(trade_date) as tradeDate,
    formatDateTime(trade_time, '%%H:%%i') as timeLabel,
    toFloat64(price) as price,
    toFloat64(price) as open,
    toFloat64(avg_price) as avgPrice,
    toFloat64(volume_lots) as volume,
    toFloat64(turnover) as turnover
  from ch_ods_trade_review_timeshare_1m FINAL
 where market = '%s'
   and symbol = '%s'
   and trade_date = toDate('%s')
 order by slot asc
FORMAT JSONEachRow
