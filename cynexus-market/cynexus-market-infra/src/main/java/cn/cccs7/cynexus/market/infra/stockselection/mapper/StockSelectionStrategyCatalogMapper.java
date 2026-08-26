package cn.cccs7.cynexus.market.infra.stockselection.mapper;

import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionStrategyCatalogPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockSelectionStrategyCatalogMapper {

    List<StockSelectionStrategyCatalogPO> findActiveStrategies();

    List<StockSelectionStrategyCatalogPO> findActiveStrategiesForScope(@Param("tenantId") String tenantId,
                                                                         @Param("ownerId") String ownerId);

    int insertUserDefinition(@Param("strategy") cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyCatalogBO.Strategy strategy,
                             @Param("tenantId") String tenantId, @Param("ownerId") String ownerId);

    int insertVersion(@Param("strategy") cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyCatalogBO.Strategy strategy);

    int updateUserDefinition(@Param("strategy") cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyCatalogBO.Strategy strategy,
                             @Param("tenantId") String tenantId, @Param("ownerId") String ownerId,
                             @Param("expectedDefinitionVersion") long expectedDefinitionVersion);

    int retireActiveVersions(@Param("definitionId") long definitionId);
}
