package cn.cccs7.cynexus.wechat.application.service;

import cn.cccs7.cynexus.wechat.domain.gateway.WechatInboundMessageParser;
import cn.cccs7.cynexus.wechat.domain.gateway.WechatImageGateway;
import cn.cccs7.cynexus.wechat.domain.gateway.WechatSettingsPort;
import cn.cccs7.cynexus.wechat.domain.model.WechatInboundMessage;
import cn.cccs7.cynexus.wechat.domain.model.WechatReply;
import cn.cccs7.cynexus.wechat.domain.service.WechatEventHistoryDomainService;
import cn.cccs7.cynexus.wechat.domain.service.WechatInteractionDomainService;
import org.springframework.stereotype.Service;

@Service
public class WechatCallbackAppService {
    private final WechatSettingsPort settings;
    private final WechatInboundMessageParser messageParser;
    private final WechatImageGateway imageGateway;
    private final WechatInteractionDomainService interactionService;
    private final WechatEventHistoryDomainService historyService;

    public WechatCallbackAppService(WechatSettingsPort settings,
                                    WechatInboundMessageParser messageParser,
                                    WechatImageGateway imageGateway,
                                    WechatInteractionDomainService interactionService,
                                    WechatEventHistoryDomainService historyService) {
        this.settings = settings;
        this.messageParser = messageParser;
        this.imageGateway = imageGateway;
        this.interactionService = interactionService;
        this.historyService = historyService;
    }

    public String verify(String signature, String timestamp, String nonce, String echostr) {
        return interactionService.verifySignature(settings.token(), signature, timestamp, nonce) ? echostr : "unknown";
    }

    public String handle(String body, String signature, String timestamp, String nonce) {
        if (!interactionService.verifySignature(settings.token(), signature, timestamp, nonce)) {
            return "unknown";
        }
        WechatInboundMessage message = messageParser.parse(body);
        WechatReply reply = interactionService.reply(message, settings.welcomeReply(),
                settings.defaultReply(), settings.imageReply());
        long historyId = historyService.recordInbound(message, reply == null ? null : reply.content());
        try {
            if (message.isImage()) {
                historyService.attachImage(historyId,
                        imageGateway.downloadAndStore(message.mediaId(), message.picUrl(), message.fromUserName()));
            }
            return reply == null ? "" : reply.toXml();
        } catch (RuntimeException exception) {
            historyService.markFailed(historyId, exception.getMessage());
            throw exception;
        }
    }
}
