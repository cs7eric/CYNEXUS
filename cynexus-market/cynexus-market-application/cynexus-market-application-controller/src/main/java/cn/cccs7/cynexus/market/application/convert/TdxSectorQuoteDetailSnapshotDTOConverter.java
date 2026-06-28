package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.TdxSectorQuoteDetailSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.TdxSectorQuoteDetailSnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TdxSectorQuoteDetailSnapshotDTOConverter extends BaseMapper<TdxSectorQuoteDetailSnapshotDTO, TdxSectorQuoteDetailSnapshotBO> {

    TdxSectorQuoteDetailSnapshotDTOConverter INSTANCE = Mappers.getMapper(TdxSectorQuoteDetailSnapshotDTOConverter.class);
}
