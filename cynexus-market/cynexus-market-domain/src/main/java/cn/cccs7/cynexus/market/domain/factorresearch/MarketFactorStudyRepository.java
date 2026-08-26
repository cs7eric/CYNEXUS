package cn.cccs7.cynexus.market.domain.factorresearch;

import java.util.Optional;
import java.util.List;
import java.util.Map;

/** Persistence port for owner-scoped, asynchronous market-factor studies. */
public interface MarketFactorStudyRepository {
    Optional<MarketFactorResearchBO.Study> findById(String tenantId, String ownerId, String studyId);
    Optional<MarketFactorResearchBO.Study> findByIdempotencyKey(String tenantId, String ownerId, String idempotencyKey);
    List<MarketFactorResearchBO.Study> listByOwner(String tenantId, String ownerId, int offset, int limit);
    void save(MarketFactorResearchBO.Study study, String idempotencyKey);
    Optional<MarketFactorResearchBO.ClaimedStudy> claimNextQueued();
    void saveSamples(String tenantId, String ownerId, String studyId, List<MarketFactorResearchBO.StudySample> samples);
    default void updateProgress(String tenantId, String ownerId, String studyId, Map<String, Object> progress) { }
    default boolean isRunning(String tenantId, String ownerId, String studyId) { return true; }
    default Map<String, Object> findProgress(String tenantId, String ownerId, String studyId) { return Map.of(); }
    default void saveSequencePoints(String tenantId, String ownerId, String studyId, List<MarketFactorResearchBO.SequencePoint> points) { }
    default List<MarketFactorResearchBO.SequencePoint> listSequencePoints(String tenantId, String ownerId, String studyId,
                                                                           String sequenceCode, int offset, int limit) { return List.of(); }
    default void saveFactorResults(String tenantId, String ownerId, String studyId, List<MarketFactorResearchBO.FactorResult> results) { }
    default void saveOutcomes(String tenantId, String ownerId, String studyId, MarketFactorResearchBO.SampleType sampleType,
                              String code, String market, java.time.LocalDate eventTradeDate, List<Map<String, Object>> outcomes) { }
    default void saveOutcomesBatch(String tenantId, String ownerId, String studyId, List<Map<String, Object>> outcomes) { }
    default List<MarketFactorResearchBO.FactorResult> listFactorResults(String tenantId, String ownerId, String studyId, int offset, int limit) { return List.of(); }
    default java.util.Optional<MarketFactorResearchBO.FactorResult> findFactorResult(String tenantId, String ownerId, String studyId, String discoveryCode) {
        return listFactorResults(tenantId, ownerId, studyId, 0, 1000).stream().filter(v -> v.discoveryCode().equals(discoveryCode)).findFirst();
    }
    /** Resolves an immutable, promoted discovery recipe for bounded screening. */
    default java.util.Optional<MarketFactorResearchBO.FactorResult> findPromotedFactorResult(String tenantId, String ownerId,
                                                                                                String studyId, String discoveryCode) {
        return findFactorResult(tenantId, ownerId, studyId, discoveryCode)
                .filter(value -> "PROMOTED".equals(value.verdict()));
    }
    default List<java.util.Map<String, Object>> listOutcomes(String tenantId, String ownerId, String studyId, int offset, int limit) { return List.of(); }
    default void promoteFactor(String tenantId, String ownerId, String studyId, String discoveryCode) { }
    List<MarketFactorResearchBO.StudySample> listSamples(String tenantId, String ownerId, String studyId,
                                                          MarketFactorResearchBO.SampleType sampleType, int offset, int limit);
    void cancel(String tenantId, String ownerId, String studyId);
    void complete(MarketFactorResearchBO.Study study);
}
