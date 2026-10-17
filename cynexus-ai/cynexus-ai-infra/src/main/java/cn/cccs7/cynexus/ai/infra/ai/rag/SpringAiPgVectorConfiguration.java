package cn.cccs7.cynexus.ai.infra.ai.rag;

import com.openai.client.OpenAIClient;
import java.time.Duration;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeEmbeddingPort;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeRagPort;
import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import cn.cccs7.cynexus.ai.infra.ai.spring.SpringAiOpenAiClientSupport;

/** Explicit opt-in RAG wiring for Release; normal chat clients remain DB-routed and dynamic. */
@Configuration
@ConditionalOnProperty(prefix = "cynexus.ai.rag", name = "enabled", havingValue = "true")
public class SpringAiPgVectorConfiguration {
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    EmbeddingModel ragEmbeddingModel(SecretResolver secrets,
            @Value("${cynexus.ai.rag.embedding-api-key-env:${cynexus.ai.platform.providers.openai.api-key-env:CYNEXUS_AI_OPENAI_API_KEY}}") String keyEnv,
            @Value("${cynexus.ai.rag.embedding-base-url:${cynexus.ai.platform.providers.openai.base-url:https://api.openai.com}}") String baseUrl,
            @Value("${cynexus.ai.rag.embedding-model:text-embedding-v3}") String model,
            @Value("${cynexus.ai.rag.embedding-dimensions:1024}") int dimensions) {
        String key = secrets.resolve("env://" + keyEnv);
        if (key == null || key.isBlank()) throw new IllegalStateException("RAG embedding API key is missing for " + keyEnv);
        Duration timeout = Duration.ofSeconds(60);
        OpenAIClient client = SpringAiOpenAiClientSupport.client(baseUrl, key, timeout);
        return new OpenAiEmbeddingModel(client, org.springframework.ai.document.MetadataMode.NONE,
                OpenAiEmbeddingOptions.builder().model(model).dimensions(dimensions).build());
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    VectorStore ragVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel,
            @Value("${cynexus.ai.rag.embedding-dimensions:1024}") int dimensions,
            @Value("${cynexus.ai.rag.vector-table:cynexus_appl_ai.vector_store}") String configuredTable) {
        String qualifiedTable = configuredTable == null || configuredTable.isBlank()
                ? "cynexus_appl_ai.vector_store" : configuredTable.trim();
        int separator = qualifiedTable.indexOf('.');
        String schema = separator > 0 ? qualifiedTable.substring(0, separator) : "public";
        String table = separator > 0 ? qualifiedTable.substring(separator + 1) : qualifiedTable;
        if (schema.isBlank() || table.isBlank() || qualifiedTable.indexOf('.', separator + 1) >= 0) {
            throw new IllegalArgumentException("cynexus.ai.rag.vector-table must be [schema.]table");
        }
        return PgVectorStore.builder(jdbcTemplate, embeddingModel).dimensions(dimensions)
                .schemaName(schema).vectorTableName(table)
                .initializeSchema(false).vectorTableValidationsEnabled(false).build();
    }

    @Bean
    @ConditionalOnMissingBean(KnowledgeRagPort.class)
    KnowledgeRagPort ragKnowledgeAdapter(VectorStore vectorStore) {
        return new SpringAiKnowledgeRagAdapter(vectorStore);
    }

    @Bean
    @ConditionalOnMissingBean(KnowledgeEmbeddingPort.class)
    KnowledgeEmbeddingPort ragEmbeddingAdapter(EmbeddingModel embeddingModel) {
        return new SpringAiKnowledgeEmbeddingAdapter(embeddingModel);
    }
}
