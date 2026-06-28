package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptBO;
import cn.cccs7.cynexus.core.domain.repository.MediaTranscriptRepository;
import cn.cccs7.cynexus.core.infra.basic.service.MediaTranscriptBasicService;
import cn.cccs7.cynexus.core.infra.convert.MediaTranscriptPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MediaTranscriptRepositoryImpl implements MediaTranscriptRepository {

    private final MediaTranscriptBasicService mediaTranscriptBasicService;

    public MediaTranscriptRepositoryImpl(MediaTranscriptBasicService mediaTranscriptBasicService) {
        this.mediaTranscriptBasicService = mediaTranscriptBasicService;
    }

    @Override
    public List<MediaTranscriptBO> list() {
        return MediaTranscriptPOConverter.INSTANCE.toTargetList(mediaTranscriptBasicService.listAll());
    }

    @Override
    public MediaTranscriptBO findById(Long id) {
        return Optional.ofNullable(mediaTranscriptBasicService.getById(id))
                .map(MediaTranscriptPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(MediaTranscriptBO value) {
        return mediaTranscriptBasicService.create(MediaTranscriptPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(MediaTranscriptBO value) {
        return mediaTranscriptBasicService.update(MediaTranscriptPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return mediaTranscriptBasicService.delete(id);
    }
}
