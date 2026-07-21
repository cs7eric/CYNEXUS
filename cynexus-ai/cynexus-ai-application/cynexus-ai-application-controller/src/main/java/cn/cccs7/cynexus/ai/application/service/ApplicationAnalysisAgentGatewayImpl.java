package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisPageDTO;
import cn.cccs7.cynexus.ai.domain.port.ApplicationAnalysisGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ApplicationAnalysisAgentGatewayImpl implements ApplicationAnalysisGateway {

    private final ApplicationAnalysisPageAppService appService;
    private final ObjectMapper objectMapper;

    public ApplicationAnalysisAgentGatewayImpl(ApplicationAnalysisPageAppService appService, ObjectMapper objectMapper) {
        this.appService = appService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> getResult(String targetId) {
        return toMap(appService.getResult(targetId));
    }

    @Override
    public Map<String, Object> getRecent30(String targetId, int limit) {
        return toMap(appService.listRecent30Full(targetId, limit));
    }

    private Map<String, Object> toMap(Object value) {
        if (value == null) return null;
        return objectMapper.convertValue(value, new TypeReference<>() { });
    }
}
