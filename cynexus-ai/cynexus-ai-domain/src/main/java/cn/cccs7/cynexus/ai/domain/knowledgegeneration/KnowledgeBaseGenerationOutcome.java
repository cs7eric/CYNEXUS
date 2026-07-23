package cn.cccs7.cynexus.ai.domain.knowledgegeneration;

import cn.cccs7.cynexus.ai.domain.knowledgebase.KnowledgeBaseDocumentDraftPolicy;
import java.util.List;

public record KnowledgeBaseGenerationOutcome(KnowledgeBaseGenerationStatus status, String sourceText,
                                             KnowledgeBaseGenerationPrompt prompt,
                                             KnowledgeBaseDocumentDraftPolicy.Draft draft,
                                             List<KnowledgeBaseGenerationAttempt> attempts,
                                             String failureReason) {
    public boolean succeeded() {
        return status == KnowledgeBaseGenerationStatus.SUCCEEDED && draft != null;
    }
}
