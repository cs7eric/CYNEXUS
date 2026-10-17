package cn.cccs7.cynexus.ai.infra.ai.rag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.cccs7.cynexus.ai.domain.port.KnowledgeIndexingPort;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class KnowledgeIndexRabbitListenerTest {
    @Test void malformedIdentityIsDeadLetteredWithoutIndexing() throws Exception {
        KnowledgeIndexingPort indexing = mock(KnowledgeIndexingPort.class);
        Channel channel = mock(Channel.class);
        KnowledgeIndexRabbitListener listener = new KnowledgeIndexRabbitListener(indexing);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(7L);
        listener.upsert(new Message("{\"documentId\":\"doc\",\"content\":\"text\"}".getBytes(), properties), channel);
        verify(channel).basicNack(7L, false, false);
        verifyNoInteractions(indexing);
    }

    @Test void validChangeIsAcknowledgedAfterIndexing() throws Exception {
        KnowledgeIndexingPort indexing = mock(KnowledgeIndexingPort.class);
        Channel channel = mock(Channel.class);
        KnowledgeIndexRabbitListener listener = new KnowledgeIndexRabbitListener(indexing);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(8L);
        String payload = "{\"documentId\":\"doc\",\"tenantKey\":\"tenant\",\"ownerKey\":\"owner\",\"content\":\"text\",\"status\":\"published\"}";
        listener.upsert(new Message(payload.getBytes(), properties), channel);
        verify(indexing).index(any(KnowledgeIndexingPort.IndexCommand.class));
        verify(channel).basicAck(8L, false);
    }

    @Test void indexingArgumentFailureIsPropagatedForRetry() throws Exception {
        KnowledgeIndexingPort indexing = mock(KnowledgeIndexingPort.class);
        doThrow(new IllegalArgumentException("provider rejected request"))
                .when(indexing).index(any(KnowledgeIndexingPort.IndexCommand.class));
        Channel channel = mock(Channel.class);
        KnowledgeIndexRabbitListener listener = new KnowledgeIndexRabbitListener(indexing);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(9L);
        String payload = "{\"documentId\":\"doc\",\"tenantKey\":\"tenant\",\"ownerKey\":\"owner\",\"content\":\"text\",\"status\":\"published\"}";

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> listener.upsert(new Message(payload.getBytes(), properties), channel));
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test void completeEnvelopeWithPayloadJsonIsAccepted() throws Exception {
        KnowledgeIndexingPort indexing = mock(KnowledgeIndexingPort.class);
        Channel channel = mock(Channel.class);
        KnowledgeIndexRabbitListener listener = new KnowledgeIndexRabbitListener(indexing);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(10L);
        String body = "{\"messageId\":\"m-1\",\"payloadJson\":\"{\\\"documentId\\\":\\\"doc\\\",\\\"tenantKey\\\":\\\"tenant\\\",\\\"ownerKey\\\":\\\"owner\\\",\\\"content\\\":\\\"text\\\"}\"}";

        listener.upsert(new Message(body.getBytes(), properties), channel);

        verify(indexing).index(any(KnowledgeIndexingPort.IndexCommand.class));
        verify(channel).basicAck(10L, false);
    }

    @Test void envelopeWithObjectPayloadIsAccepted() throws Exception {
        KnowledgeIndexingPort indexing = mock(KnowledgeIndexingPort.class);
        Channel channel = mock(Channel.class);
        KnowledgeIndexRabbitListener listener = new KnowledgeIndexRabbitListener(indexing);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(11L);
        String body = "{\"messageId\":\"m-2\",\"payload\":{\"documentId\":\"doc\",\"tenantKey\":\"tenant\",\"ownerKey\":\"owner\",\"content\":\"text\"}}";

        listener.upsert(new Message(body.getBytes(), properties), channel);

        verify(indexing).index(any(KnowledgeIndexingPort.IndexCommand.class));
        verify(channel).basicAck(11L, false);
    }
}
