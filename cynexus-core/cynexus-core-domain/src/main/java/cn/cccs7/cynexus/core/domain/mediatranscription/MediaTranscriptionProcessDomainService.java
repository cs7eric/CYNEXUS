package cn.cccs7.cynexus.core.domain.mediatranscription;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProcessBO;
import java.util.List;

public interface MediaTranscriptionProcessDomainService {

    MediaTranscriptionProcessBO submit(String sourceUrl, String idempotencyKey, String title,
                                       String platform, String noteType);

    List<MediaTranscriptionProcessBO> list(int limit);

    MediaTranscriptionProcessBO get(String processId);

    MediaTranscriptionProcessBO retry(String processId);

    MediaTranscriptionProcessBO claimNext();

    void execute(MediaTranscriptionProcessBO process);
}
