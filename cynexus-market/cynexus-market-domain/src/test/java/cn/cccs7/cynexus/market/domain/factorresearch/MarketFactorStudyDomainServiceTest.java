package cn.cccs7.cynexus.market.domain.factorresearch;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketFactorStudyDomainServiceTest {
    @Test
    void queuesOneOwnerScopedStudyForTheSameIdempotencyKey() {
        MemoryRepository repository = new MemoryRepository();
        MarketFactorStudyDomainService service = service(repository);

        MarketFactorResearchBO.Study first = service.queue(request("dedupe-1"));
        MarketFactorResearchBO.Study second = service.queue(request("dedupe-1"));

        assertEquals(first.id(), second.id());
        assertEquals(MarketFactorResearchBO.StudyStatus.QUEUED, first.status());
    }

    @Test
    void preventsCancellationOfTerminalStudies() {
        MemoryRepository repository = new MemoryRepository();
        MarketFactorStudyDomainService service = service(repository);
        MarketFactorResearchBO.Study queued = service.queue(request("cancel-1"));
        repository.byId.put(queued.id(), study(queued, MarketFactorResearchBO.StudyStatus.SUCCEEDED));

        assertThrows(IllegalStateException.class, () -> service.cancel("tenant", "owner", queued.id()));
    }

    @Test
    void retryRequiresANewCanonicalIdempotencyKey() {
        MemoryRepository repository = new MemoryRepository();
        MarketFactorStudyDomainService service = service(repository);
        MarketFactorResearchBO.Study prior = service.queue(request("first"));
        repository.byId.put(prior.id(), study(prior, MarketFactorResearchBO.StudyStatus.FAILED));

        MarketFactorResearchBO.Study retried = service.retry("tenant", "owner", prior.id(), "retry-1");

        assertEquals(MarketFactorResearchBO.StudyStatus.QUEUED, retried.status());
        assertThrows(IllegalStateException.class, () -> service.retry("tenant", "owner", retried.id(), "retry-2"));
    }

    private MarketFactorStudyDomainService service(MemoryRepository repository) {
        return new MarketFactorStudyDomainService(new MarketFactorResearchPolicy(), new MarketFactorStudyLifecyclePolicy(), repository,
                () -> "study-" + (repository.byId.size() + 1));
    }

    private MarketFactorResearchBO.StudyCreate request(String key) {
        return new MarketFactorResearchBO.StudyCreate("tenant", "owner",
                new MarketFactorResearchBO.StudyConfig(MarketFactorResearchBO.EventType.LIMIT_UP_CLOSE, 1,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 1), List.of(1, 5, 20), 2, 60, 120,
                        List.of("CHANGE_POINT")), OffsetDateTime.parse("2026-05-01T08:00:00Z"), "v1", key);
    }

    private MarketFactorResearchBO.Study study(MarketFactorResearchBO.Study value, MarketFactorResearchBO.StudyStatus status) {
        return new MarketFactorResearchBO.Study(value.id(), value.tenantId(), value.ownerId(), value.config(), status,
                value.dataCutoffAt(), value.featureRegistryVersion(), value.leakageCheckPassed(), value.eventSampleCount(),
                value.controlSampleCount(), value.warnings());
    }

    private static final class MemoryRepository implements MarketFactorStudyRepository {
        private final Map<String, MarketFactorResearchBO.Study> byId = new HashMap<>();
        private final Map<String, String> byIdempotency = new HashMap<>();

        @Override public Optional<MarketFactorResearchBO.Study> findById(String tenantId, String ownerId, String studyId) {
            return Optional.ofNullable(byId.get(studyId)).filter(value -> value.tenantId().equals(tenantId) && value.ownerId().equals(ownerId));
        }
        @Override public Optional<MarketFactorResearchBO.Study> findByIdempotencyKey(String tenantId, String ownerId, String idempotencyKey) {
            return Optional.ofNullable(byIdempotency.get(tenantId + ownerId + idempotencyKey)).map(byId::get);
        }
        @Override public List<MarketFactorResearchBO.Study> listByOwner(String tenantId, String ownerId, int offset, int limit) { return byId.values().stream().toList(); }
        @Override public void save(MarketFactorResearchBO.Study study, String idempotencyKey) {
            byId.put(study.id(), study); byIdempotency.put(study.tenantId() + study.ownerId() + idempotencyKey, study.id());
        }
        @Override public Optional<MarketFactorResearchBO.ClaimedStudy> claimNextQueued() { return Optional.empty(); }
        @Override public void saveSamples(String tenantId, String ownerId, String studyId, List<MarketFactorResearchBO.StudySample> samples) { }
        @Override public List<MarketFactorResearchBO.StudySample> listSamples(String tenantId, String ownerId, String studyId,
                                                                                MarketFactorResearchBO.SampleType sampleType, int offset, int limit) { return List.of(); }
        @Override public void cancel(String tenantId, String ownerId, String studyId) {
            MarketFactorResearchBO.Study value = byId.get(studyId);
            byId.put(studyId, new MarketFactorResearchBO.Study(value.id(), value.tenantId(), value.ownerId(), value.config(),
                    MarketFactorResearchBO.StudyStatus.CANCELLED, value.dataCutoffAt(), value.featureRegistryVersion(),
                    value.leakageCheckPassed(), value.eventSampleCount(), value.controlSampleCount(), value.warnings()));
        }
        @Override public void complete(MarketFactorResearchBO.Study study) { byId.put(study.id(), study); }
    }
}
