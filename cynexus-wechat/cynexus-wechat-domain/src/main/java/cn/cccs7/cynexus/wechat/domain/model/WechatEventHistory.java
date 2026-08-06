package cn.cccs7.cynexus.wechat.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record WechatEventHistory(
        long id,
        String messageId,
        String direction,
        String messageType,
        String eventType,
        String fromUserName,
        String toUserName,
        String content,
        String mediaId,
        String picUrl,
        String storagePath,
        String imageContentType,
        Long imageSize,
        String processingStatus,
        String routeCode,
        BigDecimal routeConfidence,
        String routeReason,
        List<String> routeSignals,
        List<String> routeWarnings,
        String replyContent,
        String errorMessage,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static WechatEventHistory inbound(WechatInboundMessage message, String replyContent) {
        return new WechatEventHistory(
                0L,
                message == null ? null : message.messageId(),
                "INBOUND",
                message == null ? null : message.messageType(),
                message == null ? null : message.event(),
                message == null ? null : message.fromUserName(),
                message == null ? null : message.toUserName(),
                message == null ? null : message.content(),
                message == null ? null : message.mediaId(),
                message == null ? null : message.picUrl(),
                null,
                null,
                null,
                message != null && message.isImage() ? "RECEIVED" : "COMPLETED",
                null,
                null,
                null,
                List.of(),
                List.of(),
                replyContent,
                null,
                null,
                null,
                null);
    }
}
