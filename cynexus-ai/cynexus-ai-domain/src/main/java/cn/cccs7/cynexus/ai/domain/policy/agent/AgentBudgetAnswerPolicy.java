package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import java.util.List;

/** Builds a final answer when the Agent tool budget is exhausted. */
public final class AgentBudgetAnswerPolicy {

    private AgentBudgetAnswerPolicy() {
    }

    public static String compose(List<AgentRunStepBO> steps, String modelContent) {
        StringBuilder answer = new StringBuilder();
        answer.append("工具预算已用尽，以下基于本轮已获取的数据作答。\n\n");
        String modelText = modelContent == null ? "" : modelContent.trim();
        if (!modelText.isBlank() && !modelText.contains("tool_calls") && modelText.length() < 4000) {
            answer.append(modelText.trim()).append("\n\n");
        }
        if (steps != null) {
            answer.append("## 已执行工具\n\n");
            int shown = 0;
            for (AgentRunStepBO step : steps) {
                if (step == null || step.getToolCode() == null || step.getToolCode().isBlank()) {
                    continue;
                }
                answer.append("- ").append(step.getToolCode());
                if (step.getSummary() != null && !step.getSummary().isBlank()) {
                    answer.append("：").append(step.getSummary());
                } else if (step.getStatus() != null) {
                    answer.append("：").append(step.getStatus());
                }
                answer.append("\n");
                shown++;
                if (shown >= 12) {
                    break;
                }
            }
            if (shown == 0) {
                answer.append("- 尚无可用工具结果\n");
            }
        }
        answer.append("\n## 限制\n\n- 为控制成本与稳定性，本轮已停止继续调用工具。如需更深分析，请缩小范围后再问。");
        return answer.toString().trim();
    }
}