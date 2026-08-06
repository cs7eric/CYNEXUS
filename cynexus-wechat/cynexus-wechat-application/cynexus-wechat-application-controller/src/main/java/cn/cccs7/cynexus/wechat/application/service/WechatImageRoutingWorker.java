package cn.cccs7.cynexus.wechat.application.service;

import cn.cccs7.cynexus.wechat.domain.model.WechatEventHistory;
import cn.cccs7.cynexus.wechat.domain.model.WechatImageRoute;
import cn.cccs7.cynexus.wechat.domain.model.WechatStoredImage;
import cn.cccs7.cynexus.wechat.domain.service.WechatEventHistoryDomainService;
import cn.cccs7.cynexus.wechat.domain.service.WechatImageRoutingDomainService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WechatImageRoutingWorker {
    private final WechatEventHistoryDomainService historyService;
    private final WechatImageRoutingDomainService routingService;

    public WechatImageRoutingWorker(WechatEventHistoryDomainService historyService,
                                    WechatImageRoutingDomainService routingService) {
        this.historyService = historyService;
        this.routingService = routingService;
    }

    @Scheduled(fixedDelayString = "${cynexus.wechat.routing.poll-delay-ms:5000}")
    public void processNext() {
        WechatEventHistory event = historyService.claimNextImage();
        if (event == null || event.storagePath() == null || event.storagePath().isBlank()) return;
        try {
            WechatStoredImage image = new WechatStoredImage(
                    event.mediaId(), event.picUrl(), event.storagePath(), event.imageContentType(),
                    event.imageSize() == null ? 0L : event.imageSize());
            WechatImageRoute route = routingService.classify(image);
            historyService.completeRouting(event.id(), route);
        } catch (Exception exception) {
            historyService.failRouting(event.id(), exception.getMessage());
        }
    }
}
