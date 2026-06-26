package cn.cccs7.cynexus.ai.domain.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAnalysisResultHistoryBO {

    private Long id;
    private Long targetId;
    private String targetKey;
    private String historyKey;
    private String sourceKind;
    private OffsetDateTime analysisRunAt;
    private String payload;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
