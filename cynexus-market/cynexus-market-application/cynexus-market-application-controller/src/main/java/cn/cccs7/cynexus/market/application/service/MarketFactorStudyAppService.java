package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.MarketFactorStudyDTO;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyDomainService;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyRepository;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketFactorStudyAppService {
    private final MarketFactorStudyDomainService domainService;
    private final MarketFactorStudyRepository repository;

    public MarketFactorStudyAppService(MarketFactorStudyDomainService domainService, MarketFactorStudyRepository repository) {
        this.domainService = domainService; this.repository = repository;
    }

    public MarketFactorStudyDTO.Response queue(StockSelectionStrategyRunBO.Scope scope, MarketFactorStudyDTO.CreateRequest request) {
        if (request == null) throw new IllegalArgumentException("study request is required");
        MarketFactorResearchBO.StudyConfig config = new MarketFactorResearchBO.StudyConfig(
                MarketFactorResearchBO.EventType.valueOf(request.getEventType()),
                defaultValue(request.getCohortDefinitionVersion(), 1), LocalDate.parse(request.getStartDate()), LocalDate.parse(request.getEndDate()),
                request.getOutcomeHorizons() == null ? List.of(1, 3, 5, 10, 20) : request.getOutcomeHorizons(),
                defaultValue(request.getControlsPerEvent(), 2), defaultValue(request.getDailyPreWindowSessions(), 60),
                defaultValue(request.getMinutePreWindowMinutes(), 120), request.getDiscoveryMethods(), request.getEventParams());
        return response(domainService.queue(new MarketFactorResearchBO.StudyCreate(scope.tenantId(), scope.ownerId(), config,
                blank(request.getDataCutoffAt()) ? null : OffsetDateTime.parse(request.getDataCutoffAt()),
                request.getFeatureRegistryVersion(), request.getIdempotencyKey())));
    }

    public MarketFactorStudyDTO.Response find(StockSelectionStrategyRunBO.Scope scope, String studyId) {
        return response(domainService.find(scope.tenantId(), scope.ownerId(), studyId));
    }

    public List<MarketFactorStudyDTO.Response> list(StockSelectionStrategyRunBO.Scope scope, Integer offset, Integer limit) {
        return repository.listByOwner(scope.tenantId(), scope.ownerId(), defaultValue(offset, 0), defaultValue(limit, 50)).stream()
                .map(this::response).toList();
    }

    public MarketFactorStudyDTO.Response cancel(StockSelectionStrategyRunBO.Scope scope, String studyId) {
        return response(domainService.cancel(scope.tenantId(), scope.ownerId(), studyId));
    }

    public MarketFactorStudyDTO.Response retry(StockSelectionStrategyRunBO.Scope scope, String studyId, String idempotencyKey) {
        return response(domainService.retry(scope.tenantId(), scope.ownerId(), studyId, idempotencyKey));
    }

    public List<MarketFactorStudyDTO.SampleResponse> samples(StockSelectionStrategyRunBO.Scope scope, String studyId,
                                                               String sampleType, Integer offset, Integer limit) {
        MarketFactorResearchBO.SampleType type = blank(sampleType) ? null : MarketFactorResearchBO.SampleType.valueOf(sampleType);
        return repository.listSamples(scope.tenantId(), scope.ownerId(), studyId, type, defaultValue(offset, 0), defaultValue(limit, 100)).stream()
                .map(sample -> MarketFactorStudyDTO.SampleResponse.builder().code(sample.code()).market(sample.market())
                        .eventTradeDate(sample.eventTradeDate().toString()).anchorAt(sample.anchorAt() == null ? null : sample.anchorAt().toString()).eventType(sample.eventType().name())
                        .sampleType(sample.sampleType().name()).matchingMetadata(sample.matchingMetadata()).source(sample.source())
                        .coverage(sample.coverage()).build()).toList();
    }

    public List<MarketFactorStudyDTO.SequencePointResponse> sequences(StockSelectionStrategyRunBO.Scope scope, String studyId,
                                                                        String sequenceCode, Integer offset, Integer limit) {
        return repository.listSequencePoints(scope.tenantId(), scope.ownerId(), studyId, sequenceCode, defaultValue(offset, 0), defaultValue(limit, 200)).stream()
                .map(value -> MarketFactorStudyDTO.SequencePointResponse.builder().code(value.code()).market(value.market())
                        .eventTradeDate(value.eventTradeDate().toString()).sampleType(value.sampleType().name()).frequency(value.frequency())
                        .sequenceCode(value.sequenceCode()).offset(value.offset()).observedAt(value.observedAt().toString())
                        .availableAt(value.availableAt().toString()).rawValue(value.rawValue()).normalizedValue(value.normalizedValue()).source(value.source()).build())
                .toList();
    }

    public List<MarketFactorStudyDTO.FactorResultResponse> discoveries(StockSelectionStrategyRunBO.Scope scope, String studyId,
                                                                        Integer offset, Integer limit) {
        return repository.listFactorResults(scope.tenantId(), scope.ownerId(), studyId, defaultValue(offset, 0), defaultValue(limit, 50)).stream()
                .map(value -> MarketFactorStudyDTO.FactorResultResponse.builder().discoveryCode(value.discoveryCode()).recipeVersion(value.recipeVersion())
                        .method(value.method()).scope(value.scope().name()).recipe(value.recipe()).learnedBounds(value.learnedBounds())
                        .statistics(value.statistics()).validation(value.validation()).evidence(value.evidence()).verdict(value.verdict()).rank(value.rank()).build())
                .toList();
    }

    public List<MarketFactorStudyDTO.ClusterResponse> clusters(StockSelectionStrategyRunBO.Scope scope, String studyId) {
        return repository.listFactorResults(scope.tenantId(), scope.ownerId(), studyId, 0, 1000).stream()
                .collect(java.util.stream.Collectors.groupingBy(v -> v.method() == null ? "UNKNOWN" : v.method(), java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()))
                .entrySet().stream().map(e -> MarketFactorStudyDTO.ClusterResponse.builder().clusterCode(e.getKey()).method(e.getKey())
                        .memberCount(e.getValue().size()).discoveryCodes(e.getValue().stream().map(MarketFactorResearchBO.FactorResult::discoveryCode).toList()).build()).toList();
    }

    public MarketFactorStudyDTO.FactorResultResponse discovery(StockSelectionStrategyRunBO.Scope scope, String studyId, String discoveryCode) {
        MarketFactorResearchBO.FactorResult value = repository.findFactorResult(scope.tenantId(), scope.ownerId(), studyId, discoveryCode)
                .orElseThrow(() -> new IllegalArgumentException("discovery not found"));
        return MarketFactorStudyDTO.FactorResultResponse.builder().discoveryCode(value.discoveryCode()).recipeVersion(value.recipeVersion())
                .method(value.method()).scope(value.scope().name()).recipe(value.recipe()).learnedBounds(value.learnedBounds()).statistics(value.statistics())
                .validation(value.validation()).evidence(value.evidence()).verdict(value.verdict()).rank(value.rank()).build();
    }

    public MarketFactorStudyDTO.FactorResultResponse promote(StockSelectionStrategyRunBO.Scope scope, String studyId, String discoveryCode) {
        MarketFactorStudyDTO.FactorResultResponse value = discovery(scope, studyId, discoveryCode);
        if (!"CANDIDATE".equals(value.getVerdict())) throw new IllegalStateException("only CANDIDATE discoveries may be promoted");
        repository.promoteFactor(scope.tenantId(), scope.ownerId(), studyId, discoveryCode);
        value.setVerdict("PROMOTED");
        return value;
    }

    public List<java.util.Map<String, Object>> outcomes(StockSelectionStrategyRunBO.Scope scope, String studyId, Integer offset, Integer limit) {
        return repository.listOutcomes(scope.tenantId(), scope.ownerId(), studyId, defaultValue(offset, 0), defaultValue(limit, 100));
    }

    private MarketFactorStudyDTO.Response response(MarketFactorResearchBO.Study study) {
        return MarketFactorStudyDTO.Response.builder().studyId(study.id()).status(study.status().name())
                .eventType(study.config().eventType().name()).startDate(study.config().startDate().toString()).endDate(study.config().endDate().toString())
                .dataCutoffAt(study.dataCutoffAt().toString()).featureRegistryVersion(study.featureRegistryVersion())
                .leakageCheckPassed(study.leakageCheckPassed()).eventSampleCount(study.eventSampleCount())
                .progress(repository.findProgress(study.tenantId(), study.ownerId(), study.id()))
                .controlSampleCount(study.controlSampleCount()).warnings(study.warnings()).build();
    }

    private int defaultValue(Integer value, int fallback) { return value == null ? fallback : value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
