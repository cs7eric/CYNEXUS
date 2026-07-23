package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

/** Port for completing an application-analysis model turn. */
public interface ApplicationAnalysisCompletionPort {

    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
}
