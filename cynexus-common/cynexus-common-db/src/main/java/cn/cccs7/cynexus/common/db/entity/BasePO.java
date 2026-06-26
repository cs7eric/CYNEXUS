package cn.cccs7.cynexus.common.db.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class BasePO {

    private Long id;
    @TableField("created_at")
    private OffsetDateTime createdAt;
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    @TableField("deleted_at")
    private OffsetDateTime deletedAt;
}
