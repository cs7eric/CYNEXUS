package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.AStockSkillDataDTO;
import cn.cccs7.cynexus.market.application.convert.AStockSkillDataDTOConverter;
import cn.cccs7.cynexus.market.domain.service.stock.AStockSkillDataQueryDomainService;
import org.springframework.stereotype.Service;

@Service
public class AStockSkillDataAppService {

    private final AStockSkillDataQueryDomainService domainService;

    public AStockSkillDataAppService(AStockSkillDataQueryDomainService domainService) {
        this.domainService = domainService;
    }

    public AStockSkillDataDTO.DatasetListDTO listDatasets() {
        return AStockSkillDataDTOConverter.INSTANCE.toDatasetListDTO(domainService.listDatasets());
    }

    public AStockSkillDataDTO.RowsDTO listRows(String datasetCode, Integer limit, String code, String source) {
        return AStockSkillDataDTOConverter.INSTANCE.toRowsDTO(
                domainService.listRows(datasetCode, limit, code, source)
        );
    }
}
