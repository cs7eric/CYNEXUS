package cn.cccs7.cynexus.market.api.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class StockInformationContentDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentDTO {
        private String kind;
        private String source;
        private String itemId;
        private String title;
        private String sourceUrl;
        private String resolvedUrl;
        private String contentType;
        private String status;
        private String text;
        private String contentHash;
        private String message;
        private OffsetDateTime extractedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisDTO {
        private String status;
        private String resultJson;
        private String modelCode;
        private String errorMessage;
        private OffsetDateTime analyzedAt;
    }
}
