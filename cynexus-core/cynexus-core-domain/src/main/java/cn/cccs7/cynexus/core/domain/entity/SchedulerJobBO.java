package cn.cccs7.cynexus.core.domain.entity;

import java.time.OffsetDateTime;

public record SchedulerJobBO(
        Long id,
        String code,
        String name,
        String description,
        String serviceModule,
        String serviceClass,
        String configFile,
        Boolean enabled,
        OffsetDateTime registeredAt,
        String extra,
        String remark
) {
}
