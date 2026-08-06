-- Apply the configured commission schedule to imported trades whose fee was not
-- visible in the source screenshot and was stored as zero.
UPDATE cynexus_appl_market.portfolio_trade
SET total_fee = CASE
    WHEN quantity * price < 10000 THEN
        CASE WHEN instrument_type = 'ETF' THEN 0.1 ELSE 0.5 END
    ELSE round(quantity * price * 0.854 / 10000, 8)
END
WHERE total_fee = 0;
