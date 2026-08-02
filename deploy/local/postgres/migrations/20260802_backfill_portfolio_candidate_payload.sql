\set ON_ERROR_STOP on

UPDATE cynexus_appl_market.portfolio_import_candidate
   SET normalized_payload = jsonb_build_object(
       'symbol', COALESCE(
           CASE WHEN jsonb_typeof(raw_payload->'symbol') = 'object'
                THEN raw_payload #>> '{symbol,value}' ELSE raw_payload->>'symbol' END,
           CASE WHEN jsonb_typeof(raw_payload->'securityCode') = 'object'
                THEN raw_payload #>> '{securityCode,value}' ELSE raw_payload->>'securityCode' END),
       'instrumentType', COALESCE(
           CASE WHEN jsonb_typeof(raw_payload->'instrumentType') = 'object'
                THEN raw_payload #>> '{instrumentType,value}' ELSE raw_payload->>'instrumentType' END,
           'STOCK'),
       'side', CASE WHEN jsonb_typeof(raw_payload->'side') = 'object'
                    THEN raw_payload #>> '{side,value}' ELSE raw_payload->>'side' END,
       'tradedAt', COALESCE(
           CASE WHEN jsonb_typeof(raw_payload->'tradedAt') = 'object'
                THEN raw_payload #>> '{tradedAt,value}' ELSE raw_payload->>'tradedAt' END,
           CASE WHEN jsonb_typeof(raw_payload->'executionAt') = 'object'
                THEN raw_payload #>> '{executionAt,value}' ELSE raw_payload->>'executionAt' END,
           CASE
             WHEN COALESCE(
                 CASE WHEN jsonb_typeof(raw_payload->'tradeDate') = 'object'
                      THEN raw_payload #>> '{tradeDate,value}' ELSE raw_payload->>'tradeDate' END,
                 CASE WHEN jsonb_typeof(raw_payload->'executionDate') = 'object'
                      THEN raw_payload #>> '{executionDate,value}' ELSE raw_payload->>'executionDate' END
             ) IS NULL THEN NULL
             ELSE COALESCE(
                 CASE WHEN jsonb_typeof(raw_payload->'tradeDate') = 'object'
                      THEN raw_payload #>> '{tradeDate,value}' ELSE raw_payload->>'tradeDate' END,
                 CASE WHEN jsonb_typeof(raw_payload->'executionDate') = 'object'
                      THEN raw_payload #>> '{executionDate,value}' ELSE raw_payload->>'executionDate' END
             ) || CASE
                 WHEN COALESCE(
                     CASE WHEN jsonb_typeof(raw_payload->'tradeTime') = 'object'
                          THEN raw_payload #>> '{tradeTime,value}' ELSE raw_payload->>'tradeTime' END,
                     CASE WHEN jsonb_typeof(raw_payload->'executionTime') = 'object'
                          THEN raw_payload #>> '{executionTime,value}' ELSE raw_payload->>'executionTime' END
                 ) IS NULL THEN ''
                 ELSE 'T' || COALESCE(
                     CASE WHEN jsonb_typeof(raw_payload->'tradeTime') = 'object'
                          THEN raw_payload #>> '{tradeTime,value}' ELSE raw_payload->>'tradeTime' END,
                     CASE WHEN jsonb_typeof(raw_payload->'executionTime') = 'object'
                          THEN raw_payload #>> '{executionTime,value}' ELSE raw_payload->>'executionTime' END
                 )
             END
           END),
       'quantity', COALESCE(
           CASE WHEN jsonb_typeof(raw_payload->'quantity') = 'object'
                THEN raw_payload #>> '{quantity,value}' ELSE raw_payload->>'quantity' END,
           CASE WHEN jsonb_typeof(raw_payload->'executionQuantity') = 'object'
                THEN raw_payload #>> '{executionQuantity,value}' ELSE raw_payload->>'executionQuantity' END),
       'price', COALESCE(
           CASE WHEN jsonb_typeof(raw_payload->'price') = 'object'
                THEN raw_payload #>> '{price,value}' ELSE raw_payload->>'price' END,
           CASE WHEN jsonb_typeof(raw_payload->'executionPrice') = 'object'
                THEN raw_payload #>> '{executionPrice,value}' ELSE raw_payload->>'executionPrice' END),
       'fee', CASE WHEN jsonb_typeof(raw_payload->'fee') = 'object'
                  THEN raw_payload #>> '{fee,value}' ELSE raw_payload->>'fee' END,
       'securityName', CASE WHEN jsonb_typeof(raw_payload->'securityName') = 'object'
                            THEN raw_payload #>> '{securityName,value}' ELSE raw_payload->>'securityName' END,
       'externalTradeNo', COALESCE(
           CASE WHEN jsonb_typeof(raw_payload->'externalTradeNo') = 'object'
                THEN raw_payload #>> '{externalTradeNo,value}' ELSE raw_payload->>'externalTradeNo' END,
           CASE WHEN jsonb_typeof(raw_payload->'tradeId') = 'object'
                THEN raw_payload #>> '{tradeId,value}' ELSE raw_payload->>'tradeId' END)
   )
 WHERE review_status = 'PENDING';

UPDATE cynexus_appl_market.portfolio_import_candidate candidate
   SET normalized_payload = candidate.normalized_payload || jsonb_build_object('instrumentType', 'ETF')
 WHERE candidate.review_status = 'PENDING'
   AND EXISTS (
       SELECT 1
         FROM cynexus_appl_market.market_etf_catalog etf
        WHERE etf.code = candidate.normalized_payload->>'symbol'
          AND etf.deleted_at IS NULL
          AND COALESCE(etf.is_active, true)
   );
