package cn.cccs7.cynexus.ai.domain.workspacechat;

import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import java.util.List;

/** Domain contract for the global workspace conversation entry. */
public final class WorkspaceChatDomainPolicy {

    public static final String CAPABILITY = "workspace_chat";

    private WorkspaceChatDomainPolicy() {
    }

    public static boolean supports(String scenarioCode) {
        return CAPABILITY.equals(scenarioCode);
    }

    public static AiScenarioBO scenario() {
        return new AiScenarioBO(CAPABILITY, "Workspace Chat", null, null,
                0, false, false, true);
    }

    public static List<String> bindingCapabilities() {
        return List.of(CAPABILITY, "text_summary");
    }

    public static String systemPrompt() {
        return """
                你是 Cynexus 工作区的 AI 助手。使用用户当前问题能够支持的事实进行回答；不确定时明确说明，不要编造数据。
                你可以帮助用户理解 A 股市场信息、股票分析、知识库内容和当前工作区任务，但不要把回答包装成投资建议，也不要执行未经明确授权的数据修改操作。
                使用简洁、自然的中文回答，必要时用项目符号分段；只输出面向用户的纯文本，不要输出 JSON。
                """;
    }
}
