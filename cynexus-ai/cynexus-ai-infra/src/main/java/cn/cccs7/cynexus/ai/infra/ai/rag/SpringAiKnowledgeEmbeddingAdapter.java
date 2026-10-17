package cn.cccs7.cynexus.ai.infra.ai.rag;

import cn.cccs7.cynexus.ai.domain.port.KnowledgeEmbeddingPort;
import org.springframework.ai.embedding.EmbeddingModel;

public class SpringAiKnowledgeEmbeddingAdapter implements KnowledgeEmbeddingPort {
    private final EmbeddingModel model;
    public SpringAiKnowledgeEmbeddingAdapter(EmbeddingModel model) { this.model = model; }
    @Override public float[] embed(String text) { return model.embed(text == null ? "" : text); }
}
