package cn.cccs7.cynexus.core.domain.repository;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptBO;
import java.util.List;

public interface MediaTranscriptRepository {

    List<MediaTranscriptBO> list();

    MediaTranscriptBO findById(Long id);

    Long create(MediaTranscriptBO value);

    boolean update(MediaTranscriptBO value);

    boolean delete(Long id);
}
