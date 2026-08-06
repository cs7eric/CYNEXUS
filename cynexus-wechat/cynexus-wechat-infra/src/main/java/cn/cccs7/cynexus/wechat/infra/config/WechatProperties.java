package cn.cccs7.cynexus.wechat.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cynexus.wechat")
public class WechatProperties {
    private String token = "";
    private String appId = "";
    private String appSecret = "";
    private String apiBaseUrl = "https://api.weixin.qq.com";
    private long timeoutMs = 10000;
    private long accessTokenCacheSeconds = 6000;
    private String welcomeReply = "欢迎关注 Cynexus！";
    private String defaultReply = "已收到你的消息。";
    private String imageReply = "图片已收到，正在判断处理类型。";
    private String imageStorageDir = "/data/cynexus/wechat/images";
    private long imageMaxBytes = 10 * 1024 * 1024;
}
