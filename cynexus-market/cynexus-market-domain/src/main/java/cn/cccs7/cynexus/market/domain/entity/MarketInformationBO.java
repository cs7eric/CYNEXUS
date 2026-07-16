package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MarketInformationBO {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Feed { private OffsetDateTime queriedAt; private NewsPage newsPage; private Brief brief; private List<News> news; private List<News> macroNews; private List<PolicyAnnouncement> policyAnnouncements; private List<Signal> signals; private List<HotStock> hotStocks; private List<HotStock> leaders; private List<LeaderEvent> leaderEvents; private List<DragonTiger> dragonTiger; private List<ImpactIndustry> impactIndustries; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NewsPage { private List<News> items; private long total; private int page; private int pageSize; private int totalPages; private boolean hasPrevious; private boolean hasNext; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class News { private String id; private String title; private String summary; private String source; private String category; private String kind; private String issuer; private List<String> labels; private OffsetDateTime publishedAt; private String url; private Long clusterId; private String analysisStatus; private String materiality; private String marketDirection; private List<ImpactIndustry> impactIndustries; private String impactStatus; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Brief { private LocalDate briefDate; private String status; private String providerCode; private String modelCode; private Boolean fallbackUsed; private String contentJson; private OffsetDateTime updatedAt; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EventDetail { private Long clusterId; private String title; private String category; private List<String> entities; private BigDecimal priorityScore; private Boolean highValue; private OffsetDateTime firstPublishedAt; private OffsetDateTime lastPublishedAt; private String contentHash; private String analysisStatus; private String providerCode; private String modelCode; private Boolean fallbackUsed; private String analysisJson; private List<EventMember> members; private List<ImpactIndustry> impactIndustries; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EventMember { private String source; private String sourceRecordId; private String title; private String summary; private String url; private OffsetDateTime publishedAt; private String contentStatus; private String contentQuality; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PolicyAnnouncement { private String id; private String title; private String category; private LocalDate publishedDate; private String url; private String issuer; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LeaderEvent { private String code; private String name; private String title; private String summary; private String source; private OffsetDateTime publishedAt; private String url; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Signal { private String code; private String name; private String pool; private String reason; private BigDecimal changePct; private BigDecimal sealAmount; private LocalDate snapshotDate; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HotStock { private String code; private String name; private Integer rank; private BigDecimal heat; private BigDecimal pct; private LocalDate snapshotDate; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DragonTiger { private String code; private String name; private String reason; private BigDecimal changePct; private BigDecimal netBuyWan; private LocalDate tradeDate; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ImpactIndustry { private String tdxCode; private String tdxName; private String thsCode; private String thsName; private String status; private String source; private BigDecimal confidence; private List<RelatedStock> relatedStocks; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RelatedStock { private String code; private String name; }
}
