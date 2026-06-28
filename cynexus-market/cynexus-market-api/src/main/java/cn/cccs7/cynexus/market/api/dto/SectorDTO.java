package cn.cccs7.cynexus.market.api.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorDTO {

    private Long id;
    private String sectorId;
    private Integer categoryRaw;
    private String categoryLabel;
    private String name;
    private String topicId;
    private String source;
    private OffsetDateTime updatedAt;
}
