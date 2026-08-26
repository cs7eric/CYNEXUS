package cn.cccs7.cynexus.market.infra.factorresearch;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyRepository;
import cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorStudyPO;
import cn.cccs7.cynexus.market.infra.factorresearch.mapper.MarketFactorStudyMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MarketFactorStudyRepositoryAdapter implements MarketFactorStudyRepository {
    private final MarketFactorStudyMapper mapper;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public MarketFactorStudyRepositoryAdapter(MarketFactorStudyMapper mapper, SnowflakeIdGenerator idGenerator,
                                              ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
    }

    @Override
    public Optional<MarketFactorResearchBO.Study> findById(String tenantId, String ownerId, String studyId) {
        return Optional.ofNullable(mapper.findByCode(tenantId, ownerId, studyId)).map(this::study);
    }

    @Override
    public Optional<MarketFactorResearchBO.Study> findByIdempotencyKey(String tenantId, String ownerId, String idempotencyKey) {
        return Optional.ofNullable(mapper.findByIdempotencyKey(tenantId, ownerId, idempotencyKey)).map(this::study);
    }

    @Override
    public List<MarketFactorResearchBO.Study> listByOwner(String tenantId, String ownerId, int offset, int limit) {
        return mapper.listByOwner(tenantId, ownerId, Math.max(0, offset), Math.max(1, Math.min(100, limit))).stream().map(this::study).toList();
    }

    @Override
    @Transactional
    public void save(MarketFactorResearchBO.Study study, String idempotencyKey) {
        mapper.insert(po(study, idempotencyKey));
    }

    @Override
    @Transactional
    public Optional<MarketFactorResearchBO.ClaimedStudy> claimNextQueued() {
        MarketFactorStudyPO next = mapper.findNextQueued();
        if (next == null || mapper.claim(next.getId(), OffsetDateTime.now(ZoneOffset.UTC)) != 1) return Optional.empty();
        return Optional.of(new MarketFactorResearchBO.ClaimedStudy(next.getStudyCode(), next.getTenantId(), next.getOwnerId()));
    }

    @Override
    @Transactional
    public void saveSamples(String tenantId, String ownerId, String studyId, List<MarketFactorResearchBO.StudySample> samples) {
        if (samples == null || samples.isEmpty()) return;
        Long internalId = mapper.findInternalIdByCode(tenantId, ownerId, studyId);
        if (internalId == null) throw new IllegalArgumentException("market-factor study was not found");
        for (int start = 0; start < samples.size(); start += 500) {
            List<java.util.Map<String, Object>> batch = samples.subList(start, Math.min(samples.size(), start + 500)).stream().map(sample -> {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("id", idGenerator.nextId()); row.put("sampleType", sample.sampleType().name()); row.put("stockCode", sample.code());
                row.put("market", sample.market()); row.put("eventTradeDate", sample.eventTradeDate()); row.put("anchorAt", sample.anchorAt());
                row.put("eventType", sample.eventType().name()); row.put("matchingMetadata", json(sample.matchingMetadata()));
                row.put("source", json(sample.source())); row.put("coverage", json(sample.coverage())); return row;
            }).toList();
            mapper.insertSamples(internalId, batch);
        }
    }

    @Override
    public void updateProgress(String tenantId, String ownerId, String studyId, java.util.Map<String, Object> progress) {
        mapper.updateProgress(tenantId, ownerId, studyId, json(progress));
    }

    @Override
    public boolean isRunning(String tenantId, String ownerId, String studyId) {
        return "RUNNING".equals(mapper.findStatus(tenantId, ownerId, studyId));
    }

    @Override
    public java.util.Map<String, Object> findProgress(String tenantId, String ownerId, String studyId) {
        return map(mapper.findProgress(tenantId, ownerId, studyId));
    }

    @Override
    @Transactional
    public void saveSequencePoints(String tenantId, String ownerId, String studyId, List<MarketFactorResearchBO.SequencePoint> points) {
        if (points == null || points.isEmpty()) return;
        Long internalId = mapper.findInternalIdByCode(tenantId, ownerId, studyId);
        if (internalId == null) throw new IllegalArgumentException("market-factor study was not found");
        for (int start = 0; start < points.size(); start += 500) {
            List<java.util.Map<String, Object>> batch = points.subList(start, Math.min(points.size(), start + 500)).stream().map(point -> {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("id", idGenerator.nextId()); row.put("sampleType", point.sampleType().name()); row.put("code", point.code());
                row.put("market", point.market()); row.put("eventTradeDate", point.eventTradeDate()); row.put("frequency", point.frequency());
                row.put("sequenceCode", point.sequenceCode()); row.put("offsetValue", point.offset()); row.put("observedAt", point.observedAt());
                row.put("availableAt", point.availableAt()); row.put("rawValue", point.rawValue()); row.put("normalizedValue", point.normalizedValue());
                row.put("source", json(point.source())); return row;
            }).toList();
            mapper.insertSequencePoints(internalId, batch);
        }
    }

    @Override
    public List<MarketFactorResearchBO.SequencePoint> listSequencePoints(String tenantId, String ownerId, String studyId,
                                                                           String sequenceCode, int offset, int limit) {
        return mapper.listSequencePoints(tenantId, ownerId, studyId, sequenceCode, Math.max(0, offset), Math.max(1, Math.min(500, limit))).stream()
                .map(value -> new MarketFactorResearchBO.SequencePoint(value.getCode(), value.getMarket(), value.getEventTradeDate(),
                        MarketFactorResearchBO.SampleType.valueOf(value.getSampleType()), value.getFrequency(), value.getSequenceCode(),
                        value.getOffsetValue(), value.getObservedAt(), value.getAvailableAt(), value.getRawValue(), value.getNormalizedValue(), map(value.getSource())))
                .toList();
    }

    @Override
    @Transactional
    public void saveFactorResults(String tenantId, String ownerId, String studyId, List<MarketFactorResearchBO.FactorResult> results) {
        if (results == null || results.isEmpty()) return;
        Long internalId = mapper.findInternalIdByCode(tenantId, ownerId, studyId);
        if (internalId == null) throw new IllegalArgumentException("market-factor study was not found");
        for (MarketFactorResearchBO.FactorResult result : results) {
            mapper.insertFactorResult(idGenerator.nextId(), internalId, result.discoveryCode(), result.recipeVersion(), result.method(),
                    result.scope().name(), json(result.recipe()), json(result.learnedBounds()), json(result.statistics()), json(result.validation()),
                    json(result.evidence()), result.verdict(), result.rank());
        }
    }

    @Override
    @Transactional
    public void saveOutcomes(String tenantId, String ownerId, String studyId, MarketFactorResearchBO.SampleType sampleType,
                             String code, String market, java.time.LocalDate eventTradeDate, List<java.util.Map<String, Object>> outcomes) {
        Long internalId = mapper.findInternalIdByCode(tenantId, ownerId, studyId);
        if (internalId == null || mapper.updateSampleOutcome(internalId, sampleType.name(), code, market, eventTradeDate, json(outcomes)) != 1) {
            throw new IllegalStateException("market-factor outcome sample was not found");
        }
    }

    @Override
    @Transactional
    public void saveOutcomesBatch(String tenantId, String ownerId, String studyId, List<java.util.Map<String, Object>> outcomes) {
        if (outcomes == null || outcomes.isEmpty()) return;
        Long internalId = mapper.findInternalIdByCode(tenantId, ownerId, studyId);
        if (internalId == null) throw new IllegalArgumentException("market-factor study was not found");
        for (int start = 0; start < outcomes.size(); start += 500) {
            List<java.util.Map<String, Object>> batch = outcomes.subList(start, Math.min(outcomes.size(), start + 500)).stream().map(row -> {
                java.util.Map<String, Object> value = new java.util.HashMap<>(row);
                value.put("outcome", json(row.get("outcome")));
                return value;
            }).toList();
            mapper.updateSampleOutcomes(internalId, batch);
        }
    }

    @Override
    public List<MarketFactorResearchBO.FactorResult> listFactorResults(String tenantId, String ownerId, String studyId, int offset, int limit) {
        return mapper.listFactorResults(tenantId, ownerId, studyId, Math.max(0, offset), Math.max(1, Math.min(100, limit))).stream()
                .map(value -> new MarketFactorResearchBO.FactorResult(value.getDiscoveryCode(), value.getRecipeVersion(), value.getMethod(),
                        MarketFactorResearchBO.DiscoveryScope.valueOf(value.getScope()), map(value.getRecipe()), map(value.getLearnedBounds()),
                        map(value.getStatistics()), map(value.getValidation()), map(value.getEvidence()), value.getVerdict(), value.getRank()))
                .toList();
    }

    @Override
    public List<java.util.Map<String, Object>> listOutcomes(String tenantId, String ownerId, String studyId, int offset, int limit) {
        return mapper.listOutcomes(tenantId, ownerId, studyId, Math.max(0, offset), Math.max(1, Math.min(200, limit))).stream()
                .map(value -> {
                    java.util.Map<String, Object> result = new java.util.LinkedHashMap<>(value);
                    Object outcomes = result.get("outcomes");
                    if (outcomes instanceof String json) result.put("outcomes", read(json, new TypeReference<List<java.util.Map<String, Object>>>() { }));
                    else if (outcomes instanceof java.util.Map<?, ?> wrapper && wrapper.containsKey("value")) {
                        Object wrapped = wrapper.get("value");
                        if (wrapped instanceof String json) result.put("outcomes", read(json, new TypeReference<List<java.util.Map<String, Object>>>() { }));
                        else if (wrapped instanceof List<?> list) result.put("outcomes", list);
                    }
                    if (!(result.get("outcomes") instanceof List<?>)) result.put("outcomes", List.of());
                    return result;
                }).toList();
    }

    @Override
    @Transactional
    public void promoteFactor(String tenantId, String ownerId, String studyId, String discoveryCode) {
        if (mapper.promoteFactor(tenantId, ownerId, studyId, discoveryCode) != 1) {
            throw new IllegalStateException("market-factor discovery is not promotable");
        }
    }

    @Override
    public List<MarketFactorResearchBO.StudySample> listSamples(String tenantId, String ownerId, String studyId,
                                                                  MarketFactorResearchBO.SampleType sampleType, int offset, int limit) {
        int safeOffset = Math.max(0, offset); int safeLimit = Math.max(1, Math.min(200, limit));
        return mapper.listSamples(tenantId, ownerId, studyId, sampleType == null ? null : sampleType.name(), safeOffset, safeLimit).stream()
                .map(value -> new MarketFactorResearchBO.StudySample(value.getStockCode(), value.getMarket(), value.getEventTradeDate(),
                        value.getAnchorAt(), MarketFactorResearchBO.EventType.valueOf(value.getEventType()),
                        MarketFactorResearchBO.SampleType.valueOf(value.getSampleType()), map(value.getMatchingMetadata()),
                        map(value.getSource()), map(value.getCoverage()))).toList();
    }

    @Override
    @Transactional
    public void cancel(String tenantId, String ownerId, String studyId) {
        if (mapper.cancel(tenantId, ownerId, studyId, OffsetDateTime.now(ZoneOffset.UTC)) != 1) {
            throw new IllegalStateException("market-factor study was no longer cancellable");
        }
    }

    @Override
    @Transactional
    public void complete(MarketFactorResearchBO.Study study) {
        if (mapper.complete(po(study, null)) != 1) throw new IllegalStateException("market-factor study was not running");
    }

    private MarketFactorStudyPO po(MarketFactorResearchBO.Study study, String idempotencyKey) {
        MarketFactorStudyPO value = new MarketFactorStudyPO();
        value.setId(idGenerator.nextId());
        value.setStudyCode(study.id()); value.setTenantId(study.tenantId()); value.setOwnerId(study.ownerId());
        value.setCanonicalConfig(json(study.config())); value.setCohortDefinitionVersion(study.config().cohortDefinitionVersion());
        value.setFeatureRegistryVersion(study.featureRegistryVersion()); value.setDataCutoffAt(study.dataCutoffAt());
        value.setLeakageCheckStatus(study.leakageCheckPassed() ? "PASSED" : "FAILED");
        value.setStatus(study.status().name()); value.setProgress(json(java.util.Map.of("completedSteps", 0, "totalSteps", 0,
                "stage", study.status().name(), "eventSampleCount", study.eventSampleCount(), "controlSampleCount", study.controlSampleCount())));
        value.setWarnings(json(study.warnings())); value.setErrorCode(null); value.setIdempotencyKey(idempotencyKey);
        value.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        value.setCompletedAt(study.status() == MarketFactorResearchBO.StudyStatus.QUEUED || study.status() == MarketFactorResearchBO.StudyStatus.RUNNING
                ? null : OffsetDateTime.now(ZoneOffset.UTC));
        return value;
    }

    private MarketFactorResearchBO.Study study(MarketFactorStudyPO value) {
        java.util.Map<String, Object> progress = map(value.getProgress());
        return new MarketFactorResearchBO.Study(value.getStudyCode(), value.getTenantId(), value.getOwnerId(),
                read(value.getCanonicalConfig(), MarketFactorResearchBO.StudyConfig.class),
                MarketFactorResearchBO.StudyStatus.valueOf(value.getStatus()), value.getDataCutoffAt(),
                value.getFeatureRegistryVersion(), "PASSED".equals(value.getLeakageCheckStatus()),
                number(progress.get("eventSampleCount")), number(progress.get("controlSampleCount")), list(value.getWarnings()));
    }

    private List<String> list(String value) { return read(value, new TypeReference<List<String>>() { }); }
    private <T> T read(String value, Class<T> type) {
        try { return objectMapper.readValue(value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to read market-factor study JSON", exception); }
    }
    private <T> T read(String value, TypeReference<T> type) {
        try { return objectMapper.readValue(value == null ? "[]" : value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to read market-factor study JSON", exception); }
    }
    private java.util.Map<String, Object> map(String value) {
        try { return objectMapper.readValue(value == null ? "{}" : value, new TypeReference<java.util.Map<String, Object>>() { }); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to read market-factor study JSON", exception); }
    }
    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to persist market-factor study JSON", exception); }
    }
    private int number(Object value) { return value instanceof Number number ? number.intValue() : 0; }
}
