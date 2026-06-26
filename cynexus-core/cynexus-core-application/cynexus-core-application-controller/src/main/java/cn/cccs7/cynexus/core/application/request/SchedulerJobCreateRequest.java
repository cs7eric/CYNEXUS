package cn.cccs7.cynexus.core.application.request;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerJobCreateRequest {

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
}
