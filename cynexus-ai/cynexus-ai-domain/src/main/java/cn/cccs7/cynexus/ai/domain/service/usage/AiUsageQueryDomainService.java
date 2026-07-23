package cn.cccs7.cynexus.ai.domain.service.usage;

import cn.cccs7.cynexus.ai.domain.entity.usage.AiUsageReportBO;

public interface AiUsageQueryDomainService {

    AiUsageReportBO query(String fromText, String toText, Integer requestedLimit);
}
