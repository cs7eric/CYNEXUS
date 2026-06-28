package cn.cccs7.cynexus.core.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.api.dto.MediaTranscriptDTO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MediaTranscriptDTOConverter extends BaseMapper<MediaTranscriptDTO, MediaTranscriptBO> {

    MediaTranscriptDTOConverter INSTANCE = Mappers.getMapper(MediaTranscriptDTOConverter.class);
}
