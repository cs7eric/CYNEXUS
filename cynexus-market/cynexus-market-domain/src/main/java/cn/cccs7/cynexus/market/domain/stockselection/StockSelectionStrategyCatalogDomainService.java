package cn.cccs7.cynexus.market.domain.stockselection;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockSelectionStrategyCatalogDomainService {

    private final StockSelectionStrategyCatalogRepository repository;
    private final StockSelectionStrategyParameterPolicy parameterPolicy;
    private final StockSelectionIdGenerator ids;

    @Autowired
    public StockSelectionStrategyCatalogDomainService(StockSelectionStrategyCatalogRepository repository,
                                                       StockSelectionStrategyParameterPolicy parameterPolicy,
                                                       StockSelectionIdGenerator ids) {
        this.repository = repository;
        this.parameterPolicy = parameterPolicy;
        this.ids = ids;
    }

    /** Compatibility constructor for pure read-only domain tests. */
    StockSelectionStrategyCatalogDomainService(StockSelectionStrategyCatalogRepository repository) {
        this(repository, new StockSelectionStrategyParameterPolicy(), () -> {
            throw new UnsupportedOperationException("strategy IDs are unavailable in this read-only catalog");
        });
    }

    public StockSelectionStrategyCatalogBO.Catalog listActive() {
        List<StockSelectionStrategyCatalogBO.Strategy> items = repository.findActiveStrategies();
        return new StockSelectionStrategyCatalogBO.Catalog(items == null ? List.of() : List.copyOf(items));
    }

    public StockSelectionStrategyCatalogBO.Catalog listActive(StockSelectionStrategyRunBO.Scope scope) {
        requireScope(scope);
        List<StockSelectionStrategyCatalogBO.Strategy> items = repository.findActiveStrategies(scope);
        return new StockSelectionStrategyCatalogBO.Catalog(items == null ? List.of() : List.copyOf(items));
    }

    public StockSelectionStrategyCatalogBO.Strategy createUserStrategy(StockSelectionStrategyRunBO.Scope scope,
                                                                        StockSelectionStrategyCatalogBO.SaveCommand command) {
        requireScope(scope);
        StockSelectionStrategyParameterPolicy.Prepared criteria = prepare(command);
        long definitionId = ids.nextId();
        StockSelectionStrategyCatalogBO.Strategy strategy = new StockSelectionStrategyCatalogBO.Strategy(definitionId,
                "USER_" + definitionId, required(command.name(), "name", 128), description(command.description()), "USER_SCREENER",
                new StockSelectionStrategyCatalogBO.Version(ids.nextId(), 1, criteria.parameterSchema(), criteria.evidenceSchema(),
                        required(command.changeSummary(), "changeSummary", 1000), java.time.LocalDate.now(), 0,
                        command.sourceFactorStudyId(), command.sourceFactorSetVersionId()));
        if (!repository.createUserStrategy(scope, strategy)) throw new IllegalStateException("strategy could not be created");
        return strategy;
    }

    public StockSelectionStrategyCatalogBO.Strategy updateUserStrategy(StockSelectionStrategyRunBO.Scope scope,
                                                                        String strategyCode,
                                                                        StockSelectionStrategyCatalogBO.SaveCommand command) {
        requireScope(scope);
        if (command == null || command.expectedDefinitionVersion() == null || command.expectedDefinitionVersion() < 0)
            throw new IllegalArgumentException("expectedDefinitionVersion is required");
        StockSelectionStrategyCatalogBO.Strategy current = listActive(scope).items().stream()
                .filter(item -> "USER_SCREENER".equals(item.type()))
                .filter(item -> item.code().equalsIgnoreCase(normalizeCode(strategyCode))).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("user strategy is unavailable"));
        if (current.activeVersion().definitionVersion() != command.expectedDefinitionVersion())
            throw new IllegalStateException("strategy was changed concurrently");
        StockSelectionStrategyParameterPolicy.Prepared criteria = prepare(command);
        StockSelectionStrategyCatalogBO.Strategy next = new StockSelectionStrategyCatalogBO.Strategy(current.definitionId(), current.code(),
                required(command.name(), "name", 128), description(command.description()), current.type(),
                new StockSelectionStrategyCatalogBO.Version(ids.nextId(), current.activeVersion().versionNo() + 1,
                        criteria.parameterSchema(), criteria.evidenceSchema(), required(command.changeSummary(), "changeSummary", 1000),
                        java.time.LocalDate.now(), current.activeVersion().definitionVersion() + 1,
                        command.sourceFactorStudyId(), command.sourceFactorSetVersionId()));
        if (!repository.appendUserStrategyVersion(scope, next, command.expectedDefinitionVersion()))
            throw new IllegalStateException("strategy was changed concurrently");
        return next;
    }

    private StockSelectionStrategyParameterPolicy.Prepared prepare(StockSelectionStrategyCatalogBO.SaveCommand command) {
        if (command == null) throw new IllegalArgumentException("strategy request is required");
        return parameterPolicy.prepare(command.criteria());
    }

    private void requireScope(StockSelectionStrategyRunBO.Scope scope) {
        if (scope == null || scope.tenantId() == null || scope.tenantId().isBlank() || scope.ownerId() == null || scope.ownerId().isBlank())
            throw new IllegalArgumentException("stock selection scope is required");
    }

    private String required(String value, String field, int maxLength) {
        String normalized = optional(value, maxLength);
        if (normalized == null) throw new IllegalArgumentException(field + " is required and must be at most " + maxLength + " characters");
        return normalized;
    }

    private String optional(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException("strategy text must be at most " + maxLength + " characters");
        return normalized;
    }

    private String description(String value) {
        String normalized = optional(value, 1000);
        return normalized == null ? "" : normalized;
    }

    private String normalizeCode(String value) { return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT); }
}
