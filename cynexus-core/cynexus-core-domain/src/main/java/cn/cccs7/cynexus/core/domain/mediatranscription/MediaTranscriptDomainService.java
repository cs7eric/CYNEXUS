package cn.cccs7.cynexus.core.domain.mediatranscription;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptBO;
import java.util.List;

public interface MediaTranscriptDomainService {

    List<MediaTranscriptBO> list();

    MediaTranscriptBO findById(Long id);

    Long create(MediaTranscriptBO value);

    boolean update(MediaTranscriptBO value);

    boolean delete(Long id);
}
