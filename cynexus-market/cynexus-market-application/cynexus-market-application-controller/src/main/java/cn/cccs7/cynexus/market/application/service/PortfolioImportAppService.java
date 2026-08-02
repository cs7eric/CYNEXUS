package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.PortfolioImportTaskDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportCandidateDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportReviewDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportProcessHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportProcessHistoryEventDTO;
import cn.cccs7.cynexus.market.api.request.PortfolioCandidateEditRequest;
import cn.cccs7.cynexus.market.api.request.PortfolioImportConfirmRequest;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioCandidateStore;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportConfirmationDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportStatus;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioCandidateRecord;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioCandidateEditPolicy;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessEventType;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryAggregateBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryRepository;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PortfolioImportAppService {
    private static final String DEFAULT_PRINCIPAL = "workspace-user";
    private static final String DEFAULT_TENANT = "default";
    private final PortfolioImportDomainService domainService;
    private final PortfolioImportTaskRepository taskRepository;
    private final PortfolioCandidateStore candidateStore;
    private final PortfolioImportConfirmationDomainService confirmationService;
    private final PortfolioImportProcessHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public PortfolioImportAppService(PortfolioImportDomainService domainService, PortfolioImportTaskRepository taskRepository,
                                    PortfolioCandidateStore candidateStore,
                                    PortfolioImportConfirmationDomainService confirmationService,
                                    PortfolioImportProcessHistoryRepository historyRepository,
                                    ObjectMapper objectMapper) {
        this.domainService = domainService;
        this.taskRepository = taskRepository;
        this.candidateStore = candidateStore;
        this.confirmationService = confirmationService;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PortfolioImportTaskDTO upload(MultipartFile file, String idempotencyKey) {
        if (file == null) {
            throw new IllegalArgumentException("portfolio screenshot is required");
        }
        try {
            String key = idempotencyKey == null || idempotencyKey.isBlank()
                    ? "upload-" + UUID.randomUUID()
                    : idempotencyKey;
            PortfolioImportTaskBO task = domainService.create(DEFAULT_PRINCIPAL, DEFAULT_TENANT,
                    file.getOriginalFilename(), file.getContentType(), file.getBytes(), key, OffsetDateTime.now());
            if (historyRepository.findByTaskId(DEFAULT_PRINCIPAL, DEFAULT_TENANT, task.getId()).isEmpty()) {
                append(task, PortfolioImportProcessEventType.TASK_CREATED, null, task.getStatus().name(), null, null, null,
                        null, null, null, "{}", null);
                append(task, PortfolioImportProcessEventType.OUTBOX_CREATED, task.getStatus().name(), task.getStatus().name(),
                        null, null, null, null, null, null, "{}", null);
            }
            return toDto(task);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("failed to read portfolio screenshot", exception);
        }
    }

    public PortfolioImportTaskDTO get(long taskId) {
        return toDto(domainService.get(DEFAULT_PRINCIPAL, DEFAULT_TENANT, taskId));
    }

    public List<PortfolioImportTaskDTO> list(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        return taskRepository.findRecentByOwner(DEFAULT_PRINCIPAL, DEFAULT_TENANT, boundedLimit)
                .stream().map(this::toDto).toList();
    }

    public PortfolioImportReviewDTO review(long taskId) {
        PortfolioImportTaskBO task = domainService.get(DEFAULT_PRINCIPAL, DEFAULT_TENANT, taskId);
        List<PortfolioImportCandidateDTO> candidates = candidateStore.listByTaskId(taskId).stream()
                .map(candidate -> PortfolioImportCandidateDTO.builder()
                        .id(candidate.id()).rowNo(candidate.rowNo()).normalizedPayload(candidate.normalizedPayload().toString())
                        .reviewStatus(candidate.reviewStatus()).version(candidate.version()).confidence(candidate.confidence())
                        .warnings(candidate.warnings()).build())
                .toList();
        return PortfolioImportReviewDTO.builder().task(toDto(task)).candidates(candidates).build();
    }

    public PortfolioImportProcessHistoryDTO history(long taskId) {
        domainService.get(DEFAULT_PRINCIPAL, DEFAULT_TENANT, taskId);
        return historyRepository.findByTaskId(DEFAULT_PRINCIPAL, DEFAULT_TENANT, taskId)
                .map(this::toHistoryDto)
                .orElseGet(() -> PortfolioImportProcessHistoryDTO.builder().taskId(taskId).events(List.of()).build());
    }

    private PortfolioImportProcessHistoryDTO toHistoryDto(PortfolioImportProcessHistoryAggregateBO history) {
        return PortfolioImportProcessHistoryDTO.builder().id(history.id()).taskId(history.taskId())
                .createdAt(history.createdAt()).updatedAt(history.updatedAt())
                .events(history.events().stream().map(this::toHistoryEventDto).toList()).build();
    }

    private PortfolioImportProcessHistoryEventDTO toHistoryEventDto(PortfolioImportProcessHistoryBO item) {
        return PortfolioImportProcessHistoryEventDTO.builder().id(item.id()).attemptNo(item.attemptNo())
                .eventType(item.eventType().name()).fromStatus(item.fromStatus()).toStatus(item.toStatus())
                .messageId(item.messageId()).correlationId(item.correlationId()).providerCode(item.providerCode())
                .modelCode(item.modelCode()).errorCode(item.errorCode()).errorMessage(item.errorMessage())
                .details(item.details()).occurredAt(item.occurredAt()).durationMs(item.durationMs()).build();
    }

    @Transactional
    public PortfolioImportCandidateDTO editCandidate(long taskId, long candidateId, PortfolioCandidateEditRequest request) {
        PortfolioImportTaskBO task = domainService.get(DEFAULT_PRINCIPAL, DEFAULT_TENANT, taskId);
        if (task.getStatus() != cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportStatus.READY_FOR_REVIEW) {
            throw new IllegalStateException("portfolio candidates can only be edited while ready for review");
        }
        if (request == null || request.getVersion() == null) throw new IllegalArgumentException("candidate version is required");
        PortfolioCandidateRecord current = candidateStore.listByTaskId(taskId).stream()
                .filter(candidate -> candidate.id() == candidateId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("portfolio candidate not found"));
        if (current.version() != request.getVersion()) throw new IllegalStateException("portfolio candidate version is stale");
        var normalized = PortfolioCandidateEditPolicy.normalize(objectMapper, request.getSymbol(), request.getSecurityName(),
                request.getSide(), request.getTradedAt(), request.getQuantity(), request.getPrice(), request.getTotalFee(),
                request.getExternalTradeNo());
        try {
            String payload = objectMapper.writeValueAsString(normalized);
            if (!candidateStore.updateNormalized(taskId, candidateId, current.version(), payload)) {
                throw new IllegalStateException("portfolio candidate was changed concurrently: " + candidateId);
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("unable to serialize portfolio candidate", exception);
        }
        append(task, PortfolioImportProcessEventType.CANDIDATE_EDITED, task.getStatus().name(), task.getStatus().name(),
                null, null, null, null, null, null, "{\"candidateId\":" + candidateId + "}", null);
        return candidateStore.listByTaskId(taskId).stream().filter(candidate -> candidate.id() == candidateId)
                .findFirst().map(this::toCandidateDto).orElseThrow();
    }

    @Transactional
    public PortfolioImportTaskDTO confirm(long taskId, PortfolioImportConfirmRequest request) {
        PortfolioImportTaskBO task = domainService.get(DEFAULT_PRINCIPAL, DEFAULT_TENANT, taskId);
        PortfolioImportStatus previousStatus = task.getStatus();
        if (request != null && request.getVersion() != null && request.getVersion() != task.getVersion()) {
            throw new IllegalStateException("portfolio import version is stale");
        }
        PortfolioImportTaskBO confirmed = confirmationService.confirm(task,
                request == null ? null : request.getCandidateIds(), DEFAULT_PRINCIPAL, OffsetDateTime.now());
        if (previousStatus == PortfolioImportStatus.READY_FOR_REVIEW) {
            append(confirmed, PortfolioImportProcessEventType.TASK_CONFIRMED, previousStatus.name(),
                    confirmed.getStatus().name(), null, null, null, null, null, null, "{}", null);
        }
        return toDto(confirmed);
    }

    private PortfolioImportCandidateDTO toCandidateDto(PortfolioCandidateRecord candidate) {
        return PortfolioImportCandidateDTO.builder().id(candidate.id()).rowNo(candidate.rowNo())
                .normalizedPayload(candidate.normalizedPayload().toString()).reviewStatus(candidate.reviewStatus())
                .version(candidate.version()).confidence(candidate.confidence()).warnings(candidate.warnings()).build();
    }

    private void append(PortfolioImportTaskBO task, PortfolioImportProcessEventType eventType, String fromStatus,
                       String toStatus, String messageId, String correlationId, String providerCode, String modelCode,
                       String errorCode, String errorMessage, String details, Long durationMs) {
        historyRepository.append(new PortfolioImportProcessHistoryBO(0, task.getId(), task.getPrincipalKey(), task.getTenantKey(),
                task.getAttemptCount(), eventType, fromStatus, toStatus, messageId, correlationId, providerCode, modelCode,
                errorCode, errorMessage, details == null ? "{}" : details, OffsetDateTime.now(), durationMs));
    }

    private PortfolioImportTaskDTO toDto(PortfolioImportTaskBO task) {
        return PortfolioImportTaskDTO.builder()
                .id(task.getId())
                .status(task.getStatus().name())
                .version(task.getVersion())
                .attemptCount(task.getAttemptCount())
                .assetId(task.getAssetId())
                .queuedAt(task.getQueuedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .confirmedAt(task.getConfirmedAt())
                .lastErrorCode(task.getLastErrorCode())
                .lastErrorMessage(task.getLastErrorMessage())
                .build();
    }
}
