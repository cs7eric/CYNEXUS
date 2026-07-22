package cn.cccs7.cynexus.core.domain.mediatranscription;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MediaTranscriptionHistoryQaBO {

    String id;
    String question;
    String answer;
    OffsetDateTime createdAt;
}
