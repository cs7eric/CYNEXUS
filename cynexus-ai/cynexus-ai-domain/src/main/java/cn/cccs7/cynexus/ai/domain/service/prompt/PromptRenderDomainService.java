package cn.cccs7.cynexus.ai.domain.service.prompt;

import cn.cccs7.cynexus.ai.domain.policy.prompt.PromptRenderPolicy;
import java.util.Map;

/** Domain service facade for prompt rendering use cases. */
public class PromptRenderDomainService {

    public String render(String template, String userInput, Map<String, Object> variables) {
        return PromptRenderPolicy.render(template, userInput, variables);
    }
}
