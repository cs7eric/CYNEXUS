package cn.cccs7.cynexus.wechat.application.service;

import cn.cccs7.cynexus.wechat.api.dto.WechatEventHistoryDTO;
import cn.cccs7.cynexus.wechat.api.dto.WechatEventHistoryPageDTO;
import cn.cccs7.cynexus.wechat.domain.model.WechatEventHistory;
import cn.cccs7.cynexus.wechat.domain.model.WechatEventHistoryPage;
import cn.cccs7.cynexus.wechat.domain.policy.WechatPrivacyPolicy;
import cn.cccs7.cynexus.wechat.domain.service.WechatEventHistoryDomainService;
import org.springframework.stereotype.Service;

@Service
public class WechatHistoryAppService {
    private final WechatEventHistoryDomainService domainService;

    public WechatHistoryAppService(WechatEventHistoryDomainService domainService) {
        this.domainService = domainService;
    }

    public WechatEventHistoryPageDTO list(int limit, int offset, String messageType, String processingStatus) {
        WechatEventHistoryPage page = domainService.list(limit, offset, messageType, processingStatus);
        return new WechatEventHistoryPageDTO(
                page.items().stream().map(this::toDto).toList(), page.total(), page.limit(), page.offset());
    }

    private WechatEventHistoryDTO toDto(WechatEventHistory value) {
        return new WechatEventHistoryDTO(
                value.id(),
                value.messageId(),
                value.direction(),
                value.messageType(),
                value.eventType(),
                WechatPrivacyPolicy.maskIdentifier(value.fromUserName()),
                WechatPrivacyPolicy.maskIdentifier(value.toUserName()),
                value.content(),
                value.processingStatus(),
                value.imageContentType(),
                value.imageSize(),
                value.routeCode(),
                value.routeConfidence(),
                value.routeReason(),
                value.routeSignals(),
                value.routeWarnings(),
                value.replyContent(),
                value.errorMessage(),
                value.occurredAt(),
                value.createdAt(),
                value.updatedAt());
    }
}
