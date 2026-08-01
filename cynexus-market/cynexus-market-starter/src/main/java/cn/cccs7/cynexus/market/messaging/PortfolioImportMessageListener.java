package cn.cccs7.cynexus.market.messaging;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportAssetBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportAssetStorePort;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessingDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportStatus;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioTradeVisionGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.time.OffsetDateTime;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "cynexus.messaging.rabbit", name = "enabled", havingValue = "true")
public class PortfolioImportMessageListener {
    private final ObjectMapper objectMapper;
    private final PortfolioImportTaskRepository taskRepository;
    private final PortfolioImportAssetStorePort assetStore;
    private final PortfolioImportProcessingDomainService processingService;
    private final PortfolioTradeVisionGateway visionGateway;
    private final RabbitTemplate rabbitTemplate;

    public PortfolioImportMessageListener(ObjectMapper objectMapper, PortfolioImportTaskRepository taskRepository,
                                          PortfolioImportAssetStorePort assetStore,
                                          PortfolioImportProcessingDomainService processingService,
                                          PortfolioTradeVisionGateway visionGateway,
                                          RabbitTemplate rabbitTemplate) {
        this.objectMapper = objectMapper;
        this.taskRepository = taskRepository;
        this.assetStore = assetStore;
        this.processingService = processingService;
        this.visionGateway = visionGateway;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = PortfolioRabbitTopologyConfiguration.PARSE_QUEUE,
            ackMode = "MANUAL", concurrency = "2", containerFactory = "portfolioRabbitListenerContainerFactory")
    @Transactional
    public void consume(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        PortfolioImportTaskBO processingTask = null;
        try {
            JsonNode payload = objectMapper.readTree(message.getBody());
            long taskId = payload.path("taskId").asLong(0);
            String principal = header(message, "principal", "workspace-user");
            String tenant = header(message, "tenant", "default");
            if (taskId <= 0) throw new IllegalArgumentException("taskId is required");
            PortfolioImportTaskBO task = taskRepository.findByOwnerAndId(principal, tenant, taskId)
                    .orElseThrow(() -> new IllegalArgumentException("portfolio task not found"));
            if (isTerminal(task.getStatus())) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (task.getStatus() != PortfolioImportStatus.PROCESSING) {
                task.claimForProcessing(OffsetDateTime.now());
                taskRepository.update(task);
            }
            processingTask = task;
            PortfolioImportAssetBO asset = assetStore.findById(task.getAssetId())
                    .orElseThrow(() -> new IllegalArgumentException("portfolio screenshot asset not found"));
            String result = visionGateway.parse(asset);
            processingService.complete(task, result, OffsetDateTime.now());
            channel.basicAck(deliveryTag, false);
        } catch (IllegalArgumentException exception) {
            if (processingTask != null && processingTask.getStatus() == PortfolioImportStatus.PROCESSING) {
                processingTask.markFailed("PORTFOLIO_PARSE_INVALID", exception.getMessage(), OffsetDateTime.now());
                taskRepository.update(processingTask);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception exception) {
            dispatchRetry(message, exception);
            channel.basicAck(deliveryTag, false);
        }
    }

    private void dispatchRetry(Message message, Exception exception) {
        int count = ((Number) message.getMessageProperties().getHeaders()
                .getOrDefault("x-portfolio-retry-count", 0)).intValue();
        if (count >= 3) {
            rabbitTemplate.send(PortfolioRabbitTopologyConfiguration.DEAD_EXCHANGE, "portfolio.import.failed.v1",
                    message);
            return;
        }
        String key = count == 0 ? "retry.30s" : count == 1 ? "retry.2m" : "retry.10m";
        message.getMessageProperties().setHeader("x-portfolio-retry-count", count + 1);
        message.getMessageProperties().setHeader("x-portfolio-last-error",
                truncate(exception.getMessage()));
        rabbitTemplate.send(PortfolioRabbitTopologyConfiguration.RETRY_EXCHANGE, key, message);
    }

    private static boolean isTerminal(PortfolioImportStatus status) {
        return status == PortfolioImportStatus.CONFIRMED || status == PortfolioImportStatus.CANCELLED
                || status == PortfolioImportStatus.READY_FOR_REVIEW;
    }

    private static String header(Message message, String name, String fallback) {
        Object value = message.getMessageProperties().getHeaders().get(name);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    private static String truncate(String value) {
        if (value == null) return "unknown processing error";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
