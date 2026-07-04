package cn.cccs7.cynexus.market.domain.entity;

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
public class DataSourceFreshnessBO {

    private OffsetDateTime generatedAt;
    private List<ItemBO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemBO {
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
