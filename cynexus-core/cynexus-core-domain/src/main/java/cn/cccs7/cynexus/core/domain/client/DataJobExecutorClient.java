package cn.cccs7.cynexus.core.domain.client;

import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import java.util.Map;

public interface DataJobExecutorClient {

    Map<String, Object> health();

    Map<String, Object> dataSourceHealth(boolean includeSlow);

    Map<String, Object> listJobs();

    Map<String, Object> getJob(String jobKey);

    DataJobTriggerResultBO triggerJob(String jobKey, DataJobTriggerRequestBO request);

    Map<String, Object> getJobRun(String runId);

    Map<String, Object> startSocialLogin(String platform, String profileName);

    Map<String, Object> getSocialLogin(String sessionId);

    Map<String, Object> completeSocialLogin(String sessionId);

    Map<String, Object> closeSocialLogin(String sessionId);
}
