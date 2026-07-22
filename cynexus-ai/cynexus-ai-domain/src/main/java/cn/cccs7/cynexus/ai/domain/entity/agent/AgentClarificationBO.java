package cn.cccs7.cynexus.ai.domain.agent;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentClarificationBO {
    String summary;
    List<String> questions;
    List<String> missingFields;
}
