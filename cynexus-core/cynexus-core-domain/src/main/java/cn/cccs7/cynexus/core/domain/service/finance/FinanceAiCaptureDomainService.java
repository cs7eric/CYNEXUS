package cn.cccs7.cynexus.core.domain.service.finance;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.common.constant.CoreConstants;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureHistoryPageBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureItemUpdate;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCategoryBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceTransactionBO;
import cn.cccs7.cynexus.core.domain.finance.port.FinanceRepository;
import cn.cccs7.cynexus.core.domain.finance.port.ReceiptAiParserPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HexFormat;
import java.util.UUID;

/** Coordinates AI receipt capture, matching, and posting into the ledger domain. */
public class FinanceAiCaptureDomainService {
    public static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private final FinanceRepository repository;
    private final ReceiptAiParserPort parser;
    private final FinanceLedgerDomainService ledgerService;
    private final FinanceInstallmentDomainService installmentService;

    public FinanceAiCaptureDomainService(FinanceRepository repository,
                                         ReceiptAiParserPort parser,
                                         FinanceLedgerDomainService ledgerService,
                                         FinanceInstallmentDomainService installmentService) {
        this.repository = repository;
        this.parser = parser;
        this.ledgerService = ledgerService;
        this.installmentService = installmentService;
    }

    public FinanceAiCaptureBO parse(String ownerKey, String fileName, String contentType, byte[] content,
                                    Long fileSize, String mode, String idempotencyKey) {
        AcceptedCapture accepted = accept(ownerKey, fileName, contentType, content, fileSize, mode, idempotencyKey);
        if (!CoreConstants.STATUS_QUEUED.equalsIgnoreCase(accepted.capture().getStatus())) {
            return accepted.capture();
        }
        return processCapture(ownerKey, accepted.capture().getId(), accepted.images(), accepted.imageIds(),
                accepted.capture().getProcessingMode());
    }

    public FinanceAiCaptureBO enqueue(String ownerKey, String fileName, String contentType, byte[] content,
                                      Long fileSize, String mode, String idempotencyKey) {
        return accept(ownerKey, fileName, contentType, content, fileSize, mode, idempotencyKey).capture();
    }

    public FinanceAiCaptureBO claimNext() {
        return repository.claimNextCapture();
    }

    public void execute(FinanceAiCaptureBO capture) {
        if (capture == null || capture.getId() == null) {
            return;
        }
        List<ReceiptAiParserPort.ReceiptImage> images = repository.listCaptureImages(capture.getOwnerKey(), capture.getId());
        if (images.isEmpty()) {
            repository.updateCapture(FinanceAiCaptureBO.builder().id(capture.getId()).ownerKey(capture.getOwnerKey())
                    .status(CoreConstants.STATUS_FAILED).processingMode(capture.getProcessingMode())
                    .sourceCount(0).errorMessage("\u539f\u59cb\u56fe\u7247\u672a\u4fdd\u5b58\uff0c\u65e0\u6cd5\u8bc6\u522b")
                    .rawResponse("{}").build());
            return;
        }
        String processingMode = capture.getProcessingMode() == null ? "auto" : capture.getProcessingMode();
        processCapture(capture.getOwnerKey(), capture.getId(), images, Map.of(), processingMode);
    }

