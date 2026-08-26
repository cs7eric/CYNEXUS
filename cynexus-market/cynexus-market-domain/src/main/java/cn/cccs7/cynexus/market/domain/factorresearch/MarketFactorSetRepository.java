package cn.cccs7.cynexus.market.domain.factorresearch;

import java.util.List;
import java.util.Optional;

public interface MarketFactorSetRepository {
    Optional<MarketFactorSetBO.Definition> find(String tenantId, String ownerId, String code);
    default Optional<MarketFactorSetBO.Definition> findByVersionId(String tenantId, String ownerId, long versionId) {
        return list(tenantId, ownerId, 0, 200).stream().filter(value -> value.activeVersion() != null && value.activeVersion().id() == versionId).findFirst();
    }
    List<MarketFactorSetBO.Definition> list(String tenantId, String ownerId, int offset, int limit);
    boolean insertDefinition(MarketFactorSetBO.Definition definition);
    boolean updateDefinition(MarketFactorSetBO.Definition definition, long expectedVersion);
    boolean insertVersion(long definitionId, MarketFactorSetBO.Version version);
    boolean retireDraftVersions(long definitionId);
    boolean updateVersionStatus(long definitionId, int versionNo, MarketFactorSetBO.VersionStatus status,
                                java.util.Map<String,Object> validationSnapshot);
    boolean activateVersion(long definitionId, String tenantId, String ownerId, long versionId);
}
