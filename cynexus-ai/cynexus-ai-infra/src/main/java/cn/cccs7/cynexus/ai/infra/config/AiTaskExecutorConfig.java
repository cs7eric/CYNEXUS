package cn.cccs7.cynexus.ai.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AiTaskExecutorConfig {

    @Bean("aiVisionTaskExecutor")
    public TaskExecutor aiVisionTaskExecutor(
            @Value("${cynexus.ai.vision-task.executor-core-size:1}") int coreSize,
            @Value("${cynexus.ai.vision-task.executor-max-size:2}") int maxSize,
            @Value("${cynexus.ai.vision-task.executor-queue-capacity:0}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int safeCoreSize = Math.max(1, coreSize);
        executor.setCorePoolSize(safeCoreSize);
        executor.setMaxPoolSize(Math.max(safeCoreSize, maxSize));
        executor.setQueueCapacity(Math.max(0, queueCapacity));
        executor.setThreadNamePrefix("ai-vision-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
