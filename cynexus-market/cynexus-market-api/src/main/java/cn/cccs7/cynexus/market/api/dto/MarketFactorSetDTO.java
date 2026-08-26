package cn.cccs7.cynexus.market.api.dto;
import java.util.List; import java.util.Map; import lombok.*;
public class MarketFactorSetDTO {
 @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class CreateRequest { private String code; private String name; private Map<String,Object> expression; private Long sourceStudyId; }
 @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class UpdateRequest { private String name; private Map<String,Object> expression; private Long sourceStudyId; private Long expectedVersion; }
 @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class ValidateRequest { private Integer versionNo; private Map<String,Object> validationSnapshot; }
 @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class PreviewRequest { private String tradeDate; private Integer limit; }
 @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class Version { private Long id; private Integer versionNo; private String status; private Map<String,Object> expression; private Map<String,Object> validationSnapshot; }
 @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class StrategyHandoffResponse { private String factorSetCode; private Long factorSetVersionId; private Integer versionNo; private Long sourceStudyId; private Map<String,Object> expression; private String status; }
 @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class PreviewItem { private String code; private String name; private String market; private String industry; private java.math.BigDecimal price; private java.math.BigDecimal changePct; private String reason; }
 @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class PreviewResponse { private Long factorSetVersionId; private String code; private Integer versionNo; private String requestedTradeDate; private String snapshotTradeDate; private String status; private Integer count; private List<PreviewItem> items; private List<String> warnings; }
 @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class Response { private Long id; private String code; private String name; private String status; private Long activeVersionId; private Long version; private Version activeVersion; }
 @Data @Builder @NoArgsConstructor @AllArgsConstructor public static class ListResponse { private List<Response> items; }
}
