package cn.cccs7.cynexus.core.domain.mediatranscription.impl;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptBO;
import cn.cccs7.cynexus.core.domain.repository.MediaTranscriptRepository;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MediaTranscriptDomainServiceImpl implements MediaTranscriptDomainService {

    private final MediaTranscriptRepository mediaTranscriptRepository;

    public MediaTranscriptDomainServiceImpl(MediaTranscriptRepository mediaTranscriptRepository) {
        this.mediaTranscriptRepository = mediaTranscriptRepository;
    }

    @Override
    public List<MediaTranscriptBO> list() {
        return mediaTranscriptRepository.list();
    }

    @Override
    public MediaTranscriptBO findById(Long id) {
        return mediaTranscriptRepository.findById(id);
    }

    @Override
    public Long create(MediaTranscriptBO value) {
        return mediaTranscriptRepository.create(value);
    }

    @Override
    public boolean update(MediaTranscriptBO value) {
        return mediaTranscriptRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return mediaTranscriptRepository.delete(id);
    }
}
