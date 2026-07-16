package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MarketInformationDTO {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class FeedDTO { private OffsetDateTime queriedAt; private NewsPageDTO newsPage; private BriefDTO brief; private List<NewsDTO> news; private List<NewsDTO> macroNews; private List<PolicyAnnouncementDTO> policyAnnouncements; private List<SignalDTO> signals; private List<HotStockDTO> hotStocks; private List<HotStockDTO> leaders; private List<LeaderEventDTO> leaderEvents; private List<DragonTigerDTO> dragonTiger; private List<ImpactIndustryDTO> impactIndustries; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class NewsPageDTO { private List<NewsDTO> items; private long total; private int page; private int pageSize; private int totalPages; private boolean hasPrevious; private boolean hasNext; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class NewsDTO { private String id; private String title; private String summary; private String source; private String category; private String kind; private String issuer; private List<String> labels; private OffsetDateTime publishedAt; private String url; private Long clusterId; private String analysisStatus; private String materiality; private String marketDirection; private List<ImpactIndustryDTO> impactIndustries; private String impactStatus; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class BriefDTO { private LocalDate briefDate; private String status; private String providerCode; private String modelCode; private Boolean fallbackUsed; private String contentJson; private OffsetDateTime updatedAt; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class EventDetailDTO { private Long clusterId; private String title; private String category; private List<String> entities; private BigDecimal priorityScore; private Boolean highValue; private OffsetDateTime firstPublishedAt; private OffsetDateTime lastPublishedAt; private String contentHash; private String analysisStatus; private String providerCode; private String modelCode; private Boolean fallbackUsed; private String analysisJson; private List<EventMemberDTO> members; private List<ImpactIndustryDTO> impactIndustries; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class EventMemberDTO { private String source; private String sourceRecordId; private String title; private String summary; private String url; private OffsetDateTime publishedAt; private String contentStatus; private String contentQuality; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class AnalysisRequestDTO { private long clusterId; private boolean queued; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class PipelineRefreshDTO { private int clusterCount; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class AnalysisStatusDTO { private Long clusterId; private String analysisStatus; private String materiality; private String marketDirection; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class PolicyAnnouncementDTO { private String id; private String title; private String category; private LocalDate publishedDate; private String url; private String issuer; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class LeaderEventDTO { private String code; private String name; private String title; private String summary; private String source; private OffsetDateTime publishedAt; private String url; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class SignalDTO { private String code; private String name; private String pool; private String reason; private BigDecimal changePct; private BigDecimal sealAmount; private LocalDate snapshotDate; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class HotStockDTO { private String code; private String name; private Integer rank; private BigDecimal heat; private BigDecimal pct; private LocalDate snapshotDate; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class DragonTigerDTO { private String code; private String name; private String reason; private BigDecimal changePct; private BigDecimal netBuyWan; private LocalDate tradeDate; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class ImpactIndustryDTO { private String tdxCode; private String tdxName; private String thsCode; private String thsName; private String status; private String source; private BigDecimal confidence; private List<RelatedStockDTO> relatedStocks; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class RelatedStockDTO { private String code; private String name; }
}
