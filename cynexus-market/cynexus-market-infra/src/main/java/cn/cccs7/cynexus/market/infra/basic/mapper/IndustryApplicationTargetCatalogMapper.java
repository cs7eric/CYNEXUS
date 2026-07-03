package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.market.infra.basic.entity.IndustryApplicationTargetCatalogPO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IndustryApplicationTargetCatalogMapper {

    List<IndustryApplicationTargetCatalogPO> listAll();

    List<IndustryApplicationTargetCatalogPO> listBySymbols(@Param("symbols") List<String> symbols);

    IndustryApplicationTargetCatalogPO findBySymbol(@Param("symbol") String symbol);

    int upsert(@Param("value") IndustryApplicationTargetCatalogPO value);
}
