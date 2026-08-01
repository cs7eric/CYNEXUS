package cn.cccs7.cynexus.market.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "cynexus.messaging.rabbit", name = "enabled", havingValue = "true")
public class PortfolioRabbitTopologyConfiguration {
    public static final String COMMAND_EXCHANGE = "cynexus.command";
    public static final String PARSE_ROUTING_KEY = "portfolio.import.parse-requested.v1";
    public static final String PARSE_QUEUE = "cynexus.market.portfolio-import-parse.v1";
    public static final String RETRY_EXCHANGE = "cynexus.market.portfolio-import.retry";
    public static final String DEAD_EXCHANGE = "cynexus.market.portfolio-import.dead";
    public static final String DEAD_QUEUE = "cynexus.market.portfolio-import.dlq";

    @Bean
    DirectExchange portfolioCommandExchange() {
        return new DirectExchange(COMMAND_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange portfolioRetryExchange() {
        return new DirectExchange(RETRY_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange portfolioDeadExchange() {
        return new DirectExchange(DEAD_EXCHANGE, true, false);
    }

    @Bean
    Queue portfolioParseQueue() {
        return QueueBuilder.durable(PARSE_QUEUE).build();
    }

    @Bean
    Binding portfolioParseBinding(Queue portfolioParseQueue, DirectExchange portfolioCommandExchange) {
        return BindingBuilder.bind(portfolioParseQueue).to(portfolioCommandExchange).with(PARSE_ROUTING_KEY);
    }

    @Bean
    Queue portfolioRetry30sQueue() {
        return retryQueue("cynexus.market.portfolio-import.retry.30s", 30_000);
    }

    @Bean
    Queue portfolioRetry2mQueue() {
        return retryQueue("cynexus.market.portfolio-import.retry.2m", 120_000);
    }

    @Bean
    Queue portfolioRetry10mQueue() {
        return retryQueue("cynexus.market.portfolio-import.retry.10m", 600_000);
    }

    @Bean
    Binding portfolioRetry30sBinding(Queue portfolioRetry30sQueue, DirectExchange portfolioRetryExchange) {
        return BindingBuilder.bind(portfolioRetry30sQueue).to(portfolioRetryExchange).with("retry.30s");
    }

    @Bean
    Binding portfolioRetry2mBinding(Queue portfolioRetry2mQueue, DirectExchange portfolioRetryExchange) {
        return BindingBuilder.bind(portfolioRetry2mQueue).to(portfolioRetryExchange).with("retry.2m");
    }

    @Bean
    Binding portfolioRetry10mBinding(Queue portfolioRetry10mQueue, DirectExchange portfolioRetryExchange) {
        return BindingBuilder.bind(portfolioRetry10mQueue).to(portfolioRetryExchange).with("retry.10m");
    }

    @Bean
    Queue portfolioDeadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    @Bean
    Binding portfolioDeadBinding(Queue portfolioDeadQueue, DirectExchange portfolioDeadExchange) {
        return BindingBuilder.bind(portfolioDeadQueue).to(portfolioDeadExchange).with("portfolio.import.failed.v1");
    }

    @Bean
    SimpleRabbitListenerContainerFactory portfolioRabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(2);
        factory.setPrefetchCount(1);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }

    private Queue retryQueue(String name, int ttl) {
        return QueueBuilder.durable(name).ttl(ttl)
                .deadLetterExchange(COMMAND_EXCHANGE).deadLetterRoutingKey(PARSE_ROUTING_KEY).build();
    }
}
