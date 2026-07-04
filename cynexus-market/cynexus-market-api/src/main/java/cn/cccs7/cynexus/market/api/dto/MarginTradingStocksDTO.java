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
public class MarginTradingStocksDTO {

    private Boolean ok;
    private String error;
    private LocalDate tradeDate;
    private String exchange;
    private String industryCode;
    private String sort;
    private String sortDir;
    private Integer page;
    private Integer pageSize;
    private Integer limit;
    private Integer count;
    private List<MarginTradingDTO.StockDetailDTO> items;
}
