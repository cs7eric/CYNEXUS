package cn.cccs7.cynexus.ai.domain.mediatranscription;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

public interface MediaTranscriptionCompletionPort {

    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
}
