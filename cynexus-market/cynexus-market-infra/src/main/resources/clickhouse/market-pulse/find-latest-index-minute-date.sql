select max(trade_date) as latestTradeDate
  from ch_dwd_kline_1m FINAL
 where market = '%s'
   and symbol = '%s'
   and period = 1
   and trade_date <= toDate('%s')
FORMAT JSONEachRow
