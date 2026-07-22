package cn.cccs7.cynexus.ai.infra.basic.redis.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Tag("integration")
class RedisAgentRateLimitConcurrencyTest {
    private LettuceConnectionFactory connectionFactory;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        boolean enabled = Boolean.parseBoolean(System.getProperty("cynexus.redis.integration", "false"));
        Assumptions.assumeTrue(enabled, "Set -Dcynexus.redis.integration=true to run the Redis concurrency gate");
        String host = System.getProperty("cynexus.redis.host", "127.0.0.1");
        int port = Integer.parseInt(System.getProperty("cynexus.redis.port", "6379"));
        connectionFactory = new LettuceConnectionFactory(host, port);
        String password = System.getProperty("cynexus.redis.password", "C020611.");
        if (!password.isBlank()) connectionFactory.setPassword(password);
        connectionFactory.afterPropertiesSet();
        executor = Executors.newFixedThreadPool(32);
    }

    @AfterEach
    void tearDown() {
        if (executor != null) executor.shutdownNow();
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @Test
    void atomicallyCapsConcurrentRequestsForOneOwner() throws Exception {
        StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        RedisAgentRuntimeLease limiter = new RedisAgentRuntimeLease(redis);
        String owner = "security-gate-" + UUID.randomUUID();
        int workers = 32;
        int callsPerWorker = 8;
        int attempts = workers * callsPerWorker;
        int limit = 30;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>(workers);
        for (int index = 0; index < workers; index++) {
            results.add(executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                int allowed = 0;
                for (int call = 0; call < callsPerWorker; call++) {
                    if (limiter.allow(owner, limit, 5)) allowed++;
                }
                return allowed;
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        long allowed = 0;
        for (Future<Integer> result : results) allowed += result.get(15, TimeUnit.SECONDS);
        assertThat(allowed).isEqualTo(limit);
    }
}
