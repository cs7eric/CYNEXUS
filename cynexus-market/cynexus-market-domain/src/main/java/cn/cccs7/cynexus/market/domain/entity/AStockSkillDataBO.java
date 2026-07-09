package cn.cccs7.cynexus.market.domain.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AStockSkillDataBO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetBO {
        private String code;
        private String name;
        private String groupName;
        private String description;
        private String store;
        private String databaseName;
        private String schemaName;
        private String tableName;
        private String source;
        private String grain;
        private String codeColumn;
        private String dateColumn;
        private String defaultSort;
        private List<String> columns;
        private Long rowCount;
        private String latestAt;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetListBO {
        private OffsetDateTime generatedAt;
        private List<DatasetBO> datasets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowsBO {
        private DatasetBO dataset;
        private OffsetDateTime queriedAt;
        private Integer limit;
        private String code;
        private String source;
        private List<String> columns;
        private List<Map<String, Object>> rows;
    }
}
