package cn.cccs7.cynexus.ai.application.service;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromptRenderAppService {

    public String render(String template, String userInput, Map<String, Object> variables) {
        String rendered = template == null ? "" : template;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
        }
        return rendered + System.lineSeparator() + "User: " + (userInput == null ? "" : userInput);
    }
}
