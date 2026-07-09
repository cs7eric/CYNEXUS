package cn.cccs7.cynexus.core.application.request;

import java.util.List;
import lombok.Data;

@Data
public class MediaTranscriptionHistoryReorderRequest {

    private List<String> orderedIds;
}
