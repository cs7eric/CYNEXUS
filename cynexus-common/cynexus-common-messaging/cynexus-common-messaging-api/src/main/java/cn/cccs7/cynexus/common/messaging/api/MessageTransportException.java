package cn.cccs7.cynexus.common.messaging.api;

public class MessageTransportException extends RuntimeException {

    public MessageTransportException(String message) {
        super(message);
    }

    public MessageTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
