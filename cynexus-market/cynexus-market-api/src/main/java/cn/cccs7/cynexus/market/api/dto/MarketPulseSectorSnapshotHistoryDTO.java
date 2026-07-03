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
public class MarketPulseSectorSnapshotHistoryDTO {

    private boolean ok;
    private int days;
    private List<LocalDate> dates;
    private int rowCount;
    private String sourceKind;
    private String sourceName;
    private OffsetDateTime fetchedAt;
    private List<MarketPulseSectorDailySnapshotDTO> rows;
    private String error;
}
