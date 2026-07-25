package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentRunExecutionCommand {
    String runNo;
    String conversationNo;
    String idempotencyKey;
    String userInput;
    String preferredProviderCode;
    String preferredModelCode;
    String preferredReasoningEffort;
    String workspace;
    String locale;
    LocalDate tradeDate;
    AgentIdentity identity;
    boolean streamTokens;
}
