package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.StyleSectorSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StyleSectorSnapshotDTOConverter extends BaseMapper<StyleSectorSnapshotDTO, StyleSectorSnapshotBO> {

    StyleSectorSnapshotDTOConverter INSTANCE = Mappers.getMapper(StyleSectorSnapshotDTOConverter.class);
}
