package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.convert.MediaTranscriptDTOConverter;
import cn.cccs7.cynexus.core.api.dto.MediaTranscriptDTO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptBO;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MediaTranscriptCrudAppService {

    private final MediaTranscriptDomainService mediaTranscriptDomainService;

    public MediaTranscriptCrudAppService(MediaTranscriptDomainService mediaTranscriptDomainService) {
        this.mediaTranscriptDomainService = mediaTranscriptDomainService;
    }

    public List<MediaTranscriptDTO> list() {
        return MediaTranscriptDTOConverter.INSTANCE.toSourceList(mediaTranscriptDomainService.list());
    }

    public MediaTranscriptDTO findById(Long id) {
        MediaTranscriptBO value = mediaTranscriptDomainService.findById(id);
        return value == null ? null : MediaTranscriptDTOConverter.INSTANCE.toSource(value);
    }

    public Long create(MediaTranscriptDTO dto) {
        return mediaTranscriptDomainService.create(MediaTranscriptDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean update(MediaTranscriptDTO dto) {
        return mediaTranscriptDomainService.update(MediaTranscriptDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean delete(Long id) {
        return mediaTranscriptDomainService.delete(id);
    }
}
