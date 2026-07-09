package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.market.api.dto.AStockSkillDataDTO;
import cn.cccs7.cynexus.market.domain.entity.AStockSkillDataBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AStockSkillDataDTOConverter {

    AStockSkillDataDTOConverter INSTANCE = Mappers.getMapper(AStockSkillDataDTOConverter.class);

    AStockSkillDataDTO.DatasetListDTO toDatasetListDTO(AStockSkillDataBO.DatasetListBO source);

    AStockSkillDataDTO.RowsDTO toRowsDTO(AStockSkillDataBO.RowsBO source);
}
