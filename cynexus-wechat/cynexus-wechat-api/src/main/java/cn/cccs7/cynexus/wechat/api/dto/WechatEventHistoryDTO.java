package cn.cccs7.cynexus.wechat.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record WechatEventHistoryDTO(
        long id,
        String messageId,
        String direction,
        String messageType,
        String eventType,
        String fromUserName,
        String toUserName,
        String content,
        String processingStatus,
        String imageContentType,
        Long imageSize,
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
}
