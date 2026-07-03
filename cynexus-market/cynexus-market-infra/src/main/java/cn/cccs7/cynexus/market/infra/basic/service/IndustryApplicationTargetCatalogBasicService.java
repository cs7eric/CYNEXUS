package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationTargetCatalogPO;
import java.util.List;
import java.util.Optional;

public interface IndustryApplicationTargetCatalogBasicService {

    List<IndustryApplicationTargetCatalogPO> listAll();

    List<IndustryApplicationTargetCatalogPO> listBySymbols(List<String> symbols);

    Optional<IndustryApplicationTargetCatalogPO> findBySymbol(String symbol);

    void upsertBatch(List<IndustryApplicationTargetCatalogPO> values);
}
