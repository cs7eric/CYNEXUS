package cn.cccs7.cynexus.wechat.domain.gateway;

import cn.cccs7.cynexus.wechat.domain.model.WechatStoredImage;
import cn.cccs7.cynexus.wechat.domain.model.WechatVisionResult;

public interface WechatVisionGateway {
    WechatVisionResult classify(WechatStoredImage image);
}
