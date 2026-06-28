select
    toString(trade_date) as tradeDate,
    toInt32(count()) as stockCount,
    toInt32(sum(close > pre_close)) as risingCount,
    toInt32(sum(close < pre_close)) as fallingCount,
    toInt32(sum(close = pre_close)) as flatCount,
    toInt32(sum((close - pre_close) / pre_close * 100 >= 9.5)) as limitUpCount,
    toInt32(sum((close - pre_close) / pre_close * 100 <= -9.5)) as limitDownCount
  from (
    select
        trade_date,
        code,
        close,
        lagInFrame(close) over (
            partition by code
            order by trade_date
            rows between unbounded preceding and current row
        ) as pre_close
      from daily_qfq
     where trade_date between toDate('%s') - interval 30 day and toDate('%s')
  )
 where trade_date between toDate('%s') and toDate('%s')
   and pre_close > 0
 group by trade_date
 order by trade_date asc
FORMAT JSONEachRow
