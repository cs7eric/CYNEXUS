package cn.cccs7.cynexus.wechat.application.service;

import cn.cccs7.cynexus.wechat.api.dto.WechatPushFailureDTO;
import cn.cccs7.cynexus.wechat.api.dto.WechatPushResultDTO;
import cn.cccs7.cynexus.wechat.api.dto.WechatTextPushRequest;
import cn.cccs7.cynexus.wechat.domain.gateway.WechatMessageGateway;
import cn.cccs7.cynexus.wechat.domain.model.WechatPushResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WechatMessageAppService {
    private final WechatMessageGateway messageGateway;

    public WechatMessageAppService(WechatMessageGateway messageGateway) {
        this.messageGateway = messageGateway;
    }

    public WechatPushResultDTO pushText(WechatTextPushRequest request) {
        WechatPushResult result = messageGateway.pushText(request.getOpenIds(), request.getContent());
        return new WechatPushResultDTO(result.requested(), result.sent(),
                result.failures().stream().map(failure -> new WechatPushFailureDTO(failure.openId(), failure.error())).toList());
    }
}
