package cn.cccs7.cynexus.core.domain.apiworkflow;

import java.util.List;

public interface ApiWorkflowQueryDomainService {
    ApiWorkflowPageBO list(int page, int pageSize, ApiWorkflowType type, ApiWorkflowStatus status);
}
