package cn.cccs7.cynexus.ai.infra.agent.market;

final class MarketResearchAgentToolSchemas {

    static final String STOCK_RESEARCH = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["search","metadata","sectors","sector_stocks","fund_flow","intraday","auction","auction_history"],"description":"Stock research operation to run."},
              "keyword":{"type":"string","minLength":1,"maxLength":80,"description":"Search text for stock or index lookup."},
              "symbol":{"type":"string","minLength":1,"maxLength":30,"description":"Stock, index, or instrument identifier."},
              "targetType":{"type":"string","enum":["stock","index","sector","etf"],"description":"Instrument type for intraday data; defaults to stock."},
              "name":{"type":"string","maxLength":120,"description":"Optional instrument display name for intraday lookup."},
              "adjust":{"type":"string","enum":["none","qfq","hfq"],"description":"Price adjustment mode for intraday data; defaults to qfq."},
              "tradeDate":{"type":"string","format":"date","description":"Optional trading date in YYYY-MM-DD format."},
              "periods":{"type":"string","maxLength":64,"description":"Comma-separated intraday periods such as 1m,5m,15m."},
              "sectorId":{"type":"string","maxLength":80,"description":"Optional sector identifier for sector_stocks."},
              "categoryRaw":{"type":"integer","description":"Optional raw sector category for sector_stocks."},
              "topicId":{"type":"string","maxLength":80,"description":"Optional topic identifier for sector_stocks."},
              "sectorName":{"type":"string","maxLength":120,"description":"Optional sector name for sector_stocks."},
              "limit":{"type":"integer","minimum":1,"maximum":120,"description":"Maximum history rows for auction_history."}
            },"additionalProperties":false}
            """;

    static final String STOCK_F10 = """
            {"type":"object","required":["operation","symbol"],"properties":{
              "operation":{"type":"string","enum":["valuation","finance_report","business_composition","profit_forecast","announcements","news","roadshows","company_news"],"description":"F10 research section to read."},
              "symbol":{"type":"string","minLength":1,"maxLength":30,"description":"Required stock identifier."},
              "requestId":{"type":"string","maxLength":40,"description":"Optional provider request id for valuation; defaults to the standard valuation query."},
              "reportType":{"type":"string","maxLength":40,"description":"Optional financial report type such as zcfzb."},
              "reportDate":{"type":"string","format":"date","description":"Optional report date for business composition."},
              "section":{"type":"string","maxLength":40,"description":"Optional company-news section; defaults to gsyj."}
            },"additionalProperties":false}
            """;

    static final String SENTIMENT = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["ma_count","risk_appetite","limit_emotion","volatility","turnover","style_risk","profit_effect","sector_breadth","msi_index","ma_count_history","risk_appetite_history","limit_emotion_history","volatility_history","turnover_history","style_risk_history","profit_effect_history","sector_breadth_history","msi_index_history","index_daily"],"description":"Market sentiment metric or bounded history operation to read."},
              "date":{"type":"string","format":"date","description":"Optional observation date in YYYY-MM-DD format; omit for the latest available session."}
              ,"start":{"type":"string","format":"date","description":"Range start date for history or index_daily."},
              "end":{"type":"string","format":"date","description":"Range end date for history or index_daily."},
              "days":{"type":"integer","minimum":1,"maximum":120,"description":"Number of sessions for sector_breadth_history."},
              "code":{"type":"string","minLength":1,"maxLength":30,"description":"Index code for index_daily."}
            },"additionalProperties":false}
            """;

    static final String INDUSTRY = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["targets","target_codes","overview","kline","result","fund_flow","fund_flow_history","tdx_fund_flow"],"description":"Industry analysis operation to run."},
              "sortBy":{"type":"string","maxLength":40,"description":"Optional overview sort field."},
              "ascending":{"type":"boolean","description":"Whether overview sorting is ascending; defaults to false."},
              "count":{"type":"integer","minimum":1,"maximum":100,"description":"Maximum industry overview rows; defaults to 100."},
              "tradeDate":{"type":"string","format":"date","description":"Optional trading date in YYYY-MM-DD format."},
              "maxTradeDate":{"type":"string","format":"date","description":"Optional newest date for fund-flow history."},
              "days":{"type":"integer","minimum":1,"maximum":365,"description":"Number of history days; bounded to 365."},
              "top":{"type":"integer","minimum":1,"maximum":100,"description":"Optional number of top industries to return."},
              "targetType":{"type":"string","enum":["stock","index","sector","etf"],"description":"Instrument type for TDX fund flow."},
              "symbol":{"type":"string","minLength":1,"maxLength":30,"description":"Required target identifier for TDX fund flow."}
              ,"kind":{"type":"string","maxLength":40,"description":"Optional target-code category filter."},
              "period":{"type":"string","enum":["day","week","month"],"description":"Kline period; defaults to day."},
              "klineCount":{"type":"integer","minimum":1,"maximum":240,"description":"Maximum kline points for the kline operation."},
              "targetId":{"type":"string","minLength":1,"maxLength":80,"description":"Target result identifier for the result operation."}
            },"additionalProperties":false}
            """;

    static final String MARGIN = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["latest","summary_history","stocks","stock_history","industries","industry_history"],"description":"Margin-trading query operation to run."},
              "tradeDate":{"type":"string","format":"date","description":"Optional trading date in YYYY-MM-DD format."},
              "start":{"type":"string","format":"date","description":"Optional range start date for summary history."},
              "end":{"type":"string","format":"date","description":"Optional range end date for summary history."},
              "stockCode":{"type":"string","minLength":1,"maxLength":30,"description":"Required stock code for stock_history."},
              "industryCode":{"type":"string","minLength":1,"maxLength":40,"description":"Required industry code for industry_history or optional industry filter for stocks."},
              "exchange":{"type":"string","maxLength":20,"description":"Optional exchange filter for stocks."},
              "sort":{"type":"string","maxLength":40,"description":"Sort field; defaults to margin_balance."},
              "sortDir":{"type":"string","enum":["asc","desc"],"description":"Sort direction; defaults to desc."},
              "page":{"type":"integer","minimum":1,"maximum":100,"description":"Optional result page."},
              "pageSize":{"type":"integer","minimum":1,"maximum":100,"description":"Optional page size."},
              "top":{"type":"integer","minimum":1,"maximum":50,"description":"Maximum latest ranking rows; defaults to 20."},
              "limit":{"type":"integer","minimum":1,"maximum":120,"description":"Maximum stock rows or history rows."}
            },"additionalProperties":false}
            """;

    static final String SCREENER = """
            {"type":"object","properties":{
              "tradeDate":{"type":"string","format":"date","description":"Optional daily close date in YYYY-MM-DD format."},
              "universe":{"type":"string","enum":["ALL","SH","SZ","BJ"],"description":"Market universe; defaults to ALL."},
              "excludeSt":{"type":"boolean","description":"Exclude ST stocks; defaults to true."},
              "excludeChiNext":{"type":"boolean","description":"Exclude ChiNext stocks; defaults to true."},
              "excludeStar":{"type":"boolean","description":"Exclude STAR Market stocks; defaults to true."},
              "minPrice":{"type":"number","minimum":0,"description":"Optional inclusive minimum close price."},
              "maxPrice":{"type":"number","minimum":0,"description":"Optional inclusive maximum close price."},
              "minChangePct":{"type":"number","minimum":-100,"maximum":100,"description":"Optional inclusive minimum daily change percentage."},
              "maxChangePct":{"type":"number","minimum":-100,"maximum":100,"description":"Optional inclusive maximum daily change percentage."},
              "minAmount":{"type":"number","minimum":0,"description":"Optional inclusive minimum daily turnover amount in CNY."},
              "minMarketCap":{"type":"number","minimum":0,"description":"Optional inclusive minimum total market capitalization in CNY."},
              "maxMarketCap":{"type":"number","minimum":0,"description":"Optional inclusive maximum total market capitalization in CNY."},
              "industry":{"type":"string","minLength":1,"maxLength":80,"description":"Optional exact industry name filter."},
              "tdxIndustryCode":{"type":"string","minLength":1,"maxLength":32,"description":"Optional TDX industry-group code from THS-to-TDX mapped screener results."},
              "thsIndustryCode":{"type":"string","minLength":1,"maxLength":32,"description":"Optional THS industry membership code from a cited screener result."},
              "conceptSectorId":{"type":"string","minLength":1,"maxLength":64,"description":"Optional TDX concept sector ID from a cited screener result."},
              "styleSectorId":{"type":"string","minLength":1,"maxLength":64,"description":"Optional TDX style sector ID from a cited screener result."},
              "board":{"type":"string","enum":["main_sh","main_sz","chinext","star","bse","unknown"],"description":"Optional listing-board filter."},
              "minAmplitudePct":{"type":"number","description":"Optional inclusive minimum daily amplitude percentage."},
              "maxAmplitudePct":{"type":"number","description":"Optional inclusive maximum daily amplitude percentage."},
              "minReturnPct5d":{"type":"number","description":"Optional inclusive minimum five-session return percentage."},
              "maxReturnPct5d":{"type":"number","description":"Optional inclusive maximum five-session return percentage."},
              "minReturnPct10d":{"type":"number","description":"Optional inclusive minimum ten-session return percentage."},
              "maxReturnPct10d":{"type":"number","description":"Optional inclusive maximum ten-session return percentage."},
              "minReturnPct30d":{"type":"number","description":"Optional inclusive minimum thirty-session return percentage."},
              "maxReturnPct30d":{"type":"number","description":"Optional inclusive maximum thirty-session return percentage."},
              "minReturnPct60d":{"type":"number","description":"Optional inclusive minimum sixty-session return percentage."},
              "maxReturnPct60d":{"type":"number","description":"Optional inclusive maximum sixty-session return percentage."},
              "minReturnPct252d":{"type":"number","description":"Optional inclusive minimum 252-session return percentage."},
              "maxReturnPct252d":{"type":"number","description":"Optional inclusive maximum 252-session return percentage."},
              "minMainNet":{"type":"number","description":"Optional inclusive minimum daily main-fund net flow in CNY."},
              "minMainNet5d":{"type":"number","description":"Optional inclusive minimum five-session main-fund net flow in CNY."},
              "minTurnoverPct":{"type":"number","description":"Optional inclusive minimum daily turnover percentage."},
              "maxTurnoverPct":{"type":"number","description":"Optional inclusive maximum daily turnover percentage."},
              "minVolumeRatio":{"type":"number","description":"Optional inclusive minimum daily volume ratio."},
              "maxVolumeRatio":{"type":"number","description":"Optional inclusive maximum daily volume ratio."},
              "minPe":{"type":"number","description":"Optional inclusive minimum PE; partial coverage only."},
              "maxPe":{"type":"number","description":"Optional inclusive maximum PE; partial coverage only."},
              "volumeContractionDays":{"type":"integer","minimum":2,"maximum":10,"description":"Optional number of immediately preceding sessions whose volumes must decrease successively."},
              "minBreakoutVolumeMultiple":{"type":"number","minimum":1,"maximum":20,"description":"Optional minimum ratio of today's volume to the mean volume across the requested contracting sessions; requires volumeContractionDays."},
              "maxBreakoutVolumeMultiple":{"type":"number","minimum":1,"maximum":20,"description":"Optional inclusive maximum for the same contraction-relative volume multiple."},
              "minBullishBodyPct":{"type":"number","minimum":0,"maximum":30,"description":"Optional minimum positive real-body percentage: (close - open) / previous close * 100."},
              "minCloseAboveMa20Pct":{"type":"number","minimum":-100,"maximum":500,"description":"Optional minimum current-close percentage above MA20."},
              "minMa5AboveMa20Pct":{"type":"number","minimum":-100,"maximum":300,"description":"Optional minimum MA5 percentage above MA20."},
              "breakoutHighDays":{"type":"integer","minimum":5,"maximum":20,"description":"Optional prior-high breakout lookback; current close must meet or exceed every prior high."},
              "maxAtrPct20":{"type":"number","minimum":0.01,"maximum":50,"description":"Optional maximum 20-session ATR as a percentage of current close."},
              "minConsecutiveBullishDays":{"type":"integer","minimum":1,"maximum":10,"description":"Optional minimum consecutive sessions, including today, whose close exceeds open."},
              "minVolumeVsMa5Multiple":{"type":"number","minimum":0.1,"maximum":20,"description":"Optional minimum current volume divided by the preceding five-session mean volume."},
              "maxVolumeVsMa5Multiple":{"type":"number","minimum":0.1,"maximum":20,"description":"Optional inclusive maximum current volume divided by the preceding five-session mean volume."},
              "candlestickPattern":{"type":"string","enum":["bullish_engulfing","bearish_engulfing","hammer","shooting_star","doji","gap_up","gap_down","inside_bar"],"description":"Optional exact daily candlestick pattern, evaluated only on an aligned daily OHLCV fact."},
              "sort":{"type":"string","enum":["amount","change_pct","amplitude_pct","return_5d","return_10d","return_30d","return_60d","return_252d","main_net","main_net_5d","turnover_pct","volume_ratio","pe","market_cap","price","code"],"description":"Sort field; defaults to amount."},
              "sortDir":{"type":"string","enum":["asc","desc"],"description":"Sort direction; defaults to desc."},
              "page":{"type":"integer","minimum":1,"maximum":20,"description":"One-based result page; defaults to 1."},
              "pageSize":{"type":"integer","minimum":1,"maximum":50,"description":"Bounded result page size; defaults to 20."}
            },"additionalProperties":false}
            """;

    static final String SCREENER_OUTPUT = """
            {"type":"object","required":["ok","universe","excludeSt","sort","sortDir","page","pageSize","universeCount","count","source","items","warnings"],"properties":{
              "ok":{"type":"boolean"},"requestedTradeDate":{"type":["string","null"],"format":"date"},"tradeDate":{"type":["string","null"],"format":"date"},
              "universe":{"type":"string"},"excludeSt":{"type":"boolean"},"excludeChiNext":{"type":"boolean"},"excludeStar":{"type":"boolean"},"industry":{"type":["string","null"]},"tdxIndustryCode":{"type":["string","null"]},"thsIndustryCode":{"type":["string","null"]},"conceptSectorId":{"type":["string","null"]},"styleSectorId":{"type":["string","null"]},"sort":{"type":"string"},"sortDir":{"type":"string"},"page":{"type":"integer"},"pageSize":{"type":"integer"},"universeCount":{"type":"integer"},"count":{"type":"integer"},"source":{"type":"string"},"warnings":{"type":"array","items":{"type":"string"}},"error":{"type":["string","null"]},"dataQuality":{"type":["object","null"],"properties":{"status":{"type":"string","enum":["READY","PARTIAL","STALE"]},"snapshotDate":{"type":["string","null"],"format":"date"},"freshnessLagDays":{"type":"integer"},"eligibleStockCount":{"type":"integer"},"dailySnapshotCount":{"type":"integer"},"warnings":{"type":"array","items":{"type":"string"}},"fieldCoverage":{"type":"array","items":{"type":"object","properties":{"field":{"type":"string"},"availableCount":{"type":"integer"},"eligibleCount":{"type":"integer"},"coveragePct":{"type":"number"}},"additionalProperties":false}}},"additionalProperties":false},
              "items":{"type":"array","items":{"type":"object","required":["code","name","market","st"],"properties":{"code":{"type":"string"},"name":{"type":"string"},"market":{"type":"string"},"industry":{"type":["string","null"]},"board":{"type":["string","null"]},"tdxIndustryCode":{"type":["string","null"]},"tdxIndustryName":{"type":["string","null"]},"thsIndustryCode":{"type":["string","null"]},"thsIndustryName":{"type":["string","null"]},"concepts":{"type":"array","items":{"type":"object","properties":{"sectorId":{"type":"string"},"name":{"type":"string"}},"additionalProperties":false}},"styles":{"type":"array","items":{"type":"object","properties":{"sectorId":{"type":"string"},"name":{"type":"string"}},"additionalProperties":false}},"st":{"type":"boolean"},"price":{"type":["number","null"]},"changePct":{"type":["number","null"]},"amplitudePct":{"type":["number","null"]},"amount":{"type":["number","null"]},"totalMarketCap":{"type":["number","null"]},"floatMarketCap":{"type":["number","null"]},"returnPct5d":{"type":["number","null"]},"returnPct10d":{"type":["number","null"]},"returnPct30d":{"type":["number","null"]},"returnPct60d":{"type":["number","null"]},"returnPct252d":{"type":["number","null"]},"mainNet":{"type":["number","null"]},"mainNet5d":{"type":["number","null"]},"turnoverPct":{"type":["number","null"]},"volumeRatio":{"type":["number","null"]},"pe":{"type":["number","null"]},"priceVolumePattern":{"type":["object","null"],"properties":{"observedContractionDays":{"type":["integer","null"]},"breakoutVolumeMultiple":{"type":["number","null"]},"bullishBodyPct":{"type":["number","null"]},"closeAboveMa20Pct":{"type":["number","null"]},"ma5AboveMa20Pct":{"type":["number","null"]},"breakoutHighDays":{"type":["integer","null"]},"atrPct20":{"type":["number","null"]},"consecutiveBullishDays":{"type":["integer","null"]},"volumeVsMa5Multiple":{"type":["number","null"]},"candlestickPatterns":{"type":"array","items":{"type":"string"}}},"additionalProperties":false}},"additionalProperties":false}}
            },"additionalProperties":false}
            """;

    static final String FACTOR_OBSERVATORY = """
            {"type":"object","properties":{
              "tradeDate":{"type":"string","format":"date","description":"Observation date in YYYY-MM-DD; defaults to the latest available daily snapshot."},
              "cohort":{"type":"string","enum":["LIMIT_UP","LARGE_MOVE"],"description":"Daily event pool; defaults to LIMIT_UP."},
              "minLargeMovePct":{"type":"number","minimum":0,"maximum":30,"description":"Inclusive daily percent-change threshold for LARGE_MOVE; defaults to 7."},
              "limit":{"type":"integer","minimum":1,"maximum":100,"description":"Maximum event rows; defaults to 40."},
              "symbol":{"type":"string","minLength":1,"maxLength":30,"description":"Optional single stock code within the selected event cohort."}
            },"additionalProperties":false}
            """;

    static final String FACTOR_OBSERVATORY_OUTPUT = """
            {"type":"object","required":["ok","cohort","populationCount","sampledCount","marketFactors","events","screeningHintsDeprecated","screeningHints","warnings"],"properties":{
              "ok":{"type":"boolean"},"requestedTradeDate":{"type":["string","null"],"format":"date"},"tradeDate":{"type":["string","null"],"format":"date"},"cohort":{"type":"string","enum":["LIMIT_UP","LARGE_MOVE"]},"minLargeMovePct":{"type":["number","null"]},"populationCount":{"type":"integer","minimum":0},"sampledCount":{"type":"integer","minimum":0},"warnings":{"type":"array","items":{"type":"string"}},"error":{"type":["string","null"]},
              "screeningHintsDeprecated":{"type":"boolean"},
              "marketFactors":{"type":"array","items":{"type":"object","required":["stage","code","label","status","source","metrics","supportingEvidence","counterEvidence","warnings"],"properties":{"stage":{"type":"string"},"code":{"type":"string"},"label":{"type":"string"},"status":{"type":"string"},"asOfDate":{"type":["string","null"],"format":"date"},"source":{"type":"string"},"metrics":{"type":"array","items":{"type":"object","required":["key","label","value","unit"],"properties":{"key":{"type":"string"},"label":{"type":"string"},"value":{"type":"string"},"unit":{"type":"string"}},"additionalProperties":false}},"supportingEvidence":{"type":"array","items":{"type":"string"}},"counterEvidence":{"type":"array","items":{"type":"string"}},"warnings":{"type":"array","items":{"type":"string"}}},"additionalProperties":false}},
              "events":{"type":"array","items":{"type":"object","required":["code","name","eventType","changePct","streak","concepts","styles","factors"],"properties":{"code":{"type":"string"},"name":{"type":"string"},"eventType":{"type":"string"},"changePct":{"type":["number","null"]},"streak":{"type":["integer","null"]},"thsIndustryCode":{"type":["string","null"]},"thsIndustryName":{"type":["string","null"]},"tdxIndustryCode":{"type":["string","null"]},"tdxIndustryName":{"type":["string","null"]},"concepts":{"type":"array","items":{"type":"string"}},"styles":{"type":"array","items":{"type":"string"}},"factors":{"type":"array","items":{"type":"object","required":["stage","code","label","status","source","metrics","supportingEvidence","counterEvidence","warnings"],"properties":{"stage":{"type":"string"},"code":{"type":"string"},"label":{"type":"string"},"status":{"type":"string"},"asOfDate":{"type":["string","null"],"format":"date"},"source":{"type":"string"},"metrics":{"type":"array","items":{"type":"object","required":["key","label","value","unit"],"properties":{"key":{"type":"string"},"label":{"type":"string"},"value":{"type":"string"},"unit":{"type":"string"}},"additionalProperties":false}},"supportingEvidence":{"type":"array","items":{"type":"string"}},"counterEvidence":{"type":"array","items":{"type":"string"}},"warnings":{"type":"array","items":{"type":"string"}}},"additionalProperties":false}}},"additionalProperties":false}},
              "screeningHints":{"type":"array","items":{"type":"object","required":["criterion","operator","value","label","basis"],"properties":{"criterion":{"type":"string"},"operator":{"type":"string"},"value":{"type":"string"},"label":{"type":"string"},"basis":{"type":"string"}},"additionalProperties":false}}
            },"additionalProperties":false}
            """;

    static final String FACTOR_STUDY = """
            {"type":"object","properties":{"offset":{"type":"integer","minimum":0,"maximum":200},"limit":{"type":"integer","minimum":1,"maximum":50}},"additionalProperties":false}
            """;
    static final String FACTOR_STUDY_OUTPUT = """
            {"type":"array","items":{"type":"object","required":["studyId","status","eventType","startDate","endDate","dataCutoffAt","featureRegistryVersion","leakageCheckPassed","eventSampleCount","controlSampleCount","warnings"],"properties":{"studyId":{"type":"string"},"status":{"type":"string"},"eventType":{"type":"string"},"startDate":{"type":"string","format":"date"},"endDate":{"type":"string","format":"date"},"dataCutoffAt":{"type":"string"},"featureRegistryVersion":{"type":"string"},"leakageCheckPassed":{"type":"boolean"},"eventSampleCount":{"type":"integer"},"controlSampleCount":{"type":"integer"},"warnings":{"type":"array","items":{"type":"string"}}},"additionalProperties":false}}
            """;
    static final String EVENT_REPLAY = """
            {"type":"object","required":["code","tradeDate"],"properties":{"code":{"type":"string","minLength":6,"maxLength":12,"pattern":"^[0-9]{6}$"},"tradeDate":{"type":"string","format":"date"},"anchorAt":{"type":"string","format":"date-time"},"minuteLimit":{"type":"integer","minimum":30,"maximum":300}},"additionalProperties":false}
            """;
    static final String EVENT_REPLAY_OUTPUT = """
            {"type":"object","required":["code","tradeDate","status","source","warnings","stockMinuteBars"],"properties":{"code":{"type":"string"},"tradeDate":{"type":"string","format":"date"},"anchorAt":{"type":["string","null"]},"status":{"type":"string"},"source":{"type":"string"},"dataCutoffAt":{"type":["string","null"]},"warnings":{"type":"array","items":{"type":"string"}},"stockMinuteBars":{"type":"array","maxItems":300,"items":{"type":"object"}},"coverage":{"type":["object","null"]},"marketReplay":{"type":["object","null"]}},"additionalProperties":false}
            """;
    static final String FACTOR_SET_VALIDATION = """
            {"type":"object","required":["code"],"properties":{"code":{"type":"string","minLength":1,"maxLength":64,"pattern":"^[A-Za-z0-9_-]+$"}},"additionalProperties":false}
            """;
    static final String FACTOR_SET_VALIDATION_OUTPUT = """
            {"type":"object","required":["id","code","name","status","version"],"properties":{"id":{"type":"integer"},"code":{"type":"string"},"name":{"type":"string"},"status":{"type":"string"},"activeVersionId":{"type":["integer","null"]},"version":{"type":"integer"},"activeVersion":{"type":["object","null"]}},"additionalProperties":false}
            """;

    static final String STOCK_SELECTION_STRATEGY_HEALTH = """
            {"type":"object","required":["strategyCode"],"properties":{
              "strategyCode":{"type":"string","enum":["TREND_MOMENTUM","VOLUME_CAPITAL_RESONANCE","INDUSTRY_RELATIVE_STRENGTH"],"description":"Published governed strategy code to assess."},
              "tradeDate":{"type":"string","format":"date","description":"Optional requested daily-close date in YYYY-MM-DD format; the response reports the actual available date."}
            },"additionalProperties":false}
            """;

    static final String STOCK_SELECTION_STRATEGY_HEALTH_OUTPUT = """
            {"type":"object","required":["strategyCode","strategyVersionNo","status","reason","recommendedAction","universeCount","candidateCount","signalDensityPct","freshnessLagDays","source","checks","warnings"],"properties":{
              "strategyCode":{"type":"string"},"strategyVersionNo":{"type":"integer"},"requestedTradeDate":{"type":["string","null"],"format":"date"},"actualTradeDate":{"type":["string","null"],"format":"date"},"status":{"type":"string","enum":["VALID","WEAKENING","INVALID","NOT_ASSESSED"]},"reason":{"type":"string"},"recommendedAction":{"type":"string"},"universeCount":{"type":"integer"},"candidateCount":{"type":"integer"},"signalDensityPct":{"type":"number"},"freshnessLagDays":{"type":"integer"},"source":{"type":"string"},"warnings":{"type":"array","items":{"type":"string"}},
              "recentPerformance":{"type":["object","null"],"properties":{"evaluatedTradeCount":{"type":"integer"},"winRatePct":{"type":"number"},"averageExcessReturnPct":{"type":"number"},"asOfDate":{"type":["string","null"],"format":"date"}},"additionalProperties":false},
              "checks":{"type":"array","items":{"type":"object","required":["code","label","status","actualValue","threshold","detail"],"properties":{"code":{"type":"string"},"label":{"type":"string"},"status":{"type":"string","enum":["PASS","WARN","FAIL"]},"actualValue":{"type":"string"},"threshold":{"type":"string"},"detail":{"type":"string"}},"additionalProperties":false}}
            },"additionalProperties":false}
            """;

    static final String INDICATOR = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["latest","industry_detail","transmission","replay_timeline","radar_state","radar_events","limit_ladder","replay_summary","replay_episode","trade_review_state"],"description":"Market-indicator query operation to run."},
              "tradeDate":{"type":"string","format":"date","description":"Optional trading date in YYYY-MM-DD format."},
              "status":{"type":"string","enum":["all","limit_up","limit_down","rapid_rise","rapid_fall"],"description":"Optional status filter within the recent historical limit-up/down pool."},
              "eventType":{"type":"string","maxLength":40,"description":"Optional event type filter for latest."},
              "limit":{"type":"integer","minimum":1,"maximum":300,"description":"Maximum rows to return from the historical limit-up/down pool."},
              "industryCode":{"type":"string","minLength":1,"maxLength":40,"description":"Required industry code for industry_detail."},
              "page":{"type":"integer","minimum":0,"maximum":100,"description":"Zero-based industry-detail page number."},
              "pageSize":{"type":"integer","minimum":10,"maximum":100,"description":"Industry-detail page size."},
              "asOf":{"type":"string","format":"date-time","description":"Optional evidence cutoff; events after this instant are excluded."},
              "board":{"type":"string","enum":["all","limit_up","first_board","sealed","limit_down","touch"],"description":"Limit ladder board filter."},
              "episodeId":{"type":"string","minLength":1,"maxLength":120,"description":"Episode identifier for replay_episode."},
              "from":{"type":"string","pattern":"^([01]\\\\d|2[0-3]):[0-5]\\\\d(:[0-5]\\\\d)?$","description":"Replay start time in Asia/Shanghai, defaults to 09:30."},
              "to":{"type":"string","pattern":"^([01]\\\\d|2[0-3]):[0-5]\\\\d(:[0-5]\\\\d)?$","description":"Replay end time in Asia/Shanghai, defaults to 15:00."},
              "mode":{"type":"string","enum":["intraday","daily"],"description":"Trade Review materialized-state mode; daily ignores asOf."},
              "membership":{"type":"string","enum":["ths","tdx","union"],"description":"Industry membership basis for Trade Review state; defaults to union."},
              "windowDays":{"type":"integer","minimum":5,"maximum":120,"description":"Bounded Trade Review trend window; defaults to 60."}
            },"additionalProperties":false}
            """;

    static final String ETF = """
            {"type":"object","properties":{
              "historyDays":{"type":"integer","minimum":3,"maximum":30,"description":"Number of ETF trend days to return."},
              "rowLimit":{"type":"integer","minimum":20,"maximum":300,"description":"Maximum ETF rows to return across broad and sector groups."}
            },"additionalProperties":false}
            """;

    static final String FUTURES_POSITION = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["latest","instruments","members","history","coverage"],"description":"Futures-position query operation to run."},
              "tradeDate":{"type":"string","format":"date","description":"Optional requested trading date in YYYY-MM-DD format."},
              "scopeType":{"type":"string","enum":["CONTRACT","VARIETY"],"description":"Position aggregation scope; defaults to CONTRACT."},
              "exchangeCode":{"type":"string","enum":["CFFEX","SHFE","CZCE","DCE","GFEX","INE"],"description":"Exchange code; required for members and history."},
              "varietyCode":{"type":"string","maxLength":30,"description":"Optional futures variety filter such as IF, IH, IM, or IC."},
              "instrumentCode":{"type":"string","minLength":1,"maxLength":40,"description":"Required instrument code for members or history."},
              "rankType":{"type":"string","enum":["VOLUME","LONG","SHORT"],"description":"Optional member ranking dimension."},
              "top":{"type":"integer","enum":[5,10,15,20],"description":"Bounded top-row count; defaults to 10."},
              "sort":{"type":"string","enum":["volume","longPosition","shortPosition","netPosition","ratio","instrumentCode"],"description":"Instrument sort field; defaults to netPosition."},
              "sortDir":{"type":"string","enum":["asc","desc"],"description":"Instrument sort direction; defaults to desc."},
              "page":{"type":"integer","minimum":1,"maximum":100,"description":"Optional one-based result page."},
              "pageSize":{"type":"integer","minimum":1,"maximum":100,"description":"Optional result page size."},
              "limit":{"type":"integer","minimum":1,"maximum":120,"description":"Maximum history rows for history."}
            },"additionalProperties":false}
            """;

    static final String PULSE = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["overview_akshare","overview_archive","overview_eltdx","overview_history","manual_fund_flow","sector_snapshot","sector_history","index_returns","index_returns_history","index_kline","style_sectors","style_constituents","industry_constituents","heatmap","limit_emotion"],"description":"Market pulse query operation to run."},
              "days":{"type":"integer","minimum":1,"maximum":120,"description":"Bounded number of sessions for history or index returns."},
              "name":{"type":"string","minLength":1,"maxLength":80,"description":"Style-sector name for style_constituents."},
              "code":{"type":"string","minLength":1,"maxLength":80,"description":"Industry code for industry_constituents."},
              "kind":{"type":"string","enum":["industries","concepts","styles"],"description":"Heatmap category; defaults to industries."},
              "topN":{"type":"integer","minimum":1,"maximum":100,"description":"Requested heatmap row bound."}
              ,"range":{"type":"string","enum":["7d","30d","60d","90d"],"description":"Overview history range."},
              "tradingDate":{"type":"string","format":"date","description":"Trading date for archived overview or manual fund-flow lookup."},
              "window":{"type":"integer","minimum":1,"maximum":30,"description":"Window size for index return history."},
              "start":{"type":"string","format":"date","description":"Range start date for index return history."},
              "end":{"type":"string","format":"date","description":"Range end date for index return history."},
              "codes":{"type":"string","maxLength":200,"description":"Optional comma-separated index codes for index_kline."},
              "interval":{"type":"string","enum":["1m","5m","15m","30m","60m"],"description":"Index kline interval."}
            },"additionalProperties":false}
            """;

    static final String EMPTY = "{\"type\":\"object\"}";

    static final String STOCK_INFORMATION = """
            {"type":"object","required":["operation","symbol"],"properties":{
              "operation":{"type":"string","enum":["feed","content","analysis"],"description":"Stock information operation to run."},
              "symbol":{"type":"string","minLength":1,"maxLength":30,"description":"Required stock identifier."},
              "limit":{"type":"integer","minimum":1,"maximum":100,"description":"Maximum feed items per category."},
              "kind":{"type":"string","minLength":1,"maxLength":40,"description":"Information item kind for content or analysis."},
              "source":{"type":"string","minLength":1,"maxLength":80,"description":"Information source for content or analysis."},
              "itemId":{"type":"string","minLength":1,"maxLength":120,"description":"Information item identifier for content or analysis."}
            },"additionalProperties":false}
            """;

    static final String SKILL_DATA = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["datasets","rows"],"description":"A-share skill dataset operation to run."},
              "datasetCode":{"type":"string","minLength":1,"maxLength":80,"description":"Registered dataset code for rows."},
              "limit":{"type":"integer","minimum":1,"maximum":200,"description":"Maximum dataset rows to return."},
              "code":{"type":"string","maxLength":40,"description":"Optional stock code filter."},
              "source":{"type":"string","maxLength":80,"description":"Optional data-source filter."}
            },"additionalProperties":false}
            """;

    private MarketResearchAgentToolSchemas() {
    }
}
