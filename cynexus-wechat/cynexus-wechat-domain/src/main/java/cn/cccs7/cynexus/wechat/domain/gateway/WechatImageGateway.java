package cn.cccs7.cynexus.wechat.domain.gateway;

import cn.cccs7.cynexus.wechat.domain.model.WechatStoredImage;

public interface WechatImageGateway {
    WechatStoredImage downloadAndStore(String mediaId, String picUrl, String fromUserName);
}
