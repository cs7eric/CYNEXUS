package cn.cccs7.cynexus.ai.api.request;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunRequest {
    private String conversationNo;
    private String idempotencyKey;
    private String userInput;
    private String preferredModelCode;
    private String workspace;
    private String locale;
    private LocalDate tradeDate;
}
