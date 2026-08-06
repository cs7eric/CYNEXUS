package cn.cccs7.cynexus.wechat.domain.gateway;

import cn.cccs7.cynexus.wechat.domain.model.WechatPushResult;
import java.util.List;

public interface WechatMessageGateway {
    WechatPushResult pushText(List<String> openIds, String content);
}
