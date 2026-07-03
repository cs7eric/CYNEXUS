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
public class MarketPulseSectorSnapshotDTO {

    private boolean ok;
    private LocalDate tradeDate;
    private LocalDate requestedTradeDate;
    private boolean fallbackTradeDate;
    private String servingMode;
    private String sourceKind;
    private String sourceName;
    private String status;
    private OffsetDateTime fetchedAt;
    private int rowCount;
    private List<MarketPulseSectorDailySnapshotDTO> rows;
    private String error;
}
