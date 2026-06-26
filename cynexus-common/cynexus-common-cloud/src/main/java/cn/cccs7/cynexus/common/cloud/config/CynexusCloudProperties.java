package cn.cccs7.cynexus.common.cloud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cynexus.cloud")
public record CynexusCloudProperties(String environment, String namespace) {
}
