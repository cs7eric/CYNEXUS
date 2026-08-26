package cn.cccs7.cynexus.market.infra.stockselection;

import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyCatalogBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyCatalogRepository;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionStrategyCatalogPO;
import cn.cccs7.cynexus.market.infra.stockselection.mapper.StockSelectionStrategyCatalogMapper;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class StockSelectionStrategyCatalogRepositoryAdapter implements StockSelectionStrategyCatalogRepository {

    private final StockSelectionStrategyCatalogMapper mapper;

    public StockSelectionStrategyCatalogRepositoryAdapter(StockSelectionStrategyCatalogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<StockSelectionStrategyCatalogBO.Strategy> findActiveStrategies() {
        return mapper.findActiveStrategies().stream().map(this::toDomain).toList();
    }

    @Override
    public List<StockSelectionStrategyCatalogBO.Strategy> findActiveStrategies(StockSelectionStrategyRunBO.Scope scope) {
        return mapper.findActiveStrategiesForScope(scope.tenantId(), scope.ownerId()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createUserStrategy(StockSelectionStrategyRunBO.Scope scope, StockSelectionStrategyCatalogBO.Strategy strategy) {
        return mapper.insertUserDefinition(strategy, scope.tenantId(), scope.ownerId()) == 1 && mapper.insertVersion(strategy) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean appendUserStrategyVersion(StockSelectionStrategyRunBO.Scope scope,
                                             StockSelectionStrategyCatalogBO.Strategy strategy,
                                             long expectedDefinitionVersion) {
        if (mapper.updateUserDefinition(strategy, scope.tenantId(), scope.ownerId(), expectedDefinitionVersion) != 1) return false;
        if (mapper.retireActiveVersions(strategy.definitionId()) < 1) throw new IllegalStateException("active user strategy version is unavailable");
        return mapper.insertVersion(strategy) == 1;
    }

    private StockSelectionStrategyCatalogBO.Strategy toDomain(StockSelectionStrategyCatalogPO value) {
        return new StockSelectionStrategyCatalogBO.Strategy(value.getDefinitionId(), value.getCode(), value.getName(), value.getDescription(),
                value.getType(), new StockSelectionStrategyCatalogBO.Version(value.getVersionId(), value.getVersionNo(),
                value.getParameterSchema(), value.getEvidenceSchema(), value.getChangeSummary(), value.getEffectiveFrom(), value.getDefinitionVersion(),
                value.getSourceFactorStudyId(), value.getSourceFactorSetVersionId()));
    }
}
