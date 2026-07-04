package cn.cccs7.cynexus.market.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarginTradingLatestDTO {

    private Boolean ok;
    private String error;
    private LocalDate tradeDate;
    private LocalDate requestedTradeDate;
    private Boolean fallbackTradeDate;
    private String source;
    private OffsetDateTime updatedAt;
    private Integer summaryCount;
    private Integer stockCount;
    private List<MarginTradingDTO.MarketSummaryDTO> summaries;
    private MarginTradingDTO.AccountStatsDTO account;
    private List<MarginTradingDTO.StockDetailDTO> topStocks;
}
