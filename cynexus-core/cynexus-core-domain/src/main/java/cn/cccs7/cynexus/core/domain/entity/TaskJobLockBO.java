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
public class TaskJobLockBO {

    private String lockKey;
    private String owner;
    private OffsetDateTime expiresAt;
    private OffsetDateTime acquiredAt;
}
