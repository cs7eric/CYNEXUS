package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptBO;
import cn.cccs7.cynexus.core.infra.basic.entity.MediaTranscriptPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MediaTranscriptPOConverter extends BaseMapper<MediaTranscriptPO, MediaTranscriptBO> {

    MediaTranscriptPOConverter INSTANCE = Mappers.getMapper(MediaTranscriptPOConverter.class);
}
