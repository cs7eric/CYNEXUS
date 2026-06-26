package cn.cccs7.cynexus.ai.domain.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAnalysisDailySnapshotBO {

    private Long id;
    private Long targetId;
    private String targetKey;
    private LocalDate tradeDate;
    private String snapshotKind;
    private String payload;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
