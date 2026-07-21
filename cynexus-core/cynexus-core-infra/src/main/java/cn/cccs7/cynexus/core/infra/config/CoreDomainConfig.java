package cn.cccs7.cynexus.core.infra.config;

import cn.cccs7.cynexus.core.domain.client.DataJobExecutorClient;
import cn.cccs7.cynexus.core.domain.repository.SchedulerRepository;
import cn.cccs7.cynexus.core.domain.coreinfo.CoreServiceInfoDomainService;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDomainService;
import cn.cccs7.cynexus.core.domain.coreinfo.impl.CoreServiceInfoDomainServiceImpl;
import cn.cccs7.cynexus.core.domain.scheduler.impl.SchedulerDomainServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class CoreDomainConfig {

    @Bean
    public CoreServiceInfoDomainService coreServiceInfoDomainService() {
        return new CoreServiceInfoDomainServiceImpl();
    }

    @Bean
    public SchedulerDomainService schedulerDomainService(SchedulerRepository schedulerRepository,
                                                         DataJobExecutorClient dataJobExecutorClient) {
        return new SchedulerDomainServiceImpl(schedulerRepository, dataJobExecutorClient);
    }

    @Bean("mediaTranscriptionProcessExecutor")
    public TaskExecutor mediaTranscriptionProcessExecutor(
            @Value("${cynexus.media-transcription.process.executor-core-size:2}") int coreSize,
            @Value("${cynexus.media-transcription.process.executor-max-size:4}") int maxSize,
            @Value("${cynexus.media-transcription.process.executor-queue-capacity:20}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, coreSize));
        executor.setMaxPoolSize(Math.max(Math.max(1, coreSize), maxSize));
        executor.setQueueCapacity(Math.max(1, queueCapacity));
        executor.setThreadNamePrefix("media-process-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
