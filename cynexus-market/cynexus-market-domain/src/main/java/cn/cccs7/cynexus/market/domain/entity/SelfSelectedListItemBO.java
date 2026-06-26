package cn.cccs7.cynexus.market.domain.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelfSelectedListItemBO {

    private Long id;
    private String legacyKey;
    private Long listId;
    private String symbol;
    private String market;
    private String name;
    private String notes;
    private String targetType;
    private String sourceType;
    private String status;
    private Integer sortOrder;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
