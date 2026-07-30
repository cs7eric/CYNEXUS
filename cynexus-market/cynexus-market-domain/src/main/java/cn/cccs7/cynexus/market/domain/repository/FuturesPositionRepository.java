package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.futures.FuturesPositionQueryBO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FuturesPositionRepository {
    Optional<LocalDate> findLatestTradeDate(LocalDate endDate);

    List<FuturesPositionQueryBO.Instrument> listInstruments(LocalDate tradeDate, String scopeType,
                                                            String exchangeCode, String varietyCode,
                                                            int top, String sort, String sortDir,
                                                            int offset, int limit);

    int countInstruments(LocalDate tradeDate, String scopeType, String exchangeCode, String varietyCode);

    FuturesPositionQueryBO.Aggregate aggregate(LocalDate tradeDate, String scopeType,
                                               String exchangeCode, String varietyCode, int top);

    List<FuturesPositionQueryBO.Member> listMembers(LocalDate tradeDate, String scopeType,
                                                    String exchangeCode, String instrumentCode,
                                                    String rankType, int top);

    List<FuturesPositionQueryBO.History> listHistory(String scopeType, String exchangeCode,
                                                     String instrumentCode, int top, int limit);

    List<FuturesPositionQueryBO.CoverageItem> listCoverage(LocalDate tradeDate);
}
