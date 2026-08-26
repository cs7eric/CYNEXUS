package cn.cccs7.cynexus.market.infra.stockselection.entity;

import lombok.Data;

@Data
public class StockSelectionStrategyEvidencePO {
    private Long id;
    private Long strategyCandidateId;
    private String evidenceCode;
    private String label;
    private String operator;
    private String expectedValue;
    private String actualValue;
    private Boolean matched;
}
