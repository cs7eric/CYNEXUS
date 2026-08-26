package cn.cccs7.cynexus.market.domain.factorresearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketFactorSetDomainServiceTest {
    private final MemoryRepository repository = new MemoryRepository();
    private final MarketFactorSetDomainService service = new MarketFactorSetDomainService(repository, new IncrementingIds());
    private final MarketFactorSetBO.Scope scope = new MarketFactorSetBO.Scope("tenant", "owner");

    @Test void createsImmutableVersionsThenRequiresExplicitValidationAndPromotion() {
        MarketFactorSetBO.Definition created = service.create(scope, new MarketFactorSetBO.CreateCommand("breakout", "Breakout", expression("first"), 7L));
        assertEquals(1, created.activeVersion().versionNo());
        MarketFactorSetBO.Definition updated = service.update(scope, "breakout", new MarketFactorSetBO.UpdateCommand("Breakout v2", expression("second"), 7L, 0));
        assertEquals(2, updated.activeVersion().versionNo());
        assertThrows(IllegalStateException.class, () -> service.validate(scope, "breakout", 1, Map.of()));
        assertEquals(MarketFactorSetBO.VersionStatus.VALIDATED, service.validate(scope, "breakout", 2, Map.of("review", "ok")).status());
        assertEquals(MarketFactorSetBO.VersionStatus.PROMOTED, service.promote(scope, "breakout", 2).status());
    }

    @Test void rejectsUntypedExpressions() {
        assertThrows(IllegalArgumentException.class, () -> service.create(scope,
                new MarketFactorSetBO.CreateCommand("invalid", "Invalid", Map.of("operator", "AND", "clauses", List.of()), null)));
    }

    private Map<String, Object> expression(String value) {
        return Map.of("schemaVersion", 1, "operator", "AND", "clauses", List.of(Map.of("field", "discoveryCode", "operator", "EQUALS", "value", value, "weight", 1)));
    }

    private static final class IncrementingIds implements MarketFactorSetIdGenerator { private long id; @Override public long nextId() { return ++id; } }
    private static final class MemoryRepository implements MarketFactorSetRepository {
        private final Map<String, MarketFactorSetBO.Definition> definitions = new HashMap<>();
        @Override public Optional<MarketFactorSetBO.Definition> find(String tenantId, String ownerId, String code) { return Optional.ofNullable(definitions.get(code)); }
        @Override public List<MarketFactorSetBO.Definition> list(String tenantId, String ownerId, int offset, int limit) { return definitions.values().stream().toList(); }
        @Override public boolean insertDefinition(MarketFactorSetBO.Definition value) { definitions.put(value.code(), value); return true; }
        @Override public boolean updateDefinition(MarketFactorSetBO.Definition value, long expectedVersion) { MarketFactorSetBO.Definition current = definitions.get(value.code()); if (current == null || current.version() != expectedVersion) return false; definitions.put(value.code(), value); return true; }
        @Override public boolean insertVersion(long definitionId, MarketFactorSetBO.Version version) { return true; }
        @Override public boolean retireDraftVersions(long definitionId) { return true; }
        @Override public boolean updateVersionStatus(long definitionId, int versionNo, MarketFactorSetBO.VersionStatus status, Map<String, Object> snapshot) {
            for (Map.Entry<String, MarketFactorSetBO.Definition> entry : definitions.entrySet()) {
                MarketFactorSetBO.Definition definition = entry.getValue(); MarketFactorSetBO.Version current = definition.activeVersion();
                if (definition.id() == definitionId && current != null && current.versionNo() == versionNo) {
                    entry.setValue(new MarketFactorSetBO.Definition(definition.id(), definition.code(), definition.tenantId(), definition.ownerId(), definition.name(), definition.status(), definition.activeVersionId(), definition.version(), new MarketFactorSetBO.Version(current.id(), current.versionNo(), current.sourceStudyId(), current.expression(), snapshot, status, current.createdAt())));
                    return true;
                }
            }
            return false;
        }
        @Override public boolean activateVersion(long definitionId, String tenantId, String ownerId, long versionId) { return true; }
    }
}
