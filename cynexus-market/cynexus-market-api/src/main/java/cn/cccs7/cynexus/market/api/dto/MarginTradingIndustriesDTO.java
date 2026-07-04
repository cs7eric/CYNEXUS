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
public class MarginTradingIndustriesDTO {

    private Boolean ok;
    private String error;
    private LocalDate tradeDate;
    private LocalDate constituentSnapshotDate;
    private String sort;
    private String sortDir;
    private Integer page;
    private Integer pageSize;
    private Integer limit;
    private Integer count;
    private List<MarginTradingDTO.IndustrySummaryDTO> items;
}
