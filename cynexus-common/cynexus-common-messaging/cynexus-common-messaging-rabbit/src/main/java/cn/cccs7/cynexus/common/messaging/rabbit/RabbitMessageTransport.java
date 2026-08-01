package cn.cccs7.cynexus.common.messaging.rabbit;

import cn.cccs7.cynexus.common.messaging.api.MessageDestination;
import cn.cccs7.cynexus.common.messaging.api.MessageEnvelope;
import cn.cccs7.cynexus.common.messaging.api.MessagePublishReceipt;
import cn.cccs7.cynexus.common.messaging.api.MessageTransport;
import cn.cccs7.cynexus.common.messaging.api.MessageTransportException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageBuilderSupport;
import org.springframework.amqp.AmqpException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RabbitMessageTransport implements MessageTransport {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMessagingProperties properties;

    public RabbitMessageTransport(RabbitTemplate rabbitTemplate, RabbitMessagingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public MessagePublishReceipt publish(MessageDestination destination, MessageEnvelope envelope) {
        byte[] payload = envelope.payloadJson().getBytes(StandardCharsets.UTF_8);
        validateSize(envelope, payload);
        CorrelationData correlationData = new CorrelationData(envelope.messageId());
        MessageBuilderSupport<Message> builder = MessageBuilder
                .withBody(payload)
                .setContentType("application/json")
                .setMessageId(envelope.messageId())
                .setCorrelationId(envelope.correlationId())
                .setHeader("messageType", envelope.messageType().name())
                .setHeader("messageName", envelope.name())
                .setHeader("schemaVersion", envelope.schemaVersion())
                .setHeader("producer", envelope.producer())
                .setHeader("aggregateType", envelope.aggregateType())
                .setHeader("aggregateId", envelope.aggregateId())
                .setHeader("traceId", envelope.traceId())
                .setHeader("causationId", envelope.causationId());
        envelope.headers().forEach(builder::setHeader);
        Message message = builder.build();
        try {
            rabbitTemplate.send(destination.exchange(), destination.routingKey(), message, correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(properties.getConfirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (confirm == null || !confirm.ack()) {
                String reason = confirm == null ? "missing publisher confirm" : confirm.reason();
                throw new MessageTransportException("RabbitMQ publisher was not confirmed: " + reason);
            }
            if (correlationData.getReturned() != null) {
                throw new MessageTransportException("RabbitMQ publisher returned the message: "
                        + correlationData.getReturned().getReplyText());
            }
            return new MessagePublishReceipt(envelope.messageId(), Instant.now());
        } catch (AmqpException exception) {
            throw new MessageTransportException("RabbitMQ publish failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MessageTransportException("Interrupted while waiting for RabbitMQ publisher confirm", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new MessageTransportException("RabbitMQ publisher confirm failed", exception);
        }
    }

    private void validateSize(MessageEnvelope envelope, byte[] payload) {
        if (payload.length > properties.getMaxPayloadBytes()) {
            throw new MessageTransportException("RabbitMQ message payload exceeds configured limit");
        }
        int headerBytes = envelope.headers().entrySet().stream()
                .mapToInt(entry -> utf8Length(entry.getKey()) + utf8Length(entry.getValue()))
                .sum();
        if (headerBytes > properties.getMaxHeaderBytes()) {
            throw new MessageTransportException("RabbitMQ message headers exceed configured limit");
        }
    }

    private static int utf8Length(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }
}
