package cn.cccs7.cynexus.core.api.dto.finance;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceCategoryDTO {
    private Long id;
    private Long parentId;
    private String name;
    private String direction;
    private String icon;
    private String color;
    private Integer sortOrder;
    private Boolean system;
    private Boolean archived;
    private OffsetDateTime createdAt;
}
