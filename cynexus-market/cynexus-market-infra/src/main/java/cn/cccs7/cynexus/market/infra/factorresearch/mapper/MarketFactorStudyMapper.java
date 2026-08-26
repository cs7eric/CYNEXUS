package cn.cccs7.cynexus.market.infra.factorresearch.mapper;

import cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorStudyPO;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorStudySamplePO;
import cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorStudySnapshotPO;
import cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorStudyFactorResultPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarketFactorStudyMapper {
    int insert(MarketFactorStudyPO value);
    MarketFactorStudyPO findByCode(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId,
                                   @Param("studyCode") String studyCode);
    MarketFactorStudyPO findByIdempotencyKey(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId,
                                             @Param("idempotencyKey") String idempotencyKey);
    java.util.List<MarketFactorStudyPO> listByOwner(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId,
                                                     @Param("offset") int offset, @Param("limit") int limit);
    MarketFactorStudyPO findNextQueued();
    int claim(@Param("id") long id, @Param("startedAt") OffsetDateTime startedAt);
    int updateProgress(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId, @Param("studyCode") String studyCode,
                       @Param("progress") String progress);
    String findStatus(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId, @Param("studyCode") String studyCode);
    String findProgress(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId, @Param("studyCode") String studyCode);
    Long findInternalIdByCode(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId, @Param("studyCode") String studyCode);
    int insertSample(@Param("id") long id, @Param("studyId") long studyId, @Param("sampleType") String sampleType,
                     @Param("stockCode") String stockCode, @Param("market") String market, @Param("eventTradeDate") LocalDate eventTradeDate,
                     @Param("anchorAt") OffsetDateTime anchorAt, @Param("eventType") String eventType,
                     @Param("matchingMetadata") String matchingMetadata, @Param("source") String source, @Param("coverage") String coverage);
    int insertSamples(@Param("studyId") long studyId, @Param("samples") java.util.List<java.util.Map<String, Object>> samples);
    java.util.List<MarketFactorStudySamplePO> listSamples(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId,
                                                           @Param("studyCode") String studyCode, @Param("sampleType") String sampleType,
                                                           @Param("offset") int offset, @Param("limit") int limit);
    int cancel(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId,
               @Param("studyCode") String studyCode, @Param("cancelledAt") OffsetDateTime cancelledAt);
    int complete(MarketFactorStudyPO value);
    java.util.List<MarketFactorStudySnapshotPO> findPreEventUniverse(@Param("eventDate") LocalDate eventDate,
                                                                       @Param("dataCutoffAt") OffsetDateTime dataCutoffAt,
                                                                       @Param("featureVersion") String featureVersion);
    java.util.List<MarketFactorStudySnapshotPO> findDailyPrimitives(@Param("code") String code, @Param("market") String market,
            @Param("eventDate") LocalDate eventDate, @Param("sessions") int sessions, @Param("dataCutoffAt") OffsetDateTime dataCutoffAt,
            @Param("featureVersion") String featureVersion);
    Long findSampleInternalId(@Param("studyId") long studyId, @Param("sampleType") String sampleType, @Param("code") String code,
                              @Param("market") String market, @Param("eventTradeDate") LocalDate eventTradeDate);
    int insertSequencePoint(@Param("id") long id, @Param("studyId") long studyId, @Param("sampleId") long sampleId,
            @Param("frequency") String frequency, @Param("sequenceCode") String sequenceCode, @Param("offsetValue") int offsetValue,
            @Param("observedAt") OffsetDateTime observedAt, @Param("availableAt") OffsetDateTime availableAt,
            @Param("rawValue") java.math.BigDecimal rawValue, @Param("normalizedValue") java.math.BigDecimal normalizedValue,
            @Param("source") String source);
    int insertSequencePoints(@Param("studyId") long studyId, @Param("points") java.util.List<java.util.Map<String, Object>> points);
    java.util.List<cn.cccs7.cynexus.market.infra.factorresearch.entity.MarketFactorStudySequencePointPO> listSequencePoints(
            @Param("tenantId") String tenantId, @Param("ownerId") String ownerId, @Param("studyCode") String studyCode,
            @Param("sequenceCode") String sequenceCode, @Param("offset") int offset, @Param("limit") int limit);
    int insertFactorResult(@Param("id") long id, @Param("studyId") long studyId, @Param("discoveryCode") String discoveryCode,
            @Param("recipeVersion") int recipeVersion, @Param("method") String method, @Param("scope") String scope,
            @Param("recipe") String recipe, @Param("learnedBounds") String learnedBounds, @Param("statistics") String statistics,
            @Param("validation") String validation, @Param("evidence") String evidence, @Param("verdict") String verdict, @Param("rank") int rank);
    java.util.List<MarketFactorStudySnapshotPO> findOutcomeSeries(@Param("code") String code, @Param("market") String market,
            @Param("eventDate") LocalDate eventDate, @Param("sessions") int sessions, @Param("dataCutoffAt") OffsetDateTime dataCutoffAt,
            @Param("featureVersion") String featureVersion);
    int updateSampleOutcome(@Param("studyId") long studyId, @Param("sampleType") String sampleType, @Param("code") String code,
            @Param("market") String market, @Param("eventTradeDate") LocalDate eventTradeDate, @Param("outcome") String outcome);
    int updateSampleOutcomes(@Param("studyId") long studyId, @Param("outcomes") java.util.List<java.util.Map<String, Object>> outcomes);
    java.util.List<MarketFactorStudyFactorResultPO> listFactorResults(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId,
            @Param("studyCode") String studyCode, @Param("offset") int offset, @Param("limit") int limit);
    java.util.List<java.util.Map<String, Object>> listOutcomes(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId,
            @Param("studyCode") String studyCode, @Param("offset") int offset, @Param("limit") int limit);
    int promoteFactor(@Param("tenantId") String tenantId, @Param("ownerId") String ownerId, @Param("studyCode") String studyCode,
                      @Param("discoveryCode") String discoveryCode);
}
