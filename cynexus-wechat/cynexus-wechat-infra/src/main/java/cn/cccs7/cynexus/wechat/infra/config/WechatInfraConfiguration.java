package cn.cccs7.cynexus.wechat.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WechatProperties.class)
public class WechatInfraConfiguration {
}
