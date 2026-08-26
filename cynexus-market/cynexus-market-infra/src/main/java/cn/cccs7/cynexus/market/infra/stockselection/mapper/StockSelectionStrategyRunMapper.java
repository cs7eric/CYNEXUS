package cn.cccs7.cynexus.market.infra.stockselection.mapper;

import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionStrategyCandidatePO;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionStrategyEvidencePO;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionStrategyRunPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockSelectionStrategyRunMapper {
    int insertRun(StockSelectionStrategyRunPO value);
    int insertCandidate(StockSelectionStrategyCandidatePO value);
    int insertEvidence(StockSelectionStrategyEvidencePO value);
    StockSelectionStrategyRunPO findLatestHealthInput(String tenantId, String ownerId, String strategyCode);
}
