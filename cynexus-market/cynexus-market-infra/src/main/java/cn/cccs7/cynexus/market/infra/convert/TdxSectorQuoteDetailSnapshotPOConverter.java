package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.TdxSectorQuoteDetailSnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxSectorQuoteDetailSnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TdxSectorQuoteDetailSnapshotPOConverter extends BaseMapper<TdxSectorQuoteDetailSnapshotPO, TdxSectorQuoteDetailSnapshotBO> {

    TdxSectorQuoteDetailSnapshotPOConverter INSTANCE = Mappers.getMapper(TdxSectorQuoteDetailSnapshotPOConverter.class);
}
