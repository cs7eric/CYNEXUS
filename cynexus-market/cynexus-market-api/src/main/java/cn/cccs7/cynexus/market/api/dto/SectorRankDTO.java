package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorRankDTO {

    private String tradeDate;
    private Integer topN;
    private List<SectorRankItemDTO> items;

}
