package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionHistoryBO;
import cn.cccs7.cynexus.core.infra.basic.entity.MediaTranscriptionHistoryPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MediaTranscriptionHistoryPOConverter extends BaseMapper<MediaTranscriptionHistoryPO, MediaTranscriptionHistoryBO> {

    MediaTranscriptionHistoryPOConverter INSTANCE = Mappers.getMapper(MediaTranscriptionHistoryPOConverter.class);
}
