package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.PortfolioImportTaskDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportCandidateDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportReviewDTO;
import cn.cccs7.cynexus.market.api.request.PortfolioImportConfirmRequest;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioCandidateStore;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportConfirmationDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskBO;
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
    private final PortfolioCandidateStore candidateStore;
    private final PortfolioImportConfirmationDomainService confirmationService;

    public PortfolioImportAppService(PortfolioImportDomainService domainService, PortfolioCandidateStore candidateStore,
                                    PortfolioImportConfirmationDomainService confirmationService) {
        this.domainService = domainService;
        this.candidateStore = candidateStore;
        this.confirmationService = confirmationService;
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

    public PortfolioImportReviewDTO review(long taskId) {
        PortfolioImportTaskBO task = domainService.get(DEFAULT_PRINCIPAL, DEFAULT_TENANT, taskId);
        List<PortfolioImportCandidateDTO> candidates = candidateStore.listByTaskId(taskId).stream()
                .map(candidate -> PortfolioImportCandidateDTO.builder()
                        .id(candidate.id()).rowNo(candidate.rowNo()).normalizedPayload(candidate.normalizedPayload().toString())
                        .reviewStatus(candidate.reviewStatus()).version(candidate.version()).build())
                .toList();
        return PortfolioImportReviewDTO.builder().task(toDto(task)).candidates(candidates).build();
    }

    @Transactional
    public PortfolioImportTaskDTO confirm(long taskId, PortfolioImportConfirmRequest request) {
        PortfolioImportTaskBO task = domainService.get(DEFAULT_PRINCIPAL, DEFAULT_TENANT, taskId);
        if (request != null && request.getVersion() != null && request.getVersion() != task.getVersion()) {
            throw new IllegalStateException("portfolio import version is stale");
        }
        return toDto(confirmationService.confirm(task,
                request == null ? null : request.getCandidateIds(), DEFAULT_PRINCIPAL, OffsetDateTime.now()));
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
