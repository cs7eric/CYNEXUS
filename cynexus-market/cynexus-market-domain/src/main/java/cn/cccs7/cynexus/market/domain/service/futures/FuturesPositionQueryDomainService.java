package cn.cccs7.cynexus.market.domain.service.futures;

import cn.cccs7.cynexus.market.domain.entity.futures.FuturesPositionQueryBO;

public interface FuturesPositionQueryDomainService {
    FuturesPositionQueryBO.Latest latest(String tradeDate, String scopeType, String exchangeCode,
                                         String varietyCode, int top, String sort, String sortDir,
                                         Integer page, Integer pageSize);

    FuturesPositionQueryBO.Latest instruments(String tradeDate, String scopeType, String exchangeCode,
                                              String varietyCode, int top, String sort, String sortDir,
                                              Integer page, Integer pageSize);

    java.util.List<FuturesPositionQueryBO.Member> members(String tradeDate, String scopeType,
                                                          String exchangeCode, String instrumentCode,
                                                          String rankType, int top);

    java.util.List<FuturesPositionQueryBO.History> history(String tradeDate, String scopeType, String exchangeCode,
                                                            String instrumentCode, int top, int limit);

    FuturesPositionQueryBO.Coverage coverage(String tradeDate);
}
