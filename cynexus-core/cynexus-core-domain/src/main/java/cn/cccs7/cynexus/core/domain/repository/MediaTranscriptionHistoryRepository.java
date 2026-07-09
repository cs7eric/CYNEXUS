package cn.cccs7.cynexus.core.domain.repository;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionHistoryBO;
import java.util.List;

public interface MediaTranscriptionHistoryRepository {

    List<MediaTranscriptionHistoryBO> listActive();

    MediaTranscriptionHistoryBO findById(Long id);

    MediaTranscriptionHistoryBO findByTaskId(String taskId);

    Long create(MediaTranscriptionHistoryBO history);

    boolean update(MediaTranscriptionHistoryBO history);

    boolean updateSortOrder(Long id, Integer sortOrder);

    boolean delete(Long id);
}
