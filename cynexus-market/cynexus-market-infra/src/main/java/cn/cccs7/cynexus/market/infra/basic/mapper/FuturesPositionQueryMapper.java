package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionAggregatePO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionCoveragePO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionHistoryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionInstrumentPO;
import cn.cccs7.cynexus.market.infra.basic.entity.FuturesPositionMemberPO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FuturesPositionQueryMapper {
    LocalDate findLatestTradeDate(@Param("endDate") LocalDate endDate);
    List<FuturesPositionInstrumentPO> listInstruments(@Param("tradeDate") LocalDate tradeDate,
                                                      @Param("scopeType") String scopeType,
                                                      @Param("exchangeCode") String exchangeCode,
                                                      @Param("varietyCode") String varietyCode,
                                                      @Param("top") int top,
                                                      @Param("sort") String sort,
                                                      @Param("sortDir") String sortDir,
                                                      @Param("offset") int offset,
                                                      @Param("limit") int limit);
    int countInstruments(@Param("tradeDate") LocalDate tradeDate, @Param("scopeType") String scopeType,
                         @Param("exchangeCode") String exchangeCode, @Param("varietyCode") String varietyCode);
    FuturesPositionAggregatePO aggregate(@Param("tradeDate") LocalDate tradeDate, @Param("scopeType") String scopeType,
                                         @Param("exchangeCode") String exchangeCode, @Param("varietyCode") String varietyCode,
                                         @Param("top") int top);
    List<FuturesPositionMemberPO> listMembers(@Param("tradeDate") LocalDate tradeDate, @Param("scopeType") String scopeType,
                                              @Param("exchangeCode") String exchangeCode, @Param("instrumentCode") String instrumentCode,
                                              @Param("rankType") String rankType, @Param("top") int top);
    List<FuturesPositionHistoryPO> listHistory(@Param("scopeType") String scopeType, @Param("exchangeCode") String exchangeCode,
                                               @Param("instrumentCode") String instrumentCode, @Param("top") int top,
                                               @Param("limit") int limit, @Param("endDate") LocalDate endDate);
    List<FuturesPositionCoveragePO> listCoverage(@Param("tradeDate") LocalDate tradeDate);
}