    private AcceptedCapture accept(String ownerKey, String fileName, String contentType, byte[] content,
                                   Long fileSize, String mode, String idempotencyKey) {
        String processingMode = CoreConstants.STATUS_REVIEW.equalsIgnoreCase(mode)
                ? CoreConstants.STATUS_REVIEW : "auto";
        String validationError = null;
        if (content == null || content.length == 0 || fileSize == null || fileSize <= 0) {
            validationError = "\u5fc5\u987b\u4e0a\u4f20\u4e00\u5f20\u56fe\u7247";
        } else if (fileSize > MAX_IMAGE_BYTES || content.length > MAX_IMAGE_BYTES) {
            validationError = "\u56fe\u7247\u4e0d\u80fd\u8d85\u8fc7 10MB";
        } else if (contentType == null || !contentType.startsWith("image/")) {
            validationError = "\u53ea\u652f\u6301\u56fe\u7247\u6587\u4ef6";
        }
        String suppliedKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : normalizeKey(idempotencyKey);
        String requestKey = suppliedKey != null
                ? suppliedKey
                : content != null && content.length > 0
                ? sha256((processingMode + ":" + sha256(content)).getBytes(StandardCharsets.UTF_8))
                : "invalid-" + UUID.randomUUID();
        FinanceAiCaptureBO existing = repository.findCaptureByIdempotencyKey(ownerKey, requestKey);
        if (existing != null) {
            return new AcceptedCapture(existing, List.of(), Map.of());
        }
        int sourceCount = content == null || content.length == 0 ? 0 : 1;
        Long captureId = repository.createCapture(FinanceAiCaptureBO.builder().ownerKey(ownerKey).status(CoreConstants.STATUS_QUEUED)
                .processingMode(processingMode).sourceCount(sourceCount).idempotencyKey(requestKey).build());
        if (validationError != null) {
            repository.updateCapture(FinanceAiCaptureBO.builder().id(captureId).ownerKey(ownerKey).status(CoreConstants.STATUS_FAILED)
                    .sourceCount(sourceCount).errorMessage(validationError).rawResponse("{}").build());
            return new AcceptedCapture(repository.findCapture(ownerKey, captureId), List.of(), Map.of());
        }
        ReceiptAiParserPort.ReceiptImage receiptImage = new ReceiptAiParserPort.ReceiptImage(0, fileName, contentType, content);
        List<ReceiptAiParserPort.ReceiptImage> images = List.of(receiptImage);
        Map<Integer, Long> imageIds = new HashMap<>();
        for (ReceiptAiParserPort.ReceiptImage image : images) {
            imageIds.put(image.imageIndex(), repository.createCaptureImage(captureId, image.imageIndex(), image.fileName(),
                    image.contentType(), image.content().length, sha256(image.content()), image.content()));
        }
        return new AcceptedCapture(repository.findCapture(ownerKey, captureId), images, imageIds);
    }

    private record AcceptedCapture(FinanceAiCaptureBO capture,
                                   List<ReceiptAiParserPort.ReceiptImage> images,
                                   Map<Integer, Long> imageIds) {
    }

