package cn.cccs7.cynexus.common.image;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("cynexus.image.matting")
public class ImageMattingProperties {

    private String baseUrl = "http://host.docker.internal:8000";
    private long connectTimeoutMs = 10_000;
    private long requestTimeoutMs = 120_000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }
}
