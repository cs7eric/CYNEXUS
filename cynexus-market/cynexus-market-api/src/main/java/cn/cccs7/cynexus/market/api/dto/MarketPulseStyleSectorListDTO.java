package cn.cccs7.cynexus.market.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MarketPulseStyleSectorListDTO {

    private Boolean ok;
    private List<ItemDTO> items;
    private Integer count;
    private List<String> names;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDTO {
        private String name;
        @JsonProperty("change_pct")
        private BigDecimal changePct;
        @JsonProperty("valid_size")
        private Integer validSize;
        @JsonProperty("sample_size")
        private Integer sampleSize;
    }
}
