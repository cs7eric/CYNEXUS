package cn.cccs7.cynexus.common.messaging.api;

public record MessageDestination(String exchange, String routingKey) {

    public MessageDestination {
        requireText(exchange, "exchange");
        requireText(routingKey, "routingKey");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
