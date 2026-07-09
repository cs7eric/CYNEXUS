package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.AStockSkillDataBO;

public interface AStockSkillDataRepository {

    AStockSkillDataBO.DatasetListBO listDatasets();

    AStockSkillDataBO.RowsBO listRows(String datasetCode, Integer limit, String code, String source);
}
