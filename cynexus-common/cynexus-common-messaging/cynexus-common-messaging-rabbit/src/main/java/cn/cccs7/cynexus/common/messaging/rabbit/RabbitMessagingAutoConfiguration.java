package cn.cccs7.cynexus.common.messaging.rabbit;

import cn.cccs7.cynexus.common.messaging.api.MessageTransport;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "cynexus.messaging.rabbit", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RabbitMessagingProperties.class)
public class RabbitMessagingAutoConfiguration {

    @Bean
    @ConditionalOnBean(RabbitTemplate.class)
    @ConditionalOnMissingBean(MessageTransport.class)
    public MessageTransport rabbitMessageTransport(
            RabbitTemplate rabbitTemplate,
            RabbitMessagingProperties properties,
            ConnectionFactory connectionFactory
    ) {
        if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
            cachingConnectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            cachingConnectionFactory.setPublisherReturns(properties.isMandatory());
        }
        rabbitTemplate.setMandatory(properties.isMandatory());
        return new RabbitMessageTransport(rabbitTemplate, properties);
    }
}
