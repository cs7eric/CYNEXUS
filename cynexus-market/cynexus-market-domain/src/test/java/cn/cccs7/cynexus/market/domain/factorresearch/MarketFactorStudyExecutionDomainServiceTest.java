package cn.cccs7.cynexus.market.domain.factorresearch;

import cn.cccs7.cynexus.market.domain.factorresearch.cohort.MarketEventCohort;
import cn.cccs7.cynexus.market.domain.factorresearch.cohort.MarketEventCohortProvider;
import cn.cccs7.cynexus.market.domain.factorresearch.cohort.MarketEventCohortProviderRegistry;
import cn.cccs7.cynexus.market.domain.factorresearch.cohort.MarketEventCohortQuery;
import cn.cccs7.cynexus.market.domain.factorresearch.control.MarketFactorControlMatchPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketFactorStudyExecutionDomainServiceTest {
    @Test
    void freezesCohortSamplesAndLeavesStudyPartialUntilMaterializedControlsAndDiscoveryRun() {
        MemoryRepository repository = new MemoryRepository(study());
        MarketEventCohortProvider provider = provider(List.of(new MarketEventCohort.Sample("600000", "SH", LocalDate.of(2026, 8, 14),
                OffsetDateTime.parse("2026-08-14T15:00:00+08:00"), "test", Map.of("coverage", "AVAILABLE"))));
        MarketFactorStudyExecutionDomainService service = new MarketFactorStudyExecutionDomainService(repository,
                new MarketEventCohortProviderRegistry(List.of(provider)), new MarketFactorStudyLifecyclePolicy());
        assertTrue(service.processNextQueuedStudy());
        assertEquals(MarketFactorResearchBO.StudyStatus.PARTIAL, repository.study.status());
        assertEquals(1, repository.samples.size());
        assertEquals(MarketFactorResearchBO.SampleType.EVENT, repository.samples.getFirst().sampleType());
        assertTrue(repository.study.warnings().contains("MATERIALIZED_CONTROL_MATCHING_NOT_CONFIGURED"));
        assertTrue(repository.study.warnings().contains("SEQUENCE_DISCOVERY_NOT_CONFIGURED"));
    }

    @Test
    void freezesOnlyPreEventMaterializedControlsAndRecordsTheirMatchQuality() {
        MemoryRepository repository = new MemoryRepository(study());
        MarketEventCohortProvider provider = provider(List.of(new MarketEventCohort.Sample("600000", "SH", LocalDate.of(2026, 8, 14),
                OffsetDateTime.parse("2026-08-14T15:00:00+08:00"), "test", Map.of())));
        MarketFactorStudySnapshotRepository snapshots = (eventDate, cutoff, version) -> List.of(
                snapshot("600000", "SH", "Utilities", 100, 20), snapshot("600001", "SH", "Utilities", 100, 20),
                snapshot("600002", "SH", "Utilities", 100, 20));
        MarketFactorStudyExecutionDomainService service = new MarketFactorStudyExecutionDomainService(repository,
                new MarketEventCohortProviderRegistry(List.of(provider)), new MarketFactorStudyLifecyclePolicy(), snapshots,
                new MarketFactorControlMatchPolicy());

        service.processNextQueuedStudy();

        assertEquals(MarketFactorResearchBO.StudyStatus.PARTIAL, repository.study.status());
        assertEquals(2, repository.samples.stream().filter(value -> value.sampleType() == MarketFactorResearchBO.SampleType.CONTROL).count());
        assertTrue(repository.samples.stream().filter(value -> value.sampleType() == MarketFactorResearchBO.SampleType.CONTROL)
                .allMatch(value -> "EXACT".equals(value.matchingMetadata().get("matchQuality"))));
    }

    @Test
    void carriesOnlyPersistedRegimeLabelsIntoMatchedControlMetadata() {
        MemoryRepository repository = new MemoryRepository(study());
        MarketEventCohortProvider provider = provider(List.of(new MarketEventCohort.Sample("600000", "SH", LocalDate.of(2026, 8, 14),
                OffsetDateTime.parse("2026-08-14T15:00:00+08:00"), "test", Map.of())));
        MarketFactorStudySnapshotRepository snapshots = (eventDate, cutoff, version) -> List.of(
                new MarketFactorStudySnapshotRepository.Snapshot("600000", "SH", LocalDate.of(2026, 8, 13),
                        OffsetDateTime.parse("2026-08-13T15:00:00+08:00"), "Utilities", false, BigDecimal.valueOf(100), BigDecimal.valueOf(20),
                        "a_stock_daily_factor_snapshot", "AVAILABLE", "RISK_ON", "START"),
                new MarketFactorStudySnapshotRepository.Snapshot("600001", "SH", LocalDate.of(2026, 8, 13),
                        OffsetDateTime.parse("2026-08-13T15:00:00+08:00"), "Utilities", false, BigDecimal.valueOf(100), BigDecimal.valueOf(20),
                        "a_stock_daily_factor_snapshot", "AVAILABLE", "RISK_ON", "START"));
        MarketFactorStudyExecutionDomainService service = new MarketFactorStudyExecutionDomainService(repository,
                new MarketEventCohortProviderRegistry(List.of(provider)), new MarketFactorStudyLifecyclePolicy(), snapshots,
                new MarketFactorControlMatchPolicy());

        service.processNextQueuedStudy();

        MarketFactorResearchBO.StudySample control = repository.samples.stream()
                .filter(value -> value.sampleType() == MarketFactorResearchBO.SampleType.CONTROL).findFirst().orElseThrow();
        MarketFactorResearchBO.StudySample event = repository.samples.stream()
                .filter(value -> value.sampleType() == MarketFactorResearchBO.SampleType.EVENT).findFirst().orElseThrow();
        assertEquals("RISK_ON", event.matchingMetadata().get("marketRegime"));
        assertEquals("START", event.matchingMetadata().get("industryRegime"));
        assertEquals("RISK_ON", control.matchingMetadata().get("marketRegime"));
        assertEquals("START", control.matchingMetadata().get("industryRegime"));
    }

    private MarketFactorStudySnapshotRepository.Snapshot snapshot(String code, String market, String industry, int cap, int amount) {
        return new MarketFactorStudySnapshotRepository.Snapshot(code, market, LocalDate.of(2026, 8, 13),
                OffsetDateTime.parse("2026-08-13T15:00:00+08:00"), industry, false, BigDecimal.valueOf(cap), BigDecimal.valueOf(amount),
                "a_stock_daily_factor_snapshot", "AVAILABLE");
    }

    @Test
    void recordsNoDataWithoutPretendingTheStudySucceeded() {
        MemoryRepository repository = new MemoryRepository(study());
        MarketFactorStudyExecutionDomainService service = new MarketFactorStudyExecutionDomainService(repository,
                new MarketEventCohortProviderRegistry(List.of(provider(List.of()))), new MarketFactorStudyLifecyclePolicy());
        service.processNextQueuedStudy();
        assertEquals(MarketFactorResearchBO.StudyStatus.NO_DATA, repository.study.status());
    }

    @Test
    void failsWhenCohortLeaksAnEventAnchorAfterTheFrozenCutoff() {
        MemoryRepository repository = new MemoryRepository(study());
        MarketEventCohortProvider provider = provider(List.of(new MarketEventCohort.Sample("600000", "SH", LocalDate.of(2026, 8, 16),
                OffsetDateTime.parse("2026-08-16T09:30:00+08:00"), "test", Map.of())));
        MarketFactorStudyExecutionDomainService service = new MarketFactorStudyExecutionDomainService(repository,
                new MarketEventCohortProviderRegistry(List.of(provider)), new MarketFactorStudyLifecyclePolicy());

        service.processNextQueuedStudy();

        assertEquals(MarketFactorResearchBO.StudyStatus.FAILED, repository.study.status());
        assertTrue(repository.study.warnings().contains("STUDY_COHORT_RESOLUTION_FAILED:IllegalArgumentException"));
    }

    private MarketEventCohortProvider provider(List<MarketEventCohort.Sample> samples) {
        return new MarketEventCohortProvider() {
            @Override public MarketFactorResearchBO.EventType eventType() { return MarketFactorResearchBO.EventType.LIMIT_UP_CLOSE; }
            @Override public int definitionVersion() { return 1; }
            @Override public MarketEventCohort resolve(MarketEventCohortQuery query) {
                return new MarketEventCohort(eventType(), 1, query.startDate(), query.endDate(), OffsetDateTime.now(), samples, Map.of(), List.of());
            }
        };
    }

    private MarketFactorResearchBO.Study study() {
        return new MarketFactorResearchBO.Study("study-1", "tenant", "owner", new MarketFactorResearchBO.StudyConfig(
                MarketFactorResearchBO.EventType.LIMIT_UP_CLOSE, 1, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14),
                List.of(1, 5), 2, 60, 120, List.of("CHANGE_POINT")), MarketFactorResearchBO.StudyStatus.QUEUED,
                OffsetDateTime.parse("2026-08-15T00:00:00Z"), "v1", true, 0, 0, List.of());
    }

    private static final class MemoryRepository implements MarketFactorStudyRepository {
        private MarketFactorResearchBO.Study study;
        private final List<MarketFactorResearchBO.StudySample> samples = new ArrayList<>();
        private MemoryRepository(MarketFactorResearchBO.Study study) { this.study = study; }
        @Override public Optional<MarketFactorResearchBO.Study> findById(String tenant, String owner, String studyId) { return Optional.of(study); }
        @Override public Optional<MarketFactorResearchBO.Study> findByIdempotencyKey(String tenant, String owner, String key) { return Optional.empty(); }
        @Override public List<MarketFactorResearchBO.Study> listByOwner(String tenant, String owner, int offset, int limit) { return List.of(study); }
        @Override public void save(MarketFactorResearchBO.Study value, String key) { study = value; }
        @Override public Optional<MarketFactorResearchBO.ClaimedStudy> claimNextQueued() {
            if (study.status() != MarketFactorResearchBO.StudyStatus.QUEUED) return Optional.empty();
            study = new MarketFactorResearchBO.Study(study.id(), study.tenantId(), study.ownerId(), study.config(), MarketFactorResearchBO.StudyStatus.RUNNING,
                    study.dataCutoffAt(), study.featureRegistryVersion(), study.leakageCheckPassed(), 0, 0, study.warnings());
            return Optional.of(new MarketFactorResearchBO.ClaimedStudy(study.id(), study.tenantId(), study.ownerId()));
        }
        @Override public void saveSamples(String tenant, String owner, String studyId, List<MarketFactorResearchBO.StudySample> values) { samples.addAll(values); }
        @Override public List<MarketFactorResearchBO.StudySample> listSamples(String tenant, String owner, String studyId,
                                                                               MarketFactorResearchBO.SampleType sampleType, int offset, int limit) { return List.copyOf(samples); }
        @Override public void cancel(String tenant, String owner, String studyId) { }
        @Override public void complete(MarketFactorResearchBO.Study value) { study = value; }
    }
}
