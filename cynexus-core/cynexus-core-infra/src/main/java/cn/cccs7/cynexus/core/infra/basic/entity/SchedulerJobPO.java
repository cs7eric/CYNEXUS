package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class SchedulerJobPO extends BasePO {

    private String code;
    private String name;
    private String description;
    private String serviceModule;
    private String serviceClass;
    private String configFile;
    private Boolean isEnabled;
    private OffsetDateTime registeredAt;
    private String extra;
    private String remark;
}
