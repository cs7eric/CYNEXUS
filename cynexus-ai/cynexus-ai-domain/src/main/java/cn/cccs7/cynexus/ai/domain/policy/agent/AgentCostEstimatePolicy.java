package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Estimates token cost for an Agent model turn. */
public final class AgentCostEstimatePolicy {

    private AgentCostEstimatePolicy() {
    }

    public static BigDecimal estimate(AiModelBO model, int inputTokens, int outputTokens) {
        if (model == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal inputPrice = model.getInputPricePerMillion() == null ? BigDecimal.ZERO : model.getInputPricePerMillion();
        BigDecimal outputPrice = model.getOutputPricePerMillion() == null ? BigDecimal.ZERO : model.getOutputPricePerMillion();
        return inputPrice.multiply(BigDecimal.valueOf(Math.max(0, inputTokens)))
                .add(outputPrice.multiply(BigDecimal.valueOf(Math.max(0, outputTokens))))
                .divide(BigDecimal.valueOf(1_000_000L), 8, RoundingMode.HALF_UP);
    }
}