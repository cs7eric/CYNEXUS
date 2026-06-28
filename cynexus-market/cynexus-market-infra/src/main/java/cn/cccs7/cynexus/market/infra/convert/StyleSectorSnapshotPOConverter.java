package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.StyleSectorSnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StyleSectorSnapshotPOConverter extends BaseMapper<StyleSectorSnapshotPO, StyleSectorSnapshotBO> {

    StyleSectorSnapshotPOConverter INSTANCE = Mappers.getMapper(StyleSectorSnapshotPOConverter.class);
}