    public FinanceAiCaptureBO get(String ownerKey, Long id) {
        FinanceAiCaptureBO capture = repository.findCapture(ownerKey, id);
        if (capture == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "AI \u8bc6\u522b\u6279\u6b21\u4e0d\u5b58\u5728");
        }
        return capture;
    }

    public ReceiptAiParserPort.ReceiptImage image(String ownerKey, Long captureId, int imageIndex) {
        get(ownerKey, captureId);
        return repository.listCaptureImages(ownerKey, captureId).stream()
                .filter(image -> image.imageIndex() == imageIndex)
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "AI capture image not found"));
    }

    public FinanceAiCaptureHistoryPageBO history(String ownerKey, int limit, int offset, String status) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        int normalizedOffset = Math.max(0, offset);
        String normalizedStatus = status == null || status.isBlank() ? "all" : status.trim().toLowerCase();
        return FinanceAiCaptureHistoryPageBO.builder()
                .items(repository.listCaptures(ownerKey, normalizedLimit, normalizedOffset, normalizedStatus))
                .total(repository.countCaptures(ownerKey, normalizedStatus))
                .limit(normalizedLimit)
                .offset(normalizedOffset)
                .status(normalizedStatus)
                .build();
    }

    public FinanceAiCaptureBO confirm(String ownerKey, Long id, List<FinanceAiCaptureItemUpdate> updates) {
        FinanceAiCaptureBO capture = repository.findCapture(ownerKey, id);
        if (capture == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "AI \u8bc6\u522b\u6279\u6b21\u4e0d\u5b58\u5728");
        }
        if (!List.of(CoreConstants.STATUS_REVIEW, CoreConstants.STATUS_FAILED, CoreConstants.STATUS_CONFIRMED).contains(capture.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "\u5f53\u524d\u6279\u6b21\u4e0d\u53ef\u786e\u8ba4");
        }
        if (capture.getItems() == null || capture.getItems().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "\u5f53\u524d\u6279\u6b21\u6ca1\u6709\u53ef\u5165\u8d26\u7684\u8bc6\u522b\u7ed3\u679c");
        }
        Map<Long, FinanceAiCaptureItemUpdate> changes = new HashMap<>();
        if (updates != null) {
            updates.forEach(item -> changes.put(item.getId(), item));
        }
        for (FinanceAiCaptureBO.Item item : capture.getItems()) {
            FinanceAiCaptureItemUpdate change = changes.get(item.getId());
            boolean changed = change != null && hasChanges(item, change);
            if (change != null) {
                apply(item, change);
            }
            if (item.getTransactionId() != null) {
                if (CoreConstants.STATUS_REJECTED.equals(item.getStatus())) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "\u5df2\u5165\u8d26\u8d26\u76ee\u4e0d\u80fd\u6807\u8bb0\u4e3a\u62d2\u7edd\uff0c\u8bf7\u76f4\u63a5\u5220\u9664\u8d26\u76ee");
                }
                if (changed) {
                    ledgerService.saveTransaction(ownerKey, toTransaction(item), "ai_capture", "capture-" + id + "-" + item.getId());
                }
                item.setStatus(CoreConstants.STATUS_CONFIRMED);
                repository.updateCaptureItem(item);
                continue;
            }
            if (CoreConstants.STATUS_REJECTED.equals(item.getStatus())) {
                repository.updateCaptureItem(item);
                continue;
            }
            if (item.getAmount() == null || item.getAccountId() == null || item.getCategoryId() == null || item.getOccurredAt() == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "\u8bf7\u8865\u5168\u6bcf\u6761\u8349\u7a3f\u7684\u8d26\u6237\u3001\u5206\u7c7b\u3001\u65e5\u671f\u548c\u91d1\u989d");
            }
            FinanceTransactionBO saved = ledgerService.saveTransaction(ownerKey, toTransaction(item), "ai_capture",
                    "capture-" + id + "-" + item.getId());
            Long transactionId = saved.getId();
            item.setStatus(CoreConstants.STATUS_CONFIRMED);
            item.setTransactionId(transactionId);
            repository.updateCaptureItem(item);
            installmentService.createForPurchase(ownerKey, item.getAccountId(), transactionId, item.getAmount(),
                    item.getInstallmentFee(), item.getInstallmentCount(), item.getOccurredAt(), item.getMerchant());
        }
        capture.setStatus(capture.getItems().stream()
                .allMatch(item -> CoreConstants.STATUS_CONFIRMED.equals(item.getStatus())
                        || CoreConstants.STATUS_REJECTED.equals(item.getStatus()))
                ? CoreConstants.STATUS_CONFIRMED : CoreConstants.STATUS_REVIEW);
        repository.updateCapture(capture);
        return repository.findCapture(ownerKey, id);
    }

    public FinanceAiCaptureBO retry(String ownerKey, Long id) {
        FinanceAiCaptureBO capture = repository.findCapture(ownerKey, id);
        if (capture == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "AI \u8bc6\u522b\u6279\u6b21\u4e0d\u5b58\u5728");
        }
        if (!CoreConstants.STATUS_FAILED.equals(capture.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "\u53ea\u6709\u5931\u8d25\u6279\u6b21\u53ef\u4ee5\u91cd\u65b0\u8bc6\u522b");
        }
        if (capture.getItems() != null && capture.getItems().stream().anyMatch(item -> item.getTransactionId() != null)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "\u8be5\u6279\u6b21\u5df2\u6709\u8d26\u76ee\u5165\u8d26\uff0c\u8bf7\u5148\u5904\u7406\u5173\u8054\u8d26\u76ee");
        }
        List<ReceiptAiParserPort.ReceiptImage> images = repository.listCaptureImages(ownerKey, id);
        if (images.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "\u539f\u59cb\u56fe\u7247\u672a\u4fdd\u5b58\uff0c\u65e0\u6cd5\u91cd\u65b0\u8bc6\u522b");
        }
        repository.deleteCaptureItems(ownerKey, id);
        String processingMode = capture.getProcessingMode() == null ? CoreConstants.STATUS_REVIEW : capture.getProcessingMode();
        repository.updateCapture(FinanceAiCaptureBO.builder().id(id).ownerKey(ownerKey).status(CoreConstants.STATUS_PROCESSING)
                .processingMode(processingMode).sourceCount(images.size()).errorMessage(null).rawResponse("{}").build());
        return processCapture(ownerKey, id, images, Map.of(), processingMode);
    }

    public boolean archive(String ownerKey, Long id) {
        return repository.archiveCapture(ownerKey, id);
    }

    private FinanceAiCaptureBO processCapture(String ownerKey, Long captureId, List<ReceiptAiParserPort.ReceiptImage> images,
                                              Map<Integer, Long> imageIds, String processingMode) {
        try {
            List<FinanceAccountBO> accounts = repository.listAccounts(ownerKey, false);
            List<FinanceCategoryBO> categories = repository.listCategories(ownerKey, false);
            ReceiptAiParserPort.ParseResult result = parser.parse(ownerKey, images,
                    accounts.stream().map(item -> new ReceiptAiParserPort.AccountCandidate(item.getId(), item.getName(),
                            item.getAccountType(), item.getNature(), item.getNotes())).toList(),
                    categories.stream().map(item -> new ReceiptAiParserPort.CategoryCandidate(item.getId(), item.getName(),
                            item.getDirection())).toList());
            List<ReceiptAiParserPort.ParsedItem> parsedItems = result.items() == null ? List.of() : result.items();
            for (int index = 0; index < parsedItems.size(); index++) {
                ReceiptAiParserPort.ParsedItem parsed = parsedItems.get(index);
                FinanceCategoryBO category = findCategory(categories, parsed.categoryId(), parsed.categoryHint(), parsed.transactionType());
                FinanceAccountBO account = findAccount(accounts, parsed.accountId(), parsed.accountHint(), parsed.rawText());
                repository.createCaptureItem(FinanceAiCaptureBO.Item.builder().captureId(captureId)
                        .imageId(imageIds.get(parsed.imageIndex()))
                        .itemIndex(index)
                        .transactionType(normalizeType(parsed.transactionType()))
                        .occurredAt(parsed.occurredAt() == null && "auto".equals(processingMode) ? OffsetDateTime.now() : parsed.occurredAt())
                        .amount(parsed.amount())
                        .currency(parsed.currency())
                        .merchant(parsed.merchant())
                        .categoryHint(parsed.categoryHint())
                        .categoryId(category == null ? null : category.getId())
                        .accountId(account == null ? null : account.getId())
                        .installmentCount(parsed.installmentCount())
                        .installmentFee(parsed.installmentFee())
                        .note(parsed.note())
                        .confidence(parsed.confidence())
                        .rawText(parsed.rawText())
                        .status(CoreConstants.STATUS_DRAFT)
                        .build());
            }
            repository.updateCapture(FinanceAiCaptureBO.builder().id(captureId).ownerKey(ownerKey).status(CoreConstants.STATUS_REVIEW)
                    .processingMode(processingMode).sourceCount(images.size()).modelCode(result.modelCode())
                    .rawResponse(result.rawResponse()).errorMessage(null).build());
            if ("auto".equals(processingMode) && !parsedItems.isEmpty()) {
                autoPost(ownerKey, captureId);
            }
        } catch (RuntimeException exception) {
            repository.updateCapture(FinanceAiCaptureBO.builder().id(captureId).ownerKey(ownerKey).status(CoreConstants.STATUS_FAILED)
                    .processingMode(processingMode).sourceCount(images.size()).errorMessage(exception.getMessage())
                    .rawResponse("{}").build());
        }
        return repository.findCapture(ownerKey, captureId);
    }

    private void autoPost(String ownerKey, Long captureId) {
        FinanceAiCaptureBO capture = repository.findCapture(ownerKey, captureId);
        for (FinanceAiCaptureBO.Item item : capture.getItems()) {
            if (item.getAmount() == null || item.getAccountId() == null || item.getCategoryId() == null || item.getOccurredAt() == null) {
                continue;
            }
            FinanceTransactionBO saved = ledgerService.saveTransaction(ownerKey, toTransaction(item), "ai_capture",
                    "capture-" + captureId + "-" + item.getId());
            Long transactionId = saved.getId();
            item.setStatus(CoreConstants.STATUS_CONFIRMED);
            item.setTransactionId(transactionId);
            repository.updateCaptureItem(item);
            installmentService.createForPurchase(ownerKey, item.getAccountId(), transactionId, item.getAmount(),
                    item.getInstallmentFee(), item.getInstallmentCount(), item.getOccurredAt(), item.getMerchant());
        }
        capture.setStatus(capture.getItems().stream().allMatch(item -> CoreConstants.STATUS_CONFIRMED.equals(item.getStatus()))
                ? CoreConstants.STATUS_CONFIRMED : CoreConstants.STATUS_REVIEW);
        repository.updateCapture(capture);
    }

    private FinanceTransactionBO toTransaction(FinanceAiCaptureBO.Item item) {
        return FinanceTransactionBO.builder()
                .id(item.getTransactionId())
                .transactionType(item.getTransactionType())
                .accountId(item.getAccountId())
                .categoryId(item.getCategoryId())
                .amount(item.getAmount())
                .currency(item.getCurrency())
                .occurredAt(item.getOccurredAt())
                .merchant(item.getMerchant())
                .note(item.getNote())
                .build();
    }

    private void apply(FinanceAiCaptureBO.Item target, FinanceAiCaptureItemUpdate source) {
        if (source.getStatus() != null) target.setStatus(source.getStatus());
        if (source.getTransactionType() != null) target.setTransactionType(source.getTransactionType());
        if (source.getOccurredAt() != null) target.setOccurredAt(source.getOccurredAt());
        if (source.getAmount() != null) target.setAmount(source.getAmount());
        if (source.getCurrency() != null) target.setCurrency(source.getCurrency());
        if (source.getMerchant() != null) target.setMerchant(source.getMerchant());
        if (source.getCategoryId() != null) target.setCategoryId(source.getCategoryId());
        if (source.getAccountId() != null) target.setAccountId(source.getAccountId());
        if (source.getInstallmentCount() != null) target.setInstallmentCount(source.getInstallmentCount());
        if (source.getInstallmentFee() != null) target.setInstallmentFee(source.getInstallmentFee());
        if (source.getNote() != null) target.setNote(source.getNote());
    }

    private boolean hasChanges(FinanceAiCaptureBO.Item target, FinanceAiCaptureItemUpdate source) {
        return !Objects.equals(target.getTransactionType(), source.getTransactionType())
                || !Objects.equals(target.getOccurredAt(), source.getOccurredAt())
                || !Objects.equals(target.getAmount(), source.getAmount())
                || !Objects.equals(target.getCurrency(), source.getCurrency())
                || !Objects.equals(target.getMerchant(), source.getMerchant())
                || !Objects.equals(target.getCategoryId(), source.getCategoryId())
                || !Objects.equals(target.getAccountId(), source.getAccountId())
                || !Objects.equals(target.getInstallmentCount(), source.getInstallmentCount())
                || !Objects.equals(target.getInstallmentFee(), source.getInstallmentFee())
                || !Objects.equals(target.getNote(), source.getNote())
                || !Objects.equals(target.getStatus(), source.getStatus());
    }

    private FinanceAccountBO findAccount(List<FinanceAccountBO> accounts, Long id, String hint, String rawText) {
        FinanceAccountBO direct = accounts.stream().filter(item -> !Boolean.TRUE.equals(item.getArchived()))
                .filter(item -> id != null && item.getId().equals(id)).findFirst().orElse(null);
        if (direct != null) {
            return direct;
        }
        String normalizedHint = normalizeText(hint);
        String normalizedRawText = normalizeText(rawText);
        if (normalizedHint.isBlank() && (normalizedRawText.contains("paymentmethodbalance") || normalizedRawText.contains("\u652f\u4ed8\u65b9\u5f0fbalance"))) {
            normalizedHint = "\u5fae\u4fe1\u4f59\u989d";
        }
        if (normalizedHint.isBlank()) {
            return null;
        }
        String accountHint = normalizedHint;
        boolean wechatBalance = accountHint.contains("\u5fae\u4fe1") || accountHint.contains("balance") || accountHint.contains("wechat");
        return accounts.stream().filter(item -> !Boolean.TRUE.equals(item.getArchived())).filter(item -> {
            String searchable = normalizeText(String.join(" ", item.getName(), item.getAccountType(), item.getNature(),
                    item.getNotes() == null ? "" : item.getNotes()));
            if (normalizeText(item.getName()).equals(accountHint) || searchable.contains(accountHint)
                    || accountHint.contains(normalizeText(item.getName()))) {
                return true;
            }
            return wechatBalance && (searchable.contains("\u5fae\u4fe1") || searchable.contains("wechat") || searchable.contains("\u94b1\u5305"));
        }).findFirst().orElse(null);
    }

    private FinanceCategoryBO findCategory(List<FinanceCategoryBO> categories, Long id, String hint, String direction) {
        String normalizedDirection = normalizeType(direction);
        FinanceCategoryBO direct = categories.stream().filter(item -> !Boolean.TRUE.equals(item.getArchived()))
                .filter(item -> id != null && item.getId().equals(id))
                .filter(item -> "both".equals(item.getDirection()) || normalizedDirection.equals(item.getDirection()))
                .findFirst().orElse(null);
        if (direct != null) {
            return direct;
        }
        FinanceCategoryBO matched = categories.stream()
                .filter(item -> ("both".equals(item.getDirection()) || Objects.equals(item.getDirection(), normalizedDirection)))
                .filter(item -> hint != null && !hint.isBlank()
                        && (item.getName().equalsIgnoreCase(hint) || hint.contains(item.getName()) || item.getName().contains(hint)))
                .findFirst().orElse(null);
        if (matched != null) {
            return matched;
        }
        String fallbackName = "income".equals(normalizedDirection) ? "\u5176\u4ed6\u6536\u5165" : "\u5176\u4ed6\u652f\u51fa";
        return categories.stream()
                .filter(item -> fallbackName.equals(item.getName())
                        && ("both".equals(item.getDirection()) || normalizedDirection.equals(item.getDirection())))
                .findFirst().orElse(null);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeType(String value) {
        return "income".equalsIgnoreCase(value) ? "income" : "expense";
    }

    private String normalizeKey(String value) {
        return value.trim().length() <= 128 ? value.trim() : value.trim().substring(0, 128);
    }

    private String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
