package cn.cccs7.cynexus.common.messaging.rabbit;

import cn.cccs7.cynexus.common.messaging.api.MessageDestination;
import cn.cccs7.cynexus.common.messaging.api.MessageEnvelope;
import cn.cccs7.cynexus.common.messaging.api.MessagePublishReceipt;
import cn.cccs7.cynexus.common.messaging.api.MessageTransportException;
import cn.cccs7.cynexus.common.messaging.api.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class RabbitMessageTransportTest {

    private static final MessageDestination DESTINATION = new MessageDestination("portfolio.events", "import.completed");

    @Test
    void publishesAndReturnsConfirmedReceipt() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        MessagePublishReceipt receipt = transport(template).publish(DESTINATION, envelope());

        assertEquals("message-1", receipt.messageId());
    }

    @Test
    void rejectsNegativePublisherConfirm() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.getFuture().complete(new CorrelationData.Confirm(false, "exchange not found"));
            return null;
        }).when(template).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        MessageTransportException exception = assertThrows(MessageTransportException.class,
                () -> transport(template).publish(DESTINATION, envelope()));

        assertTrue(exception.getMessage().contains("exchange not found"));
    }

    @Test
    void rejectsReturnedMessageEvenWhenBrokerConfirmsPublish() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.setReturned(new ReturnedMessage(
                    invocation.getArgument(2), 312, "NO_ROUTE", "portfolio.events", "import.completed"));
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        assertThrows(MessageTransportException.class,
                () -> transport(template).publish(DESTINATION, envelope()));
    }

    @Test
    void wrapsImmediateRabbitFailure() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(invocation -> {
            throw new AmqpException("connection refused");
        }).when(template).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        MessageTransportException exception = assertThrows(MessageTransportException.class,
                () -> transport(template).publish(DESTINATION, envelope()));

        assertTrue(exception.getMessage().contains("publish failed"));
    }

    @Test
    void rejectsOversizedPayloadBeforeOpeningBrokerCall() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitMessagingProperties properties = new RabbitMessagingProperties();
        properties.setMaxPayloadBytes(4);
        RabbitMessageTransport transport = new RabbitMessageTransport(template, properties);

        assertThrows(MessageTransportException.class,
                () -> transport.publish(DESTINATION, envelope()));
    }

    private static RabbitMessageTransport transport(RabbitTemplate template) {
        RabbitMessagingProperties properties = new RabbitMessagingProperties();
        properties.setConfirmTimeout(Duration.ofSeconds(1));
        return new RabbitMessageTransport(template, properties);
    }

    private static MessageEnvelope envelope() {
        return new MessageEnvelope(
                "message-1", MessageType.EVENT, "portfolio.import.completed", 1,
                "cynexus-market", "portfolio-import", "import-1", Instant.now(),
                "trace-1", "correlation-1", null, "{\"status\":\"COMPLETED\"}",
                Map.of("tenant", "demo")
        );
    }
}
