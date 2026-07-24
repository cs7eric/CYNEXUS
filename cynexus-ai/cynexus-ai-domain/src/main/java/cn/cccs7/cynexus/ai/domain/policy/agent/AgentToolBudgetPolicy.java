package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionLimits;
import java.util.Locale;

/** Classifies Agent questions into simple/deep tool budgets. */
public final class AgentToolBudgetPolicy {

    public enum Depth {
        SIMPLE,
        DEEP
    }

    private AgentToolBudgetPolicy() {
    }

    public static Depth classify(String userInput) {
        String text = userInput == null ? "" : userInput.trim().toLowerCase(Locale.ROOT);
        if (text.isBlank()) return Depth.SIMPLE;
        // Short factual asks stay simple even if they contain one analysis-ish word.
        if (text.length() <= 24 && looksSimple(text)) return Depth.SIMPLE;
        if (looksDeep(text)) return Depth.DEEP;
        if (looksSimple(text)) return Depth.SIMPLE;
        // Medium-length open questions default to deep enough for multi-module analysis.
        return text.length() >= 40 ? Depth.DEEP : Depth.SIMPLE;
    }

    public static int budgetFor(String userInput, AgentExecutionLimits limits) {
        AgentExecutionLimits safe = limits == null ? AgentExecutionLimits.defaults() : limits;
        int budget = classify(userInput) == Depth.DEEP
                ? safe.getDeepMaxToolCalls()
                : safe.getSimpleMaxToolCalls();
        // Never exceed the absolute ceiling.
        return Math.max(0, Math.min(budget, safe.getMaxToolCalls()));
    }

    private static boolean looksSimple(String text) {
        return containsAny(text,
                "多少", "点位", "最新", "现在", "一句话", "简要", "简单", "是什么", "有没有",
                "hi", "hello", "你好", "在吗", "价格", "市盈率", "涨了吗", "跌了吗");
    }

    private static boolean looksDeep(String text) {
        return containsAny(text,
                "分析", "深度", "复盘", "对比", "全面", "详细", "为什么", "原因", "驱动",
                "结构", "研判", "策略", "机会", "风险", "板块轮动", "涨跌停", "资金流",
                "多日", "区间", "历史", "趋势", "归因", "拆解", "完整", "系统");
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
