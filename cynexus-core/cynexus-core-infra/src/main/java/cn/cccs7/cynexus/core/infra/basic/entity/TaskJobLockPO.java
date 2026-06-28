package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_job_lock")
public class TaskJobLockPO extends BasePO {

    private String lockKey;
    private String owner;
    private OffsetDateTime expiresAt;
    private OffsetDateTime acquiredAt;
}
