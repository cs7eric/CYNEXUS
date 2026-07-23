package cn.cccs7.cynexus.market.domain.service.stock.impl;

import cn.cccs7.cynexus.market.domain.entity.AStockSkillDataBO;
import cn.cccs7.cynexus.market.domain.repository.AStockSkillDataRepository;
import cn.cccs7.cynexus.market.domain.service.stock.AStockSkillDataQueryDomainService;
import org.springframework.stereotype.Service;

@Service
public class AStockSkillDataQueryDomainServiceImpl implements AStockSkillDataQueryDomainService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;
    private final AStockSkillDataRepository repository;

    public AStockSkillDataQueryDomainServiceImpl(AStockSkillDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public AStockSkillDataBO.DatasetListBO listDatasets() {
        return repository.listDatasets();
    }

    @Override
    public AStockSkillDataBO.RowsBO listRows(String datasetCode, Integer limit, String code, String source) {
        int safeLimit = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return repository.listRows(datasetCode == null ? null : datasetCode.trim(), safeLimit,
                code == null ? null : code.trim(), source == null ? null : source.trim());
    }
}
