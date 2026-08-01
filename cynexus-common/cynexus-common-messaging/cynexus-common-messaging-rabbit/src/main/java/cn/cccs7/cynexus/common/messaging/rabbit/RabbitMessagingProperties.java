package cn.cccs7.cynexus.common.messaging.rabbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cynexus.messaging.rabbit")
public class RabbitMessagingProperties {

    private boolean enabled;
    private boolean mandatory = true;
    private Duration confirmTimeout = Duration.ofSeconds(5);

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
}
