package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.AStockSkillDataDTO;
import cn.cccs7.cynexus.market.application.convert.AStockSkillDataDTOConverter;
import cn.cccs7.cynexus.market.domain.repository.AStockSkillDataRepository;
import org.springframework.stereotype.Service;

@Service
public class AStockSkillDataAppService {

    private final AStockSkillDataRepository aStockSkillDataRepository;

    public AStockSkillDataAppService(AStockSkillDataRepository aStockSkillDataRepository) {
        this.aStockSkillDataRepository = aStockSkillDataRepository;
    }

    public AStockSkillDataDTO.DatasetListDTO listDatasets() {
        return AStockSkillDataDTOConverter.INSTANCE.toDatasetListDTO(aStockSkillDataRepository.listDatasets());
    }

    public AStockSkillDataDTO.RowsDTO listRows(String datasetCode, Integer limit, String code, String source) {
        return AStockSkillDataDTOConverter.INSTANCE.toRowsDTO(
                aStockSkillDataRepository.listRows(datasetCode, limit, code, source)
        );
    }
}
