package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.SectorFundFlowCaptureBatchDTO;
import cn.cccs7.cynexus.market.domain.entity.SectorFundFlowCaptureBatchBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SectorFundFlowCaptureBatchDTOConverter
        extends BaseMapper<SectorFundFlowCaptureBatchDTO, SectorFundFlowCaptureBatchBO> {

    SectorFundFlowCaptureBatchDTOConverter INSTANCE =
            Mappers.getMapper(SectorFundFlowCaptureBatchDTOConverter.class);
}
