package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/** Runtime context passed to every Agent tool without exposing web or persistence types. */
@Value
@Builder
public class AgentExecutionContext {

    String ownerKey;
    String tenantKey;
    String conversationNo;
    String runNo;
    String workspace;
    LocalDate tradeDate;
    String locale;
}
