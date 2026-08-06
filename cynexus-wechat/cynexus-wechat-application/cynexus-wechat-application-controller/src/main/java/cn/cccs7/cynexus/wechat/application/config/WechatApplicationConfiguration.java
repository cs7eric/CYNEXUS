package cn.cccs7.cynexus.wechat.application.config;

import cn.cccs7.cynexus.wechat.domain.service.WechatInteractionDomainService;
import cn.cccs7.cynexus.wechat.domain.gateway.WechatVisionGateway;
import cn.cccs7.cynexus.wechat.domain.repository.WechatEventHistoryRepository;
import cn.cccs7.cynexus.wechat.domain.service.WechatEventHistoryDomainService;
import cn.cccs7.cynexus.wechat.domain.service.WechatImageRoutingDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WechatApplicationConfiguration {
    @Bean
    public WechatInteractionDomainService wechatInteractionDomainService() {
        return new WechatInteractionDomainService();
    }

    @Bean
    public WechatEventHistoryDomainService wechatEventHistoryDomainService(
            WechatEventHistoryRepository repository) {
        return new WechatEventHistoryDomainService(repository);
    }

    @Bean
    public WechatImageRoutingDomainService wechatImageRoutingDomainService(
            WechatVisionGateway visionGateway) {
        return new WechatImageRoutingDomainService(visionGateway);
    }
}
