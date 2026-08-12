package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.ApiWorkflowEventDTO;
import cn.cccs7.cynexus.market.api.dto.ApiWorkflowProcessDTO;
import cn.cccs7.cynexus.market.api.dto.ApiWorkflowPageDTO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessEventType;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryAggregateBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportStatus;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskPageBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApiWorkflowAppService {
    private static final String PRINCIPAL = "workspace-user";
    private static final String TENANT = "default";
    private final PortfolioImportTaskRepository taskRepository;
    private final PortfolioImportProcessHistoryRepository historyRepository;

    public ApiWorkflowAppService(PortfolioImportTaskRepository taskRepository,
                                 PortfolioImportProcessHistoryRepository historyRepository) {
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
    }

    public ApiWorkflowPageDTO list(int page, int pageSize) {
        PortfolioImportTaskPageBO result = taskRepository.findPageByOwner(PRINCIPAL, TENANT, page, pageSize);
        List<ApiWorkflowProcessDTO> items = result.items().stream()
                .map(this::toDto)
                .toList();
        return ApiWorkflowPageDTO.builder().items(items).total(result.total()).page(result.page()).pageSize(result.pageSize())
                .totalPages(result.totalPages()).hasPrevious(result.hasPrevious()).hasNext(result.hasNext()).build();
    }

    private ApiWorkflowProcessDTO toDto(PortfolioImportTaskBO task) {
        PortfolioImportProcessHistoryAggregateBO history = historyRepository.findByTaskId(PRINCIPAL, TENANT, task.getId()).orElse(null);
        List<PortfolioImportProcessHistoryBO> rawEvents = history == null ? List.of() : history.events();
        List<ApiWorkflowEventDTO> events = rawEvents.stream().map(this::event).toList();
        PortfolioImportProcessHistoryBO last = rawEvents.stream().max(Comparator.comparing(PortfolioImportProcessHistoryBO::occurredAt)).orElse(null);
        String status = mapStatus(task.getStatus());
        String phase = last == null ? "queued" : phase(last.eventType());
        int progress = progress(task.getStatus(), last == null ? null : last.eventType());
        OffsetDateTime completedAt = task.getConfirmedAt() != null ? task.getConfirmedAt() : task.getCompletedAt();
        return ApiWorkflowProcessDTO.builder().id("portfolio-trade-" + task.getId()).workflowType("PORTFOLIO_TRADE_IMPORT")
                .workflowKey("portfolio.trade.import.screenshot").source("api").sourceRef(String.valueOf(task.getId()))
                .title("AI Trade 识别").status(status).phase(phase).progress(progress).attemptCount(task.getAttemptCount())
                .errorCode(task.getLastErrorCode()).errorMessage(task.getLastErrorMessage()).resultRoute("/personal-portfolio/imports/" + task.getId() + "/review")
                .resultRef(String.valueOf(task.getId())).createdAt(task.getCreatedAt()).startedAt(task.getStartedAt()).completedAt(completedAt)
                .updatedAt(completedAt == null ? task.getStartedAt() : completedAt).events(events).build();
    }

    private ApiWorkflowEventDTO event(PortfolioImportProcessHistoryBO item) {
        String status = mapStatus(item.toStatus());
        return ApiWorkflowEventDTO.builder().attemptNo(item.attemptNo()).eventType(mapEvent(item.eventType()))
                .fromStatus(item.fromStatus()).toStatus(item.toStatus()).phase(phase(item.eventType())).status(status)
                .progress(progress(null, item.eventType())).message(message(item.eventType())).providerCode(item.providerCode())
                .modelCode(item.modelCode()).errorCode(item.errorCode()).errorMessage(item.errorMessage())
                .details(item.details()).durationMs(item.durationMs()).occurredAt(item.occurredAt()).build();
    }

    private static String mapStatus(PortfolioImportStatus status) { return mapStatus(status == null ? null : status.name()); }
    private static String mapStatus(String status) {
        if (status == null) return "QUEUED";
        return switch (status.toUpperCase()) {
            case "PENDING_DISPATCH", "QUEUED", "RETRY_WAITING" -> "QUEUED";
            case "PROCESSING" -> "RUNNING";
            case "READY_FOR_REVIEW" -> "WAITING_REVIEW";
            case "CONFIRMED" -> "SUCCEEDED";
            case "CANCELLED" -> "CANCELLED";
            default -> "FAILED";
        };
    }

    private static String mapEvent(PortfolioImportProcessEventType type) {
        return switch (type) {
            case TASK_CREATED -> "QUEUED";
            case OUTBOX_CREATED -> "DISPATCHED";
            case PROCESSING_STARTED -> "PROCESSING_STARTED";
            case AI_REQUEST_STARTED -> "AI_STARTED";
            case AI_REQUEST_SUCCEEDED -> "AI_SUCCEEDED";
            case AI_REQUEST_FAILED -> "AI_FAILED";
            case RETRY_SCHEDULED, MANUAL_RETRY_REQUESTED -> "RETRY_SCHEDULED";
            case CANDIDATES_SAVED -> "RESULT_READY";
            case TASK_READY_FOR_REVIEW -> "WAITING_REVIEW";
            case TASK_CONFIRMED -> "CONFIRMED";
            case TASK_FAILED -> "FAILED";
            case CANDIDATE_EDITED -> "STEP_UPDATED";
        };
    }

    private static String phase(PortfolioImportProcessEventType type) {
        return switch (type) {
            case TASK_CREATED, OUTBOX_CREATED -> "queued";
            case PROCESSING_STARTED -> "processing";
            case AI_REQUEST_STARTED, AI_REQUEST_SUCCEEDED, AI_REQUEST_FAILED -> "ai_processing";
            case RETRY_SCHEDULED, MANUAL_RETRY_REQUESTED -> "retrying";
            case CANDIDATES_SAVED -> "result_ready";
            case TASK_READY_FOR_REVIEW, CANDIDATE_EDITED -> "waiting_review";
            case TASK_CONFIRMED -> "completed";
            case TASK_FAILED -> "failed";
        };
    }

    private static int progress(PortfolioImportStatus status, PortfolioImportProcessEventType event) {
        if (status != null) return switch (status) {
            case PENDING_DISPATCH, QUEUED, RETRY_WAITING -> 0;
            case PROCESSING -> 50;
            case READY_FOR_REVIEW -> 90;
            case CONFIRMED -> 100;
            case FAILED, CANCELLED -> 0;
        };
        if (event == null) return 0;
        return switch (event) {
            case TASK_CREATED, OUTBOX_CREATED, RETRY_SCHEDULED, MANUAL_RETRY_REQUESTED -> 0;
            case PROCESSING_STARTED -> 25;
            case AI_REQUEST_STARTED -> 50;
            case AI_REQUEST_SUCCEEDED, CANDIDATES_SAVED -> 80;
            case TASK_READY_FOR_REVIEW, CANDIDATE_EDITED -> 90;
            case TASK_CONFIRMED -> 100;
            case AI_REQUEST_FAILED, TASK_FAILED -> 0;
        };
    }

    private static String message(PortfolioImportProcessEventType type) { return phase(type); }
}
