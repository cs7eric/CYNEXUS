package cn.cccs7.cynexus.ai.domain.port;

/** Embedding boundary kept independent from Spring AI and vector-store SDKs. */
public interface KnowledgeEmbeddingPort {
    float[] embed(String text);
}
