package cn.cccs7.cynexus.common.image;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ImageMattingProperties.class)
public class ImageMattingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ImageMattingClient.class)
    public ImageMattingClient imageMattingClient(ImageMattingProperties properties) {
        return new HttpImageMattingClient(properties.getBaseUrl(), properties.getConnectTimeoutMs(),
                properties.getRequestTimeoutMs());
    }
}
