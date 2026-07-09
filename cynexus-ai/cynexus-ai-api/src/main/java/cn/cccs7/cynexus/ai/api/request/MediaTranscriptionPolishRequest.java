package cn.cccs7.cynexus.ai.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionPolishRequest {

    private String text;
    private String preferredModelCode;
}
