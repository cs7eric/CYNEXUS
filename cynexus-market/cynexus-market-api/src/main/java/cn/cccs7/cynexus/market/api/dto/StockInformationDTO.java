package cn.cccs7.cynexus.market.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class StockInformationDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedDTO {
        private String symbol;
        private OffsetDateTime queriedAt;
        private List<AnnouncementDTO> announcements;
        private List<NewsDTO> news;
        private List<ResearchDTO> research;
        private List<InvestorInteractionDTO> investorInteractions;
        private List<RoadshowDTO> roadshows;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnouncementDTO {
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
    public static class NewsDTO {
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
    public static class ResearchDTO {
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
    public static class InvestorInteractionDTO {
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
    public static class RoadshowDTO {
        private String title;
        private String type;
        private OffsetDateTime startAt;
        private String source;
        private String url;
    }
}
