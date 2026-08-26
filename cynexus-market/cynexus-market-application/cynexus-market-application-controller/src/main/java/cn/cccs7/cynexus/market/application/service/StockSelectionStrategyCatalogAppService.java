package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockSelectionStrategyDTO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyCatalogBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyCatalogDomainService;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import org.springframework.stereotype.Service;

@Service
public class StockSelectionStrategyCatalogAppService {

    private final StockSelectionStrategyCatalogDomainService domainService;

    public StockSelectionStrategyCatalogAppService(StockSelectionStrategyCatalogDomainService domainService) {
        this.domainService = domainService;
    }

    public StockSelectionStrategyDTO.CatalogResponse listActive() {
        return StockSelectionStrategyDTO.CatalogResponse.builder()
                .items(domainService.listActive().items().stream().map(this::strategy).toList())
                .build();
    }

    public StockSelectionStrategyDTO.CatalogResponse listActive(StockSelectionStrategyRunBO.Scope scope) {
        return StockSelectionStrategyDTO.CatalogResponse.builder()
                .items(domainService.listActive(scope).items().stream().map(this::strategy).toList())
                .build();
    }

    public StockSelectionStrategyDTO.Strategy create(StockSelectionStrategyRunBO.Scope scope,
                                                       StockSelectionStrategyDTO.SaveRequest request) {
        return strategy(domainService.createUserStrategy(scope, command(request)));
    }

    public StockSelectionStrategyDTO.Strategy update(StockSelectionStrategyRunBO.Scope scope, String strategyCode,
                                                       StockSelectionStrategyDTO.SaveRequest request) {
        return strategy(domainService.updateUserStrategy(scope, strategyCode, command(request)));
    }

    private StockSelectionStrategyDTO.Strategy strategy(StockSelectionStrategyCatalogBO.Strategy value) {
        StockSelectionStrategyCatalogBO.Version version = value.activeVersion();
        return StockSelectionStrategyDTO.Strategy.builder()
                .code(value.code()).name(value.name()).description(value.description()).type(value.type())
                .activeVersion(StockSelectionStrategyDTO.Version.builder()
                        .id(version.id()).versionNo(version.versionNo()).parameterSchema(version.parameterSchema())
                        .evidenceSchema(version.evidenceSchema()).changeSummary(version.changeSummary())
                        .effectiveFrom(version.effectiveFrom() == null ? null : version.effectiveFrom().toString())
                        .definitionVersion(version.definitionVersion())
                        .sourceFactorStudyId(version.sourceFactorStudyId())
                        .sourceFactorSetVersionId(version.sourceFactorSetVersionId())
                        .build())
                .build();
    }

    private StockSelectionStrategyCatalogBO.SaveCommand command(StockSelectionStrategyDTO.SaveRequest request) {
        return request == null ? null : new StockSelectionStrategyCatalogBO.SaveCommand(request.getName(), request.getDescription(),
                request.getCriteria(), request.getChangeSummary(), request.getExpectedDefinitionVersion(),
                request.getSourceFactorStudyId(), request.getSourceFactorSetVersionId());
    }
}
