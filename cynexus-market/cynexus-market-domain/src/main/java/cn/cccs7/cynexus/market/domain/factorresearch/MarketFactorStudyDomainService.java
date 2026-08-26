package cn.cccs7.cynexus.market.domain.factorresearch;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Owns Study identity, idempotency and lifecycle transitions; workers own computation behind this boundary. */
public final class MarketFactorStudyDomainService {
    private final MarketFactorResearchPolicy researchPolicy;
    private final MarketFactorStudyLifecyclePolicy lifecyclePolicy;
    private final MarketFactorStudyRepository repository;
    private final MarketFactorStudyIdGenerator idGenerator;

    public MarketFactorStudyDomainService(MarketFactorResearchPolicy researchPolicy,
                                          MarketFactorStudyLifecyclePolicy lifecyclePolicy,
                                          MarketFactorStudyRepository repository,
                                          MarketFactorStudyIdGenerator idGenerator) {
        this.researchPolicy = researchPolicy;
        this.lifecyclePolicy = lifecyclePolicy;
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    public MarketFactorResearchBO.Study queue(MarketFactorResearchBO.StudyCreate request) {
        requireScope(request);
        researchPolicy.validateConfig(request.config());
        String idempotencyKey = requireIdempotencyKey(request.idempotencyKey());
        return repository.findByIdempotencyKey(request.tenantId(), request.ownerId(), idempotencyKey).orElseGet(() -> {
            MarketFactorResearchBO.Study study = new MarketFactorResearchBO.Study(idGenerator.nextStudyId(), request.tenantId(),
                    request.ownerId(), request.config(), MarketFactorResearchBO.StudyStatus.QUEUED,
                    request.dataCutoffAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : request.dataCutoffAt(),
                    blank(request.featureRegistryVersion()) ? "v1" : request.featureRegistryVersion().trim(), true, 0, 0, List.of());
            repository.save(study, idempotencyKey);
            return study;
        });
    }

    public MarketFactorResearchBO.Study find(String tenantId, String ownerId, String studyId) {
        return repository.findById(tenantId, ownerId, studyId)
                .orElseThrow(() -> new IllegalArgumentException("market-factor study was not found"));
    }

    public MarketFactorResearchBO.Study cancel(String tenantId, String ownerId, String studyId) {
        MarketFactorResearchBO.Study study = find(tenantId, ownerId, studyId);
        lifecyclePolicy.requireTransition(study.status(), MarketFactorResearchBO.StudyStatus.CANCELLED);
        repository.cancel(tenantId, ownerId, studyId);
        return new MarketFactorResearchBO.Study(study.id(), study.tenantId(), study.ownerId(), study.config(),
                MarketFactorResearchBO.StudyStatus.CANCELLED, study.dataCutoffAt(), study.featureRegistryVersion(),
                study.leakageCheckPassed(), study.eventSampleCount(), study.controlSampleCount(), study.warnings());
    }

    public MarketFactorResearchBO.Study retry(String tenantId, String ownerId, String studyId, String idempotencyKey) {
        MarketFactorResearchBO.Study prior = find(tenantId, ownerId, studyId);
        lifecyclePolicy.requireRetryable(prior.status());
        return queue(new MarketFactorResearchBO.StudyCreate(tenantId, ownerId, prior.config(), prior.dataCutoffAt(),
                prior.featureRegistryVersion(), idempotencyKey));
    }

    private void requireScope(MarketFactorResearchBO.StudyCreate request) {
        if (request == null || blank(request.tenantId()) || blank(request.ownerId())) {
            throw new IllegalArgumentException("tenant and owner are required");
        }
    }

    private String requireIdempotencyKey(String value) {
        if (blank(value) || value.trim().length() > 128) throw new IllegalArgumentException("idempotencyKey is required and must be at most 128 characters");
        return value.trim();
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
