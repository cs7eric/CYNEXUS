package cn.cccs7.cynexus.market.domain.factorresearch;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

public final class MarketFactorSetBO {
    private MarketFactorSetBO() {}

    public record Scope(String tenantId, String ownerId) {}
    public enum DefinitionStatus { DRAFT, ACTIVE, RETIRED }
    public enum VersionStatus { DRAFT, VALIDATED, PROMOTED, RETIRED }

    public record Version(long id, int versionNo, Long sourceStudyId, Map<String,Object> expression,
                          Map<String,Object> validationSnapshot, VersionStatus status, Instant createdAt) {}
    public record Definition(long id, String code, String tenantId, String ownerId, String name,
                             DefinitionStatus status, Long activeVersionId, long version, Version activeVersion) {}
    public record CreateCommand(String code, String name, Map<String,Object> expression, Long sourceStudyId) {}
    public record UpdateCommand(String name, Map<String,Object> expression, Long sourceStudyId, long expectedVersion) {}
    public record Validation(Map<String,Object> snapshot) {}
    public record VersionSummary(long id, String code, int versionNo, VersionStatus status, Map<String,Object> expression,
                                 Map<String,Object> validationSnapshot) {}
    public record Preview(long factorSetVersionId, String code, int versionNo, LocalDate requestedTradeDate,
                          LocalDate snapshotTradeDate, String status, int count, List<PreviewItem> items,
                          List<String> warnings) {}
    public record PreviewItem(String code, String name, String market, String industry,
                              java.math.BigDecimal price, java.math.BigDecimal changePct, String reason) {}
}
