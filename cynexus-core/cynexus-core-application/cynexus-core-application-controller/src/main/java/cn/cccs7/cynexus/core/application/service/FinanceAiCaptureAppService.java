package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceAiCaptureDTO;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceAiCapturePageDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceAiCaptureConfirmRequest;
import cn.cccs7.cynexus.core.application.request.finance.FinanceTransactionRequest;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCategoryBO;
import cn.cccs7.cynexus.core.domain.finance.port.FinanceRepository;
import cn.cccs7.cynexus.core.domain.finance.port.ReceiptAiParserPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FinanceAiCaptureAppService {
    private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private final FinanceRepository repository;
    private final ReceiptAiParserPort parser;
    private final FinanceLedgerAppService ledgerAppService;
    private final FinanceInstallmentAppService installmentAppService;

    public FinanceAiCaptureAppService(FinanceRepository repository, ReceiptAiParserPort parser, FinanceLedgerAppService ledgerAppService,
                                      FinanceInstallmentAppService installmentAppService) {
        this.repository = repository;
        this.parser = parser;
        this.ledgerAppService = ledgerAppService;
        this.installmentAppService = installmentAppService;
    }

    @Transactional
    public FinanceAiCaptureDTO parse(MultipartFile file, String mode, String idempotencyKey) {
        String processingMode = "review".equalsIgnoreCase(mode) ? "review" : "auto";
        byte[] content = null;
        String validationError = null;
        if (file == null || file.isEmpty()) {
            validationError = "必须上传一张图片";
        } else if (file.getSize() > MAX_IMAGE_BYTES) {
            validationError = "图片不能超过 10MB";
        } else {
            try {
                content = file.getBytes();
            } catch (Exception exception) {
                validationError = "读取图片失败";
            }
            if (validationError == null && (file.getContentType() == null || !file.getContentType().startsWith("image/"))) {
                validationError = "只支持图片文件";
            }
        }
        String suppliedKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : normalizeKey(idempotencyKey);
        String requestKey = suppliedKey != null
                ? suppliedKey
                : content != null && content.length > 0
                ? sha256((processingMode + ":" + sha256(content)).getBytes(StandardCharsets.UTF_8))
                : "invalid-" + UUID.randomUUID();
        FinanceAiCaptureBO existing = repository.findCaptureByIdempotencyKey(FinanceLedgerAppService.DEFAULT_OWNER, requestKey);
        if (existing != null) return toDto(existing, processingMode);
        int sourceCount = content == null || content.length == 0 ? 0 : 1;
        Long captureId = repository.createCapture(FinanceAiCaptureBO.builder().ownerKey(FinanceLedgerAppService.DEFAULT_OWNER).status("processing")
                .processingMode(processingMode).sourceCount(sourceCount).idempotencyKey(requestKey).build());
        if (validationError != null) {
            repository.updateCapture(FinanceAiCaptureBO.builder().id(captureId).ownerKey(FinanceLedgerAppService.DEFAULT_OWNER).status("failed")
                    .sourceCount(sourceCount).errorMessage(validationError).rawResponse("{}").build());
            return toDto(repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, captureId), processingMode);
        }
        ReceiptAiParserPort.ReceiptImage receiptImage = new ReceiptAiParserPort.ReceiptImage(0, file.getOriginalFilename(), file.getContentType(), content);
        List<ReceiptAiParserPort.ReceiptImage> images = List.of(receiptImage);
        Map<Integer, Long> imageIds = new HashMap<>();
        for (ReceiptAiParserPort.ReceiptImage image : images) {
            imageIds.put(image.imageIndex(), repository.createCaptureImage(captureId, image.imageIndex(), image.fileName(), image.contentType(), image.content().length, sha256(image.content()), image.content()));
        }
        return processCapture(captureId, images, imageIds, processingMode);
    }

    public FinanceAiCaptureDTO get(Long id) {
        FinanceAiCaptureBO capture = repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, id);
        if (capture == null) throw new BizException(ErrorCode.NOT_FOUND, "AI 识别批次不存在");
        return toDto(capture, "review");
    }

    public FinanceAiCapturePageDTO history(int limit, int offset, String status) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        int normalizedOffset = Math.max(0, offset);
        String normalizedStatus = status == null || status.isBlank() ? "all" : status.trim().toLowerCase();
        return FinanceAiCapturePageDTO.builder()
                .items(repository.listCaptures(FinanceLedgerAppService.DEFAULT_OWNER, normalizedLimit, normalizedOffset, normalizedStatus).stream()
                        .map(item -> toDto(item, item.getProcessingMode())).toList())
                .total(repository.countCaptures(FinanceLedgerAppService.DEFAULT_OWNER, normalizedStatus))
                .limit(normalizedLimit).offset(normalizedOffset).status(normalizedStatus).build();
    }

    @Transactional
    public FinanceAiCaptureDTO confirm(Long id, FinanceAiCaptureConfirmRequest request) {
        FinanceAiCaptureBO capture = repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, id);
        if (capture == null) throw new BizException(ErrorCode.NOT_FOUND, "AI 识别批次不存在");
        if (!List.of("review", "failed", "confirmed").contains(capture.getStatus())) throw new BizException(ErrorCode.BAD_REQUEST, "当前批次不可确认");
        if (capture.getItems() == null || capture.getItems().isEmpty()) throw new BizException(ErrorCode.BAD_REQUEST, "当前批次没有可入账的识别结果");
        Map<Long, FinanceAiCaptureConfirmRequest.Item> changes = new HashMap<>();
        if (request != null && request.getItems() != null) request.getItems().forEach(item -> changes.put(item.getId(), item));
        for (FinanceAiCaptureBO.Item item : capture.getItems()) {
            FinanceAiCaptureConfirmRequest.Item change = changes.get(item.getId());
            boolean changed = change != null && hasChanges(item, change);
            if (change != null) apply(item, change);
            if (item.getTransactionId() != null) {
                if ("rejected".equals(item.getStatus())) throw new BizException(ErrorCode.BAD_REQUEST, "已入账账目不能标记为拒绝，请直接删除账目");
                if (changed) ledgerAppService.saveCapturedTransaction(transaction(item), "capture-" + id + "-" + item.getId());
                item.setStatus("confirmed");
                repository.updateCaptureItem(item);
                continue;
            }
            if ("rejected".equals(item.getStatus())) { repository.updateCaptureItem(item); continue; }
            if (item.getAmount() == null || item.getAccountId() == null || item.getCategoryId() == null || item.getOccurredAt() == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "请补全每条草稿的账户、分类、日期和金额");
            }
            Long transactionId = ledgerAppService.saveCapturedTransaction(transaction(item), "capture-" + id + "-" + item.getId()).getId();
            item.setStatus("confirmed"); item.setTransactionId(transactionId); repository.updateCaptureItem(item);
            installmentAppService.createForPurchase(item.getAccountId(), transactionId, item.getAmount(), item.getInstallmentFee(), item.getInstallmentCount(), item.getOccurredAt(), item.getMerchant());
        }
        capture.setStatus(capture.getItems().stream().allMatch(item -> "confirmed".equals(item.getStatus()) || "rejected".equals(item.getStatus())) ? "confirmed" : "review");
        repository.updateCapture(capture);
        return toDto(repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, id), "review");
    }

    @Transactional
    public FinanceAiCaptureDTO retry(Long id) {
        FinanceAiCaptureBO capture = repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, id);
        if (capture == null) throw new BizException(ErrorCode.NOT_FOUND, "AI 识别批次不存在");
        if (!"failed".equals(capture.getStatus())) throw new BizException(ErrorCode.BAD_REQUEST, "只有失败批次可以重新识别");
        if (capture.getItems() != null && capture.getItems().stream().anyMatch(item -> item.getTransactionId() != null)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该批次已有账目入账，请先处理关联账目");
        }
        List<ReceiptAiParserPort.ReceiptImage> images = repository.listCaptureImages(FinanceLedgerAppService.DEFAULT_OWNER, id);
        if (images.isEmpty()) throw new BizException(ErrorCode.BAD_REQUEST, "原始图片未保存，无法重新识别");
        repository.deleteCaptureItems(FinanceLedgerAppService.DEFAULT_OWNER, id);
        String processingMode = capture.getProcessingMode() == null ? "review" : capture.getProcessingMode();
        repository.updateCapture(FinanceAiCaptureBO.builder().id(id).ownerKey(FinanceLedgerAppService.DEFAULT_OWNER).status("processing")
                .processingMode(processingMode).sourceCount(images.size()).errorMessage(null).rawResponse("{}").build());
        return processCapture(id, images, Map.of(), processingMode);
    }

    @Transactional
    public boolean archive(Long id) {
        return repository.archiveCapture(FinanceLedgerAppService.DEFAULT_OWNER, id);
    }

    private void apply(FinanceAiCaptureBO.Item target, FinanceAiCaptureConfirmRequest.Item source) {
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

    private boolean hasChanges(FinanceAiCaptureBO.Item target, FinanceAiCaptureConfirmRequest.Item source) {
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

    private FinanceTransactionRequest transaction(FinanceAiCaptureBO.Item item) {
        FinanceTransactionRequest transaction = new FinanceTransactionRequest();
        transaction.setId(item.getTransactionId()); transaction.setTransactionType(item.getTransactionType());
        transaction.setAccountId(item.getAccountId()); transaction.setCategoryId(item.getCategoryId());
        transaction.setAmount(item.getAmount()); transaction.setCurrency(item.getCurrency()); transaction.setOccurredAt(item.getOccurredAt());
        transaction.setMerchant(item.getMerchant()); transaction.setNote(item.getNote());
        return transaction;
    }

    private FinanceAiCaptureDTO processCapture(Long captureId, List<ReceiptAiParserPort.ReceiptImage> images,
                                               Map<Integer, Long> imageIds, String processingMode) {
        try {
            List<FinanceAccountBO> accounts = repository.listAccounts(FinanceLedgerAppService.DEFAULT_OWNER, false);
            List<FinanceCategoryBO> categories = repository.listCategories(FinanceLedgerAppService.DEFAULT_OWNER, false);
            ReceiptAiParserPort.ParseResult result = parser.parse(FinanceLedgerAppService.DEFAULT_OWNER, images,
                    accounts.stream().map(item -> new ReceiptAiParserPort.AccountCandidate(item.getId(), item.getName(), item.getAccountType(), item.getNature(), item.getNotes())).toList(),
                    categories.stream().map(item -> new ReceiptAiParserPort.CategoryCandidate(item.getId(), item.getName(), item.getDirection())).toList());
            List<ReceiptAiParserPort.ParsedItem> parsedItems = result.items() == null ? List.of() : result.items();
            for (int index = 0; index < parsedItems.size(); index++) {
                ReceiptAiParserPort.ParsedItem parsed = parsedItems.get(index);
                FinanceCategoryBO category = findCategory(categories, parsed.categoryId(), parsed.categoryHint(), parsed.transactionType());
                FinanceAccountBO account = findAccount(accounts, parsed.accountId(), parsed.accountHint(), parsed.rawText());
                repository.createCaptureItem(FinanceAiCaptureBO.Item.builder().captureId(captureId).imageId(imageIds.get(parsed.imageIndex()))
                        .itemIndex(index).transactionType(normalizeType(parsed.transactionType())).occurredAt(parsed.occurredAt() == null && "auto".equals(processingMode) ? OffsetDateTime.now() : parsed.occurredAt()).amount(parsed.amount())
                        .currency(parsed.currency()).merchant(parsed.merchant()).categoryHint(parsed.categoryHint()).categoryId(category == null ? null : category.getId())
                        .accountId(account == null ? null : account.getId()).installmentCount(parsed.installmentCount()).installmentFee(parsed.installmentFee())
                        .note(parsed.note()).confidence(parsed.confidence()).rawText(parsed.rawText()).status("draft").build());
            }
            repository.updateCapture(FinanceAiCaptureBO.builder().id(captureId).ownerKey(FinanceLedgerAppService.DEFAULT_OWNER).status("review")
                    .processingMode(processingMode).sourceCount(images.size()).modelCode(result.modelCode()).rawResponse(result.rawResponse()).errorMessage(null).build());
            if ("auto".equals(processingMode) && !parsedItems.isEmpty()) autoPost(captureId);
        } catch (RuntimeException exception) {
            repository.updateCapture(FinanceAiCaptureBO.builder().id(captureId).ownerKey(FinanceLedgerAppService.DEFAULT_OWNER).status("failed")
                    .processingMode(processingMode).sourceCount(images.size()).errorMessage(exception.getMessage()).rawResponse("{}").build());
        }
        return toDto(repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, captureId), processingMode);
    }

    private FinanceAccountBO findAccount(List<FinanceAccountBO> accounts, Long id, String hint, String rawText) {
        FinanceAccountBO direct = accounts.stream().filter(item -> !Boolean.TRUE.equals(item.getArchived())).filter(item -> id != null && item.getId().equals(id)).findFirst().orElse(null);
        if (direct != null) return direct;
        String normalizedHint = normalizeText(hint);
        String normalizedRawText = normalizeText(rawText);
        if (normalizedHint.isBlank() && (normalizedRawText.contains("paymentmethodbalance") || normalizedRawText.contains("支付方式balance"))) {
            normalizedHint = "微信余额";
        }
        if (normalizedHint.isBlank()) return null;
        String accountHint = normalizedHint;
        boolean wechatBalance = accountHint.contains("微信") || accountHint.contains("balance") || accountHint.contains("wechat");
        return accounts.stream().filter(item -> !Boolean.TRUE.equals(item.getArchived())).filter(item -> {
            String searchable = normalizeText(String.join(" ", item.getName(), item.getAccountType(), item.getNature(), item.getNotes() == null ? "" : item.getNotes()));
            if (normalizeText(item.getName()).equals(accountHint) || searchable.contains(accountHint) || accountHint.contains(normalizeText(item.getName()))) return true;
            return wechatBalance && (searchable.contains("微信") || searchable.contains("wechat") || searchable.contains("钱包"));
        }).findFirst().orElse(null);
    }

    private FinanceCategoryBO findCategory(List<FinanceCategoryBO> categories, Long id, String hint, String direction) {
        String normalizedDirection = normalizeType(direction);
        FinanceCategoryBO direct = categories.stream().filter(item -> !Boolean.TRUE.equals(item.getArchived())).filter(item -> id != null && item.getId().equals(id))
                .filter(item -> "both".equals(item.getDirection()) || normalizedDirection.equals(item.getDirection())).findFirst().orElse(null);
        if (direct != null) return direct;
        FinanceCategoryBO matched = categories.stream().filter(item -> ("both".equals(item.getDirection()) || Objects.equals(item.getDirection(), normalizedDirection)))
                .filter(item -> hint != null && !hint.isBlank() && (item.getName().equalsIgnoreCase(hint) || hint.contains(item.getName()) || item.getName().contains(hint))).findFirst().orElse(null);
        if (matched != null) return matched;
        String fallbackName = "income".equals(normalizedDirection) ? "其他收入" : "其他支出";
        return categories.stream().filter(item -> fallbackName.equals(item.getName()) && ("both".equals(item.getDirection()) || normalizedDirection.equals(item.getDirection()))).findFirst().orElse(null);
    }

    private String normalizeText(String value) { return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT); }

    private String normalizeType(String value) { return "income".equalsIgnoreCase(value) ? "income" : "expense"; }
    private String normalizeKey(String value) { return value.trim().length() <= 128 ? value.trim() : value.trim().substring(0, 128); }
    private String sha256(byte[] data) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); } catch (Exception e) { throw new IllegalStateException(e); } }

    private void autoPost(Long captureId) {
        FinanceAiCaptureBO capture = repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, captureId);
        for (FinanceAiCaptureBO.Item item : capture.getItems()) {
            if (item.getAmount() == null || item.getAccountId() == null || item.getCategoryId() == null || item.getOccurredAt() == null) continue;
            FinanceTransactionRequest transaction = new FinanceTransactionRequest();
            transaction.setTransactionType(item.getTransactionType()); transaction.setAccountId(item.getAccountId()); transaction.setCategoryId(item.getCategoryId());
            transaction.setAmount(item.getAmount()); transaction.setCurrency(item.getCurrency()); transaction.setOccurredAt(item.getOccurredAt());
            transaction.setMerchant(item.getMerchant()); transaction.setNote(item.getNote());
            Long transactionId = ledgerAppService.saveCapturedTransaction(transaction, "capture-" + captureId + "-" + item.getId()).getId();
            item.setStatus("confirmed"); item.setTransactionId(transactionId); repository.updateCaptureItem(item);
            installmentAppService.createForPurchase(item.getAccountId(), transactionId, item.getAmount(), item.getInstallmentFee(), item.getInstallmentCount(), item.getOccurredAt(), item.getMerchant());
        }
        capture.setStatus(capture.getItems().stream().allMatch(item -> "confirmed".equals(item.getStatus())) ? "confirmed" : "review");
        repository.updateCapture(capture);
    }

    private FinanceAiCaptureDTO toDto(FinanceAiCaptureBO value, String processingMode) {
        long autoPosted = value.getItems() == null ? 0 : value.getItems().stream().filter(item -> "confirmed".equals(item.getStatus())).count();
        long review = value.getItems() == null ? 0 : value.getItems().stream().filter(item -> "draft".equals(item.getStatus())).count();
        return FinanceAiCaptureDTO.builder().id(value.getId()).status(value.getStatus()).processingMode(value.getProcessingMode() == null ? processingMode : value.getProcessingMode()).sourceCount(value.getSourceCount()).modelCode(value.getModelCode())
                .errorMessage(value.getErrorMessage()).autoPostedCount((int) autoPosted).reviewCount((int) review).createdAt(value.getCreatedAt()).updatedAt(value.getUpdatedAt()).items(value.getItems() == null ? List.of() : value.getItems().stream().map(item -> FinanceAiCaptureDTO.Item.builder()
                        .id(item.getId()).imageId(item.getImageId()).itemIndex(item.getItemIndex()).transactionType(item.getTransactionType()).occurredAt(item.getOccurredAt()).amount(item.getAmount())
                        .currency(item.getCurrency()).merchant(item.getMerchant()).categoryHint(item.getCategoryHint()).categoryId(item.getCategoryId()).categoryName(item.getCategoryName())
                        .accountId(item.getAccountId()).accountName(item.getAccountName()).installmentCount(item.getInstallmentCount()).installmentFee(item.getInstallmentFee())
                        .note(item.getNote()).confidence(item.getConfidence()).rawText(item.getRawText()).status(item.getStatus()).transactionId(item.getTransactionId()).build()).toList()).build();
    }

}
