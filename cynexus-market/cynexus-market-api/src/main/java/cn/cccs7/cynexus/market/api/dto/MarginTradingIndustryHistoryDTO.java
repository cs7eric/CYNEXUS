package cn.cccs7.cynexus.market.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarginTradingIndustryHistoryDTO {
    private Boolean ok;
    private String error;
    private String industryCode;
    private String tradeDate;
    private Integer count;
    private List<MarginTradingDTO.IndustryHistoryDTO> items;
}
