package cn.cccs7.cynexus.market.api.dto;

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
public class DataSourceFreshnessDTO {

    private Boolean ok;
    private String error;
    private OffsetDateTime generatedAt;
    private SummaryDTO summary;
    private List<ItemDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDTO {
        private Integer total;
        private Integer fresh;
        private Integer lagging;
        private Integer stale;
        private Integer empty;
        private Integer untracked;
        private Integer error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDTO {
        private String sourceType;
        private String databaseName;
        private String schemaName;
        private String tableName;
        private String effectiveDateColumn;
        private String latestEffectiveDate;
        private String latestUpdatedAt;
        private Long rowCount;
        private Long lagDays;
        private String status;
        private String error;
        private String businessLogic;
        private List<String> usedBy;
        private String updateJob;
        private String dataSource;
    }
}
