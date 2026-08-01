package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.common.messaging.api.MessageTransport;
import cn.cccs7.cynexus.common.messaging.outbox.OutboxRelay;
import cn.cccs7.cynexus.common.messaging.outbox.OutboxStore;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "cynexus.market.portfolio.outbox", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@ConditionalOnProperty(prefix = "cynexus.messaging.rabbit", name = "enabled", havingValue = "true")
public class PortfolioOutboxRelayConfiguration {

    @Bean
    public PortfolioOutboxRelay portfolioOutboxRelay(OutboxStore store, MessageTransport transport) {
        return new PortfolioOutboxRelay(new OutboxRelay(store, transport, 50, Duration.ofSeconds(60), 5));
    }

    public static final class PortfolioOutboxRelay {
        private final OutboxRelay delegate;
        private final String leaseOwner = "market-portfolio-" + UUID.randomUUID();

        private PortfolioOutboxRelay(OutboxRelay delegate) {
            this.delegate = delegate;
        }

        @Scheduled(fixedDelayString = "${cynexus.market.portfolio.outbox.poll-interval-ms:1000}")
        public void relay() {
            delegate.relayOnce(Instant.now(), leaseOwner);
        }
    }
}
