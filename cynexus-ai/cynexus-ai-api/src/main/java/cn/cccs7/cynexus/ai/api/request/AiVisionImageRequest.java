package cn.cccs7.cynexus.ai.api.request;

import lombok.Data;

@Data
public class AiVisionImageRequest {
    private String fileName;
    private String contentType;
    private String dataUrl;
}
