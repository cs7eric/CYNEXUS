package cn.cccs7.cynexus.market.infra.portfolio.entity;

import java.time.LocalDate;
import lombok.Data;

@Data
public class PortfolioReviewPO {
    private Long id;
    private String principalKey;
    private String tenantKey;
    private String symbol;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String thesis;
    private String plan;
    private String execution;
    private String lessons;
    private String nextAction;
    private String tags;
    private Integer rating;
    private String metricsSnapshot;
    private String status;
}
