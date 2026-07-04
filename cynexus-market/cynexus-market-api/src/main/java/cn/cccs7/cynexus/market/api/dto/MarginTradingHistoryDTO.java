package cn.cccs7.cynexus.market.api.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarginTradingHistoryDTO {

    private Boolean ok;
    private String error;
    private LocalDate start;
    private LocalDate end;
    private Integer count;
    private List<MarginTradingDTO.MarketSummaryDTO> items;
}
