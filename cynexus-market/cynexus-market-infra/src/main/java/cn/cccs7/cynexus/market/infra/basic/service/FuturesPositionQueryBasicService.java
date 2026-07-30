package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionAggregatePO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionCoveragePO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionHistoryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionInstrumentPO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionMemberPO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FuturesPositionQueryBasicService {
    Optional<LocalDate> findLatestTradeDate(LocalDate endDate);
    List<FuturesPositionInstrumentPO> listInstruments(LocalDate tradeDate, String scopeType, String exchangeCode,
                                                      String varietyCode, int top, String sort, String sortDir,
                                                      int offset, int limit);
    int countInstruments(LocalDate tradeDate, String scopeType, String exchangeCode, String varietyCode);
    FuturesPositionAggregatePO aggregate(LocalDate tradeDate, String scopeType, String exchangeCode,
                                         String varietyCode, int top);
    List<FuturesPositionMemberPO> listMembers(LocalDate tradeDate, String scopeType, String exchangeCode,
                                              String instrumentCode, String rankType, int top);
    List<FuturesPositionHistoryPO> listHistory(String scopeType, String exchangeCode, String instrumentCode,
                                               int top, int limit);
    List<FuturesPositionCoveragePO> listCoverage(LocalDate tradeDate);
}
