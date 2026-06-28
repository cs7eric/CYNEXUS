select close
  from index_daily_raw
 where code = '%s'
   and trade_date < toDate('%s')
 order by trade_date desc
 limit 1
FORMAT JSONEachRow
