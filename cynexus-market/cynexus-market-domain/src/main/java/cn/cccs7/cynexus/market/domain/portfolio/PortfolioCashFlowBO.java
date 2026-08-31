package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PortfolioCashFlowBO(Long id, OffsetDateTime occurredAt, BigDecimal amount, String flowType, String note) {
    public PortfolioCashFlowBO {
        if (occurredAt == null || amount == null || amount.signum() <= 0 || flowType == null
                || !("DEPOSIT".equals(flowType) || "WITHDRAWAL".equals(flowType))) {
            throw new IllegalArgumentException("invalid portfolio cash flow");
        }
    }
}
