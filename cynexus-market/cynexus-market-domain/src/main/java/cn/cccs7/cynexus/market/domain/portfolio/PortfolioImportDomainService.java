package cn.cccs7.cynexus.market.domain.portfolio;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

public final class PortfolioImportDomainService {
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final int RETENTION_DAYS = 30;
    private final PortfolioIdGenerator idGenerator;
    private final PortfolioImportAssetStorePort assetStore;
    private final PortfolioImportTaskRepository taskRepository;
    private final PortfolioImportDispatchPort dispatchPort;

    public PortfolioImportDomainService(PortfolioIdGenerator idGenerator,
                                        PortfolioImportAssetStorePort assetStore,
                                        PortfolioImportTaskRepository taskRepository,
                                        PortfolioImportDispatchPort dispatchPort) {
        this.idGenerator = idGenerator;
        this.assetStore = assetStore;
        this.taskRepository = taskRepository;
        this.dispatchPort = dispatchPort;
    }

    public PortfolioImportTaskBO create(String principalKey, String tenantKey, String fileName,
                                        String contentType, byte[] content, String idempotencyKey,
                                        OffsetDateTime now) {
        requireText(principalKey, "principalKey");
        requireText(tenantKey, "tenantKey");
        requireText(idempotencyKey, "idempotencyKey");
        validateContent(contentType, content);
        Optional<PortfolioImportTaskBO> existing = taskRepository.findByIdempotencyKey(principalKey, tenantKey, idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        byte[] safeContent = content.clone();
        long assetId = idGenerator.nextId();
        PortfolioImportAssetBO asset = new PortfolioImportAssetBO(assetId, principalKey, tenantKey,
                fileName, contentType, safeContent.length, sha256(safeContent), safeContent,
                now.plusDays(RETENTION_DAYS));
        assetStore.save(asset);

        long taskId = idGenerator.nextId();
        PortfolioImportTaskBO task = new PortfolioImportTaskBO(taskId, principalKey, tenantKey, assetId,
                idempotencyKey, PortfolioImportStatus.PENDING_DISPATCH, 0, 0);
        taskRepository.save(task);
        dispatchPort.dispatch(taskId, principalKey, tenantKey);
        return task;
    }

    public PortfolioImportTaskBO get(String principalKey, String tenantKey, long taskId) {
        return taskRepository.findByOwnerAndId(principalKey, tenantKey, taskId)
                .orElseThrow(() -> new IllegalArgumentException("portfolio import task not found"));
    }

    private static void validateContent(String contentType, byte[] content) {
        if (content == null || content.length == 0 || content.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("portfolio screenshot must be between 1 byte and 10 MiB");
        }
        if (!"image/png".equalsIgnoreCase(contentType) && !"image/jpeg".equalsIgnoreCase(contentType)
                && !"image/webp".equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("portfolio screenshot must be PNG, JPEG or WebP");
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
