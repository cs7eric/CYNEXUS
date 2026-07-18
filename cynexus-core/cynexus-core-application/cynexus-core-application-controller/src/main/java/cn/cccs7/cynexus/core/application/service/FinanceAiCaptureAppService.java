package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceAiCaptureDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceAiCaptureConfirmRequest;
import cn.cccs7.cynexus.core.application.request.finance.FinanceTransactionRequest;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCategoryBO;
import cn.cccs7.cynexus.core.domain.finance.port.FinanceRepository;
import cn.cccs7.cynexus.core.domain.finance.port.ReceiptAiParserPort;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FinanceAiCaptureAppService {
    private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_IMAGES = 10;

    private final FinanceRepository repository;
    private final ReceiptAiParserPort parser;
    private final FinanceLedgerAppService ledgerAppService;

    public FinanceAiCaptureAppService(FinanceRepository repository, ReceiptAiParserPort parser, FinanceLedgerAppService ledgerAppService) {
        this.repository = repository;
        this.parser = parser;
        this.ledgerAppService = ledgerAppService;
    }

    @Transactional
    public FinanceAiCaptureDTO parse(MultipartFile[] files) {
        if (files == null || files.length == 0) throw new BizException(ErrorCode.BAD_REQUEST, "至少上传一张图片");
        if (files.length > MAX_IMAGES) throw new BizException(ErrorCode.BAD_REQUEST, "单批最多上传 10 张图片");
        List<ReceiptAiParserPort.ReceiptImage> images = java.util.stream.IntStream.range(0, files.length).mapToObj(index -> {
            MultipartFile file = files[index];
            if (file == null || file.isEmpty()) throw new BizException(ErrorCode.BAD_REQUEST, "图片不能为空");
            if (file.getSize() > MAX_IMAGE_BYTES) throw new BizException(ErrorCode.BAD_REQUEST, "单张图片不能超过 10MB");
            if (file.getContentType() == null || !file.getContentType().startsWith("image/")) throw new BizException(ErrorCode.BAD_REQUEST, "只支持图片文件");
            try { return new ReceiptAiParserPort.ReceiptImage(index, file.getOriginalFilename(), file.getContentType(), file.getBytes()); }
            catch (Exception e) { throw new BizException(ErrorCode.BAD_REQUEST, "读取图片失败"); }
        }).toList();

        Long captureId = repository.createCapture(FinanceAiCaptureBO.builder().ownerKey(FinanceLedgerAppService.DEFAULT_OWNER).status("processing").sourceCount(images.size()).build());
        Map<Integer, Long> imageIds = new HashMap<>();
        for (ReceiptAiParserPort.ReceiptImage image : images) {
            imageIds.put(image.imageIndex(), repository.createCaptureImage(captureId, image.imageIndex(), image.fileName(), image.contentType(), image.content().length, sha256(image.content()), image.content()));
        }
        try {
            ReceiptAiParserPort.ParseResult result = parser.parse(FinanceLedgerAppService.DEFAULT_OWNER, images);
            List<FinanceCategoryBO> categories = repository.listCategories(FinanceLedgerAppService.DEFAULT_OWNER, false);
            FinanceAccountBO defaultAccount = repository.listAccounts(FinanceLedgerAppService.DEFAULT_OWNER, false).stream().filter(item -> "asset".equals(item.getNature())).findFirst().orElse(null);
            for (int index = 0; index < result.items().size(); index++) {
                ReceiptAiParserPort.ParsedItem parsed = result.items().get(index);
                FinanceCategoryBO category = findCategory(categories, parsed.categoryHint(), parsed.transactionType());
                repository.createCaptureItem(FinanceAiCaptureBO.Item.builder().captureId(captureId).imageId(imageIds.get(parsed.imageIndex()))
                        .itemIndex(index).transactionType(normalizeType(parsed.transactionType())).occurredAt(parsed.occurredAt()).amount(parsed.amount())
                        .currency(parsed.currency()).merchant(parsed.merchant()).categoryHint(parsed.categoryHint()).categoryId(category == null ? null : category.getId())
                        .accountId(defaultAccount == null ? null : defaultAccount.getId()).note(parsed.note()).confidence(parsed.confidence()).rawText(parsed.rawText()).status("draft").build());
            }
            repository.updateCapture(FinanceAiCaptureBO.builder().id(captureId).ownerKey(FinanceLedgerAppService.DEFAULT_OWNER).status("review")
                    .sourceCount(images.size()).modelCode(result.modelCode()).rawResponse(result.rawResponse()).build());
        } catch (RuntimeException exception) {
            repository.updateCapture(FinanceAiCaptureBO.builder().id(captureId).ownerKey(FinanceLedgerAppService.DEFAULT_OWNER).status("failed")
                    .sourceCount(images.size()).errorMessage(exception.getMessage()).rawResponse("{}").build());
        }
        return toDto(repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, captureId));
    }

    public FinanceAiCaptureDTO get(Long id) {
        FinanceAiCaptureBO capture = repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, id);
        if (capture == null) throw new BizException(ErrorCode.NOT_FOUND, "AI 识别批次不存在");
        return toDto(capture);
    }

    @Transactional
    public FinanceAiCaptureDTO confirm(Long id, FinanceAiCaptureConfirmRequest request) {
        FinanceAiCaptureBO capture = repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, id);
        if (capture == null) throw new BizException(ErrorCode.NOT_FOUND, "AI 识别批次不存在");
        if (!"review".equals(capture.getStatus())) throw new BizException(ErrorCode.BAD_REQUEST, "当前批次不可确认");
        Map<Long, FinanceAiCaptureConfirmRequest.Item> changes = new HashMap<>();
        if (request != null && request.getItems() != null) request.getItems().forEach(item -> changes.put(item.getId(), item));
        for (FinanceAiCaptureBO.Item item : capture.getItems()) {
            if (item.getTransactionId() != null || "confirmed".equals(item.getStatus())) {
                continue;
            }
            FinanceAiCaptureConfirmRequest.Item change = changes.get(item.getId());
            if (change != null) apply(item, change);
            if ("rejected".equals(item.getStatus())) { repository.updateCaptureItem(item); continue; }
            if (item.getAmount() == null || item.getAccountId() == null || item.getCategoryId() == null || item.getOccurredAt() == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "请补全每条草稿的账户、分类、日期和金额");
            }
            FinanceTransactionRequest transaction = new FinanceTransactionRequest();
            transaction.setTransactionType(item.getTransactionType()); transaction.setAccountId(item.getAccountId()); transaction.setCategoryId(item.getCategoryId());
            transaction.setAmount(item.getAmount()); transaction.setCurrency(item.getCurrency()); transaction.setOccurredAt(item.getOccurredAt());
            transaction.setMerchant(item.getMerchant()); transaction.setNote(item.getNote());
            FinanceTransactionDTOHolder posted = new FinanceTransactionDTOHolder(ledgerAppService.saveCapturedTransaction(transaction, "capture-" + id + "-" + item.getId()).getId());
            item.setStatus("confirmed"); item.setTransactionId(posted.id()); repository.updateCaptureItem(item);
        }
        capture.setStatus(capture.getItems().stream().allMatch(item -> "confirmed".equals(item.getStatus()) || "rejected".equals(item.getStatus())) ? "confirmed" : "review");
        repository.updateCapture(capture);
        return toDto(repository.findCapture(FinanceLedgerAppService.DEFAULT_OWNER, id));
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
        if (source.getNote() != null) target.setNote(source.getNote());
    }

    private FinanceCategoryBO findCategory(List<FinanceCategoryBO> categories, String hint, String direction) {
        if (hint == null || hint.isBlank()) return null;
        return categories.stream().filter(item -> ("both".equals(item.getDirection()) || Objects.equals(item.getDirection(), normalizeType(direction))))
                .filter(item -> item.getName().equalsIgnoreCase(hint) || hint.contains(item.getName()) || item.getName().contains(hint)).findFirst().orElse(null);
    }

    private String normalizeType(String value) { return "income".equalsIgnoreCase(value) ? "income" : "expense"; }
    private String sha256(byte[] data) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); } catch (Exception e) { throw new IllegalStateException(e); } }

    private FinanceAiCaptureDTO toDto(FinanceAiCaptureBO value) {
        return FinanceAiCaptureDTO.builder().id(value.getId()).status(value.getStatus()).sourceCount(value.getSourceCount()).modelCode(value.getModelCode())
                .errorMessage(value.getErrorMessage()).createdAt(value.getCreatedAt()).updatedAt(value.getUpdatedAt()).items(value.getItems() == null ? List.of() : value.getItems().stream().map(item -> FinanceAiCaptureDTO.Item.builder()
                        .id(item.getId()).imageId(item.getImageId()).itemIndex(item.getItemIndex()).transactionType(item.getTransactionType()).occurredAt(item.getOccurredAt()).amount(item.getAmount())
                        .currency(item.getCurrency()).merchant(item.getMerchant()).categoryHint(item.getCategoryHint()).categoryId(item.getCategoryId()).categoryName(item.getCategoryName())
                        .accountId(item.getAccountId()).accountName(item.getAccountName()).note(item.getNote()).confidence(item.getConfidence()).rawText(item.getRawText()).status(item.getStatus()).transactionId(item.getTransactionId()).build()).toList()).build();
    }

    private record FinanceTransactionDTOHolder(Long id) { }
}
