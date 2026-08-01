package cn.cccs7.cynexus.common.messaging.rabbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cynexus.messaging.rabbit")
public class RabbitMessagingProperties {

    private boolean enabled;
    private boolean mandatory = true;
    private Duration confirmTimeout = Duration.ofSeconds(5);
    private int maxPayloadBytes = 64 * 1024;
    private int maxHeaderBytes = 8 * 1024;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Duration getConfirmTimeout() {
        return confirmTimeout;
    }

    public void setConfirmTimeout(Duration confirmTimeout) {
        if (confirmTimeout == null || confirmTimeout.isNegative() || confirmTimeout.isZero()) {
            throw new IllegalArgumentException("confirmTimeout must be positive");
        }
        this.confirmTimeout = confirmTimeout;
    }

    public int getMaxPayloadBytes() {
        return maxPayloadBytes;
    }

    public void setMaxPayloadBytes(int maxPayloadBytes) {
        if (maxPayloadBytes < 1) {
            throw new IllegalArgumentException("maxPayloadBytes must be positive");
        }
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public int getMaxHeaderBytes() {
        return maxHeaderBytes;
    }

    public void setMaxHeaderBytes(int maxHeaderBytes) {
        if (maxHeaderBytes < 1) {
            throw new IllegalArgumentException("maxHeaderBytes must be positive");
        }
        this.maxHeaderBytes = maxHeaderBytes;
    }
}
