package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.MaCountDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.MaCountDailyBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MaCountDailyDTOConverter extends BaseMapper<MaCountDailyDTO, MaCountDailyBO> {

    MaCountDailyDTOConverter INSTANCE = Mappers.getMapper(MaCountDailyDTOConverter.class);
}
