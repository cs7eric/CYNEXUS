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
public class SelfSelectedListBO {

    private Long id;
    private String legacyKey;
    private String name;
    private String description;
    private String color;
    private String listKind;
    private String status;
    private Integer sortOrder;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
