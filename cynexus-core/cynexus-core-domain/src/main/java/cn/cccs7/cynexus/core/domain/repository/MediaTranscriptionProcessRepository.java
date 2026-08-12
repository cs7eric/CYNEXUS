package cn.cccs7.cynexus.core.domain.repository;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProcessBO;
import java.util.List;

public interface MediaTranscriptionProcessRepository {

    MediaTranscriptionProcessBO findById(String ownerKey, Long id);

    MediaTranscriptionProcessBO findByIdempotencyKey(String ownerKey, String idempotencyKey);

    List<MediaTranscriptionProcessBO> listActive(String ownerKey, int limit);

    List<MediaTranscriptionProcessBO> listActive(String ownerKey, int limit, int offset);

    long countActive(String ownerKey);

    Long create(MediaTranscriptionProcessBO process);

    boolean update(MediaTranscriptionProcessBO process);

    MediaTranscriptionProcessBO claimNext();
}
