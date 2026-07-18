package cn.cccs7.cynexus.ai.api.request;

import java.util.List;
import lombok.Data;

@Data
public class AiVisionExecuteRequest {
    private String scenarioCode;
    private String userInput;
    private String preferredModelCode;
    private List<AiVisionImageRequest> images;
}
