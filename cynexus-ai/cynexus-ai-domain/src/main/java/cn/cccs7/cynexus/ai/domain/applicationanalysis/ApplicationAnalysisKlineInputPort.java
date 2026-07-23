package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import java.util.Map;

/** Supplies market facts required by the application-analysis use case. */
public interface ApplicationAnalysisKlineInputPort {

    Map<String, Object> buildRecent30Input(ApplicationAnalysisPageBO.Target target);
}
