select
    toUnixTimestamp(trade_time) * 1000 as timestamp,
    toString(trade_date) as tradeDate,
    formatDateTime(trade_time, '%%H:%%i:%%S') as timeLabel,
    phase,
    price,
    matched_volume as matchedVolume,
    matched_amount as matchedAmount,
    unmatched_buy_volume as unmatchedBuyVolume,
    unmatched_sell_volume as unmatchedSellVolume
  from ch_ods_stock_auction_point FINAL
 where market = '%s'
   and symbol = '%s'
   and trade_date = toDate('%s')
 order by trade_time asc, phase asc
FORMAT JSONEachRow
