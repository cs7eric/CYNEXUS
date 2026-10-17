package cn.cccs7.cynexus.ai.domain.knowledgegeneration;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import java.util.List;

public final class KnowledgeBaseDocumentGenerationPromptPolicy {

    public static final String CAPABILITY = "knowledge_base_document_generate";
    public static final String PROMPT_KEY = CAPABILITY;

    public static final String DEFAULT_SYSTEM_PROMPT = """
            你是 Cynexus 知识库 Markdown 文档整理助手。
            任务：根据用户输入的一段原始文本，提炼出适合保存到知识库的文档草稿。
            要求：title 最多 40 个字，description 最多 120 个字，tags 使用短词，content 输出完整 Markdown 正文。
            不要编造输入中没有的事实，严格输出合法 JSON，不要添加 Markdown 代码块或额外解释。
            """;
    public static final String DEFAULT_USER_PROMPT_TEMPLATE = """
            {{formatInstructions}}

            原始文本：
            {{sourceText}}
            """;

    public KnowledgeBaseGenerationPrompt resolve(AiPromptBO bound, List<AiPromptBO> candidates,
                                                  String requestedVersion) {
        if (isActive(bound) && versionMatches(bound.getPromptVersion(), requestedVersion)) {
            return toPrompt(bound);
        }
        if (candidates != null) {
            return candidates.stream()
                    .filter(this::isActive)
                    .filter(item -> PROMPT_KEY.equals(item.getPromptKey()))
                    .filter(item -> versionMatches(item.getPromptVersion(), requestedVersion))
                    .findFirst()
                    .map(this::toPrompt)
                    .orElseGet(this::fallback);
        }
        return fallback();
    }

    private KnowledgeBaseGenerationPrompt toPrompt(AiPromptBO prompt) {
        return new KnowledgeBaseGenerationPrompt(
                PROMPT_KEY,
                valueOrDefault(prompt.getPromptVersion(), "configured"),
                valueOrDefault(prompt.getSystemPrompt(), DEFAULT_SYSTEM_PROMPT),
                valueOrDefault(prompt.getUserPromptTemplate(), DEFAULT_USER_PROMPT_TEMPLATE));
    }

    private KnowledgeBaseGenerationPrompt fallback() {
        return new KnowledgeBaseGenerationPrompt(PROMPT_KEY, "fallback-service-default",
                DEFAULT_SYSTEM_PROMPT, DEFAULT_USER_PROMPT_TEMPLATE);
    }

    private boolean isActive(AiPromptBO prompt) {
        return prompt != null && AiConstants.STATUS_ACTIVE.equalsIgnoreCase(prompt.getStatus());
    }

    private boolean versionMatches(String actual, String requested) {
        return requested == null || requested.isBlank() || requested.equals(actual);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
