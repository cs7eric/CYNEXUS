package cn.cccs7.cynexus.market.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioTradeImageAnalysisDTO {
    private String scenarioCode;
    private String status;
    private String content;
}
