package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionHistoryBO;
import cn.cccs7.cynexus.core.domain.repository.MediaTranscriptionHistoryRepository;
import cn.cccs7.cynexus.core.infra.basic.service.MediaTranscriptionHistoryBasicService;
import cn.cccs7.cynexus.core.infra.convert.MediaTranscriptionHistoryPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MediaTranscriptionHistoryRepositoryImpl implements MediaTranscriptionHistoryRepository {

    private final MediaTranscriptionHistoryBasicService basicService;

    public MediaTranscriptionHistoryRepositoryImpl(MediaTranscriptionHistoryBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<MediaTranscriptionHistoryBO> listActive() {
        return MediaTranscriptionHistoryPOConverter.INSTANCE.toTargetList(basicService.listActive());
    }

    @Override
    public MediaTranscriptionHistoryBO findById(Long id) {
        return Optional.ofNullable(basicService.getActiveById(id))
                .map(MediaTranscriptionHistoryPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public MediaTranscriptionHistoryBO findByTaskId(String taskId) {
        return Optional.ofNullable(basicService.getActiveByTaskId(taskId))
                .map(MediaTranscriptionHistoryPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(MediaTranscriptionHistoryBO history) {
        return basicService.create(MediaTranscriptionHistoryPOConverter.INSTANCE.toSource(history));
    }

    @Override
    public boolean update(MediaTranscriptionHistoryBO history) {
        return basicService.update(MediaTranscriptionHistoryPOConverter.INSTANCE.toSource(history));
    }

    @Override
    public boolean updateSortOrder(Long id, Integer sortOrder) {
        return basicService.updateSortOrder(id, sortOrder);
    }

    @Override
    public boolean delete(Long id) {
        return basicService.softDelete(id);
    }
}
