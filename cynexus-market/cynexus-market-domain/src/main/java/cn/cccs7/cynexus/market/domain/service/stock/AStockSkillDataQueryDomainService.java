package cn.cccs7.cynexus.market.domain.service.stock;

import cn.cccs7.cynexus.market.domain.entity.AStockSkillDataBO;

public interface AStockSkillDataQueryDomainService {

    AStockSkillDataBO.DatasetListBO listDatasets();

    AStockSkillDataBO.RowsBO listRows(String datasetCode, Integer limit, String code, String source);
}
