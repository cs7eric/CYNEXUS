package cn.cccs7.cynexus.market.messaging;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportAssetBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportAssetStorePort;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessingDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportStatus;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessEventType;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryRepository;
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
    private final PortfolioImportProcessHistoryRepository historyRepository;

    public PortfolioImportMessageListener(ObjectMapper objectMapper, PortfolioImportTaskRepository taskRepository,
                                          PortfolioImportAssetStorePort assetStore,
                                          PortfolioImportProcessingDomainService processingService,
                                          PortfolioTradeVisionGateway visionGateway,
                                          RabbitTemplate rabbitTemplate,
                                          PortfolioImportProcessHistoryRepository historyRepository) {
        this.objectMapper = objectMapper;
        this.taskRepository = taskRepository;
        this.assetStore = assetStore;
        this.processingService = processingService;
        this.visionGateway = visionGateway;
        this.rabbitTemplate = rabbitTemplate;
        this.historyRepository = historyRepository;
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
                PortfolioImportStatus previous = task.getStatus();
                task.claimForProcessing(OffsetDateTime.now());
                taskRepository.update(task);
                append(task, PortfolioImportProcessEventType.PROCESSING_STARTED, previous.name(), task.getStatus().name(), message);
            }
            processingTask = task;
            PortfolioImportAssetBO asset = assetStore.findById(task.getAssetId())
                    .orElseThrow(() -> new IllegalArgumentException("portfolio screenshot asset not found"));
            append(task, PortfolioImportProcessEventType.AI_REQUEST_STARTED, task.getStatus().name(), task.getStatus().name(), message);
            String result = visionGateway.parse(asset);
            append(task, PortfolioImportProcessEventType.AI_REQUEST_SUCCEEDED, task.getStatus().name(), task.getStatus().name(), message);
            processingService.complete(task, result, OffsetDateTime.now());
            append(task, PortfolioImportProcessEventType.CANDIDATES_SAVED, PortfolioImportStatus.PROCESSING.name(),
                    PortfolioImportStatus.READY_FOR_REVIEW.name(), message);
            append(task, PortfolioImportProcessEventType.TASK_READY_FOR_REVIEW, PortfolioImportStatus.PROCESSING.name(),
                    PortfolioImportStatus.READY_FOR_REVIEW.name(), message);
            channel.basicAck(deliveryTag, false);
        } catch (IllegalArgumentException exception) {
            if (processingTask != null && processingTask.getStatus() == PortfolioImportStatus.PROCESSING) {
                processingTask.markFailed("PORTFOLIO_PARSE_INVALID", exception.getMessage(), OffsetDateTime.now());
                taskRepository.update(processingTask);
                append(processingTask, PortfolioImportProcessEventType.AI_REQUEST_FAILED, PortfolioImportStatus.PROCESSING.name(),
                        PortfolioImportStatus.FAILED.name(), message, "PORTFOLIO_PARSE_INVALID", exception.getMessage());
                append(processingTask, PortfolioImportProcessEventType.TASK_FAILED, PortfolioImportStatus.PROCESSING.name(),
                        PortfolioImportStatus.FAILED.name(), message, "PORTFOLIO_PARSE_INVALID", exception.getMessage());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception exception) {
            int retryCount = retryCount(message);
            if (processingTask != null && processingTask.getStatus() == PortfolioImportStatus.PROCESSING) {
                if (retryCount >= 3) {
                    processingTask.markFailed("PORTFOLIO_PARSE_FAILED", exception.getMessage(), OffsetDateTime.now());
                    append(processingTask, PortfolioImportProcessEventType.AI_REQUEST_FAILED, PortfolioImportStatus.PROCESSING.name(),
                            PortfolioImportStatus.FAILED.name(), message, "PORTFOLIO_PARSE_FAILED", exception.getMessage());
                    append(processingTask, PortfolioImportProcessEventType.TASK_FAILED, PortfolioImportStatus.PROCESSING.name(),
                            PortfolioImportStatus.FAILED.name(), message, "PORTFOLIO_PARSE_FAILED", exception.getMessage());
                } else {
                    processingTask.markRetryWaiting("PORTFOLIO_PARSE_RETRY", exception.getMessage());
                    append(processingTask, PortfolioImportProcessEventType.AI_REQUEST_FAILED, PortfolioImportStatus.PROCESSING.name(),
                            PortfolioImportStatus.RETRY_WAITING.name(), message, "PORTFOLIO_PARSE_RETRY", exception.getMessage());
                    append(processingTask, PortfolioImportProcessEventType.RETRY_SCHEDULED, PortfolioImportStatus.PROCESSING.name(),
                            PortfolioImportStatus.RETRY_WAITING.name(), message, "PORTFOLIO_PARSE_RETRY", exception.getMessage());
                }
                taskRepository.update(processingTask);
            }
            dispatchRetry(message, exception);
            channel.basicAck(deliveryTag, false);
        }
    }

    private void dispatchRetry(Message message, Exception exception) {
        int count = retryCount(message);
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

    private static int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get("x-portfolio-retry-count");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
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

    private void append(PortfolioImportTaskBO task, PortfolioImportProcessEventType eventType, String fromStatus,
                        String toStatus, Message message) {
        append(task, eventType, fromStatus, toStatus, message, null, null);
    }

    private void append(PortfolioImportTaskBO task, PortfolioImportProcessEventType eventType, String fromStatus,
                        String toStatus, Message message, String errorCode, String errorMessage) {
        String messageId = message.getMessageProperties().getMessageId();
        historyRepository.append(new PortfolioImportProcessHistoryBO(0, task.getId(), task.getPrincipalKey(), task.getTenantKey(),
                task.getAttemptCount(), eventType, fromStatus, toStatus, messageId, messageId, null, null, errorCode,
                truncate(errorMessage), "{}", OffsetDateTime.now(), null));
    }
}
