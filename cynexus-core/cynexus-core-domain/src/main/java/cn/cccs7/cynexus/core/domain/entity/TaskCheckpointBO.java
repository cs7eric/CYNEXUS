package cn.cccs7.cynexus.core.domain.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCheckpointBO {

    private String checkpointKey;
    private String jobKey;
    private String checkpointValue;
    private OffsetDateTime updatedAt;
}
