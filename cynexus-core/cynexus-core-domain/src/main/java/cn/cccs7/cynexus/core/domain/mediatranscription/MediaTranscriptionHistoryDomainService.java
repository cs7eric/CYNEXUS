package cn.cccs7.cynexus.core.domain.mediatranscription;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import java.util.List;
import java.util.Map;

public interface MediaTranscriptionHistoryDomainService {

    List<MediaTranscriptionHistoryBO> list();

    MediaTranscriptionHistoryBO require(String historyId);

    Map<String, Object> record(String historyId);

    MediaTranscriptionHistoryBO saveCompletedTask(MediaTranscriptionTaskBO task);

    List<MediaTranscriptionHistoryBO> reorder(List<String> orderedIds);

    MediaTranscriptionHistoryBO delete(String historyId);

    MediaTranscriptionHistoryQaBO ask(String historyId, String question);
}
