package cn.cccs7.cynexus.core.domain.apiworkflow;

import java.util.List;

public interface ApiWorkflowQueryDomainService {
    List<ApiWorkflowProcessBO> list(int limit, ApiWorkflowType type, ApiWorkflowStatus status);
}
