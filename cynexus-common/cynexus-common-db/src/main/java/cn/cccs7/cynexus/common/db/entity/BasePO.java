package cn.cccs7.cynexus.common.db.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BasePO {

    private Long id;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
