package cn.cccs7.cynexus.wechat.domain.gateway;

public interface WechatSettingsPort {
    String token();

    String welcomeReply();

    String defaultReply();

    String imageReply();
}
