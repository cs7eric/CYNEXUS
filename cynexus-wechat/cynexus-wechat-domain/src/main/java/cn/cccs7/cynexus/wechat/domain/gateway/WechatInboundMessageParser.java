package cn.cccs7.cynexus.wechat.domain.gateway;

import cn.cccs7.cynexus.wechat.domain.model.WechatInboundMessage;

public interface WechatInboundMessageParser {
    WechatInboundMessage parse(String xml);
}
