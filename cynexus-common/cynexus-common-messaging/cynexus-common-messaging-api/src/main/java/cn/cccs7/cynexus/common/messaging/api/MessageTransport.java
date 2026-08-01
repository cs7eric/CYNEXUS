package cn.cccs7.cynexus.common.messaging.api;

public interface MessageTransport {

    MessagePublishReceipt publish(MessageDestination destination, MessageEnvelope envelope);
}
