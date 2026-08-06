package cn.cccs7.cynexus.wechat.infra.config;

import cn.cccs7.cynexus.wechat.domain.gateway.WechatSettingsPort;
import org.springframework.stereotype.Component;

@Component
public class WechatSettingsAdapter implements WechatSettingsPort {
    private final WechatProperties properties;

    public WechatSettingsAdapter(WechatProperties properties) {
        this.properties = properties;
    }

    @Override
    public String token() {
        return properties.getToken();
    }

    @Override
    public String welcomeReply() {
        return properties.getWelcomeReply();
    }

    @Override
    public String defaultReply() {
        return properties.getDefaultReply();
    }

    @Override
    public String imageReply() {
        return properties.getImageReply();
    }
}
