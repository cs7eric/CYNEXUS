package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.dto.ApiWorkflowEventDTO;
import cn.cccs7.cynexus.core.application.dto.ApiWorkflowProcessDTO;
import cn.cccs7.cynexus.core.domain.apiworkflow.ApiWorkflowProcessBO;
import cn.cccs7.cynexus.core.domain.apiworkflow.ApiWorkflowQueryDomainService;
import cn.cccs7.cynexus.core.domain.apiworkflow.ApiWorkflowStatus;
import cn.cccs7.cynexus.core.domain.apiworkflow.ApiWorkflowType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApiWorkflowAppService {
    private final ApiWorkflowQueryDomainService domainService;

    public ApiWorkflowAppService(ApiWorkflowQueryDomainService domainService) { this.domainService = domainService; }

    public List<ApiWorkflowProcessDTO> list(int limit, String type, String status) {
        return domainService.list(limit, parseType(type), parseStatus(status)).stream().map(this::toDto).toList();
    }

    private ApiWorkflowProcessDTO toDto(ApiWorkflowProcessBO item) {
        return ApiWorkflowProcessDTO.builder().id(item.id()).workflowType(item.workflowType()).workflowKey(item.workflowKey())
                .source(item.source()).sourceRef(item.sourceRef()).title(item.title()).status(item.status()).phase(item.phase())
                .progress(item.progress()).attemptCount(item.attemptCount()).errorCode(item.errorCode()).errorMessage(item.errorMessage())
                .resultRoute(item.resultRoute()).resultRef(item.resultRef()).createdAt(item.createdAt()).startedAt(item.startedAt())
                .completedAt(item.completedAt()).updatedAt(item.updatedAt()).events(item.events().stream().map(event -> ApiWorkflowEventDTO.builder()
                        .eventType(event.eventType()).phase(event.phase()).status(event.status()).progress(event.progress())
                        .message(event.message()).errorCode(event.errorCode()).errorMessage(event.errorMessage()).occurredAt(event.occurredAt()).build()).toList()).build();
    }

    private static ApiWorkflowType parseType(String value) {
        if (value == null || value.isBlank()) return null;
        try { return ApiWorkflowType.valueOf(value.trim().toUpperCase()); } catch (IllegalArgumentException ignored) { return null; }
    }

    private static ApiWorkflowStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try { return ApiWorkflowStatus.valueOf(value.trim().toUpperCase()); } catch (IllegalArgumentException ignored) { return null; }
    }
}
