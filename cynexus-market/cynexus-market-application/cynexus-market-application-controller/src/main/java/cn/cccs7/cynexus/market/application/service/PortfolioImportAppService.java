package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.PortfolioImportTaskDTO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskBO;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PortfolioImportAppService {
    private static final String DEFAULT_PRINCIPAL = "workspace-user";
    private static final String DEFAULT_TENANT = "default";
    private final PortfolioImportDomainService domainService;

    public PortfolioImportAppService(PortfolioImportDomainService domainService) {
        this.domainService = domainService;
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
