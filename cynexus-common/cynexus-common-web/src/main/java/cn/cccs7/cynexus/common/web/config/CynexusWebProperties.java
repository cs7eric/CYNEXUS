package cn.cccs7.cynexus.common.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cynexus.web")
public record CynexusWebProperties(boolean wrapResponse, boolean requestLogEnabled) {
}
