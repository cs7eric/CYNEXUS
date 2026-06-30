select
    toString(max(trade_date)) as latestTradeDate
  from ch_ods_stock_auction_point FINAL
 where market = '%s'
   and symbol = '%s'
   and trade_date <= toDate('%s')
FORMAT JSONEachRow
