package cn.cccs7.cynexus.market.infra.stockselection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunRepository;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionStrategyCandidatePO;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionStrategyEvidencePO;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionStrategyRunPO;
import cn.cccs7.cynexus.market.infra.stockselection.mapper.StockSelectionStrategyRunMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class StockSelectionStrategyRunRepositoryAdapter implements StockSelectionStrategyRunRepository {

    private final StockSelectionStrategyRunMapper mapper;
    private final ObjectMapper objectMapper;

    public StockSelectionStrategyRunRepositoryAdapter(StockSelectionStrategyRunMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void save(StockSelectionStrategyRunBO.Run value) {
        StockSelectionStrategyRunPO run = new StockSelectionStrategyRunPO();
        run.setId(value.id()); run.setTenantId(value.scope().tenantId()); run.setOwnerId(value.scope().ownerId());
        run.setStrategyVersionId(value.strategyVersionId()); run.setRequestedTradeDate(value.requestedTradeDate()); run.setActualTradeDate(value.actualTradeDate());
        run.setStatus(value.status()); run.setParameterSnapshot(json(value.parameterSnapshot())); run.setDataSource(value.source());
        Map<String, Object> freshness = new LinkedHashMap<>();
        freshness.put("actualTradeDate", value.actualTradeDate() == null ? null : value.actualTradeDate().toString());
        run.setDataFreshness(json(freshness));
        run.setUniverseCount(value.universeCount()); run.setCandidateCount(value.matchedCount()); run.setWarnings(json(value.warnings()));
        run.setStartedAt(value.startedAt()); run.setCompletedAt(value.completedAt());
        mapper.insertRun(run);
        for (StockSelectionStrategyRunBO.Candidate candidate : value.candidates()) {
            StockSelectionStrategyCandidatePO item = new StockSelectionStrategyCandidatePO();
            item.setId(candidate.id()); item.setStrategyRunId(value.id()); item.setStockCode(candidate.stockCode()); item.setStockName(candidate.stockName());
            item.setRankNo(candidate.rank()); item.setScore(candidate.score()); item.setReasonSummary(candidate.reasonSummary()); item.setSnapshot(json(candidate.snapshot()));
            mapper.insertCandidate(item);
            for (StockSelectionStrategyRunBO.Evidence evidence : candidate.evidence()) {
                StockSelectionStrategyEvidencePO evidencePo = new StockSelectionStrategyEvidencePO();
                evidencePo.setId(evidence.id()); evidencePo.setStrategyCandidateId(candidate.id()); evidencePo.setEvidenceCode(evidence.code());
                evidencePo.setLabel(evidence.label()); evidencePo.setOperator(evidence.operator()); evidencePo.setExpectedValue(json(evidence.expectedValue()));
                evidencePo.setActualValue(json(evidence.actualValue())); evidencePo.setMatched(evidence.matched());
                mapper.insertEvidence(evidencePo);
            }
        }
    }

    @Override
    public Optional<StockSelectionStrategyRunBO.HealthInput> findLatestHealthInput(StockSelectionStrategyRunBO.Scope scope,
                                                                                     String strategyCode) {
        StockSelectionStrategyRunPO run = mapper.findLatestHealthInput(scope.tenantId(), scope.ownerId(), strategyCode);
        if (run == null) return Optional.empty();
        return Optional.of(new StockSelectionStrategyRunBO.HealthInput(run.getStrategyCode(), run.getStrategyVersionNo(),
                run.getRequestedTradeDate(), run.getActualTradeDate(), run.getStatus(), run.getUniverseCount(), run.getCandidateCount(),
                run.getDataSource(), warnings(run.getWarnings())));
    }

    private List<String> warnings(String value) {
        try {
            return value == null || value.isBlank() ? List.of() : objectMapper.readValue(value, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            return List.of("Stored run warnings could not be decoded.");
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to persist stock selection JSON", exception);
        }
    }
}
