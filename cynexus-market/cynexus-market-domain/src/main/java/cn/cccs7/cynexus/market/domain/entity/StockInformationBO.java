package cn.cccs7.cynexus.market.domain.entity;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class StockInformationBO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceRecord {
        private String kind;
        private String source;
        private String code;
        private String itemId;
        private String title;
        private String url;
        private String inlineText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private Long id;
        private SourceRecord sourceRecord;
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
    public static class Analysis {
        private Long id;
        private Long contentId;
        private String contentHash;
        private String schemaVersion;
        private String promptVersion;
        private String modelCode;
        private String status;
        private String resultJson;
        private String errorMessage;
        private OffsetDateTime analyzedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Feed {
        private String symbol;
        private OffsetDateTime queriedAt;
        private List<Announcement> announcements;
        private List<News> news;
        private List<Research> research;
        private List<InvestorInteraction> investorInteractions;
        private List<Roadshow> roadshows;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Announcement {
        private String id;
        private String title;
        private String category;
        private OffsetDateTime publishedAt;
        private String source;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class News {
        private String id;
        private String title;
        private String summary;
        private String mediaName;
        private OffsetDateTime publishedAt;
        private String source;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Research {
        private String id;
        private String title;
        private String organization;
        private String author;
        private String rating;
        private OffsetDateTime publishedAt;
        private String source;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestorInteraction {
        private String id;
        private String question;
        private String answer;
        private String answerer;
        private OffsetDateTime askedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Roadshow {
        private String title;
        private String type;
        private OffsetDateTime startAt;
        private String source;
        private String url;
    }
}
