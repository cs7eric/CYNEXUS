package cn.cccs7.cynexus.wechat.domain.service;

import cn.cccs7.cynexus.wechat.domain.model.WechatEventHistory;
import cn.cccs7.cynexus.wechat.domain.model.WechatEventHistoryPage;
import cn.cccs7.cynexus.wechat.domain.model.WechatImageRoute;
import cn.cccs7.cynexus.wechat.domain.model.WechatInboundMessage;
import cn.cccs7.cynexus.wechat.domain.model.WechatStoredImage;
import cn.cccs7.cynexus.wechat.domain.repository.WechatEventHistoryRepository;
import java.util.Objects;

public class WechatEventHistoryDomainService {
    private final WechatEventHistoryRepository repository;

    public WechatEventHistoryDomainService(WechatEventHistoryRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public long recordInbound(WechatInboundMessage message, String replyContent) {
        return repository.create(WechatEventHistory.inbound(message, replyContent));
    }

    public void attachImage(long id, WechatStoredImage image) {
        repository.attachImage(id, image);
    }

    public void markFailed(long id, String errorMessage) {
        repository.markFailed(id, errorMessage);
    }

    public WechatEventHistory claimNextImage() {
        return repository.claimNextImage();
    }

    public void completeRouting(long id, WechatImageRoute route) {
        repository.completeRouting(id, route);
    }

    public void failRouting(long id, String errorMessage) {
        repository.failRouting(id, errorMessage);
    }

    public WechatEventHistoryPage list(int limit, int offset, String messageType, String processingStatus) {
        return repository.list(limit, offset, messageType, processingStatus);
    }
}
