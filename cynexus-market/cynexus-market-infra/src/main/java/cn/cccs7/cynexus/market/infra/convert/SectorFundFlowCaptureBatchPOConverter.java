package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.SectorFundFlowCaptureBatchBO;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorFundFlowCaptureBatchPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SectorFundFlowCaptureBatchPOConverter
        extends BaseMapper<SectorFundFlowCaptureBatchPO, SectorFundFlowCaptureBatchBO> {

    SectorFundFlowCaptureBatchPOConverter INSTANCE = Mappers.getMapper(SectorFundFlowCaptureBatchPOConverter.class);
}
