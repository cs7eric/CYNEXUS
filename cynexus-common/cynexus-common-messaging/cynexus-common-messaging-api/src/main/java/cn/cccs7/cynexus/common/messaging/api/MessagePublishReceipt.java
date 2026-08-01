package cn.cccs7.cynexus.common.messaging.api;

import java.time.Instant;

public record MessagePublishReceipt(String messageId, Instant confirmedAt) {

    public MessagePublishReceipt {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        if (confirmedAt == null) {
            throw new IllegalArgumentException("confirmedAt must not be null");
        }
    }
}
