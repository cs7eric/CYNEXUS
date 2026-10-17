package cn.cccs7.cynexus.ai.infra.ai.rag;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;

@Configuration
@ConditionalOnProperty(prefix = "cynexus.messaging.rabbit", name = "enabled", havingValue = "true")
public class KnowledgeIndexRabbitTopologyConfiguration {
    public static final String EXCHANGE = "cynexus.event";
    public static final String UPSERT_KEY = "knowledge.document.changed.v1";
    public static final String DELETE_KEY = "knowledge.document.deleted.v1";
    public static final String UPSERT_QUEUE = "cynexus.ai.knowledge-index-upsert.v1";
    public static final String DELETE_QUEUE = "cynexus.ai.knowledge-index-delete.v1";
    public static final String DLX = "cynexus.event.dlx";
    public static final String UPSERT_DLQ = UPSERT_QUEUE + ".dlq";
    public static final String DELETE_DLQ = DELETE_QUEUE + ".dlq";
    @Bean DirectExchange knowledgeDocumentEventExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean DirectExchange knowledgeDocumentDeadLetterExchange() { return new DirectExchange(DLX, true, false); }
    @Bean Queue knowledgeDocumentUpsertQueue() { return QueueBuilder.durable(UPSERT_QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey(UPSERT_DLQ).build(); }
    @Bean Queue knowledgeDocumentDeleteQueue() { return QueueBuilder.durable(DELETE_QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey(DELETE_DLQ).build(); }
    @Bean Queue knowledgeDocumentUpsertDlq() { return QueueBuilder.durable(UPSERT_DLQ).build(); }
    @Bean Queue knowledgeDocumentDeleteDlq() { return QueueBuilder.durable(DELETE_DLQ).build(); }
    @Bean Binding knowledgeDocumentUpsertBinding(Queue knowledgeDocumentUpsertQueue, DirectExchange knowledgeDocumentEventExchange) { return BindingBuilder.bind(knowledgeDocumentUpsertQueue).to(knowledgeDocumentEventExchange).with(UPSERT_KEY); }
    @Bean Binding knowledgeDocumentDeleteBinding(Queue knowledgeDocumentDeleteQueue, DirectExchange knowledgeDocumentEventExchange) { return BindingBuilder.bind(knowledgeDocumentDeleteQueue).to(knowledgeDocumentEventExchange).with(DELETE_KEY); }
    @Bean Binding knowledgeDocumentUpsertDlqBinding(Queue knowledgeDocumentUpsertDlq, DirectExchange knowledgeDocumentDeadLetterExchange) { return BindingBuilder.bind(knowledgeDocumentUpsertDlq).to(knowledgeDocumentDeadLetterExchange).with(UPSERT_DLQ); }
    @Bean Binding knowledgeDocumentDeleteDlqBinding(Queue knowledgeDocumentDeleteDlq, DirectExchange knowledgeDocumentDeadLetterExchange) { return BindingBuilder.bind(knowledgeDocumentDeleteDlq).to(knowledgeDocumentDeadLetterExchange).with(DELETE_DLQ); }
    @Bean SimpleRabbitListenerContainerFactory knowledgeIndexRabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory); factory.setConcurrentConsumers(1); factory.setMaxConcurrentConsumers(2); factory.setPrefetchCount(1);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3).backOffOptions(500L, 2.0, 5000L).build());
        return factory;
    }
}
