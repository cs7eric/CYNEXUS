package cn.cccs7.cynexus.core.application.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerJobDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String serviceModule;
    private String serviceClass;
    private String configFile;
    private Boolean enabled;
    private OffsetDateTime registeredAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
