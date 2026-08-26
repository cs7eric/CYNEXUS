package cn.cccs7.cynexus.market.infra.factorresearch.entity;
import java.time.Instant;
import lombok.Data;
@Data public class MarketFactorSetPO {
    private Long id; private String factorSetCode; private String tenantId; private String ownerId; private String name; private String status; private Long activeVersionId; private Long version;
    private Long versionId; private Integer versionNo; private Long sourceStudyId; private String expression; private String validationSnapshot; private String versionStatus; private Instant createdAt;
}
