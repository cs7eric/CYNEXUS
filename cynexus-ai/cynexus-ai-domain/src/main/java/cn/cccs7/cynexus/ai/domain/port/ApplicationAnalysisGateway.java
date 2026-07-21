package cn.cccs7.cynexus.ai.domain.port;

import java.util.Map;

public interface ApplicationAnalysisGateway {
    Map<String, Object> getResult(String targetId);
    Map<String, Object> getRecent30(String targetId, int limit);
}
