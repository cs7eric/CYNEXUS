package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiUsageObservationBO;
import java.time.OffsetDateTime;
import java.util.List;

public interface AiUsageObservabilityRepository {
    List<AiUsageObservationBO> list(OffsetDateTime from, OffsetDateTime to, int limit);
}
