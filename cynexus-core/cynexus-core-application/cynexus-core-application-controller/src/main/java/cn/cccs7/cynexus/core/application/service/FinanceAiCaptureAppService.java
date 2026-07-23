package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceAiCaptureDTO;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceAiCapturePageDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceAiCaptureConfirmRequest;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureHistoryPageBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureItemUpdate;
import cn.cccs7.cynexus.core.domain.service.finance.FinanceAiCaptureDomainService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FinanceAiCaptureAppService {
    private final FinanceAiCaptureDomainService domainService;

    public FinanceAiCaptureAppService(FinanceAiCaptureDomainService domainService) {
        this.domainService = domainService;
    }

    @Transactional
    public FinanceAiCaptureDTO parse(MultipartFile file, String mode, String idempotencyKey) {
        byte[] content = new byte[0];
        String contentType = null;
        String fileName = null;
        long size = 0L;
        if (file != null && !file.isEmpty()) {
            contentType = file.getContentType();
            fileName = file.getOriginalFilename();
            size = file.getSize();
            try {
                content = file.getBytes();
            } catch (Exception exception) {
                throw new BizException(ErrorCode.BAD_REQUEST, "\u8bfb\u53d6\u56fe\u7247\u5931\u8d25");
            }
        }
        String processingMode = "review".equalsIgnoreCase(mode) ? "review" : "auto";
        return toDto(domainService.parse(FinanceLedgerAppService.DEFAULT_OWNER, fileName, contentType, content, size, mode, idempotencyKey),
                processingMode);
    }

    public FinanceAiCaptureDTO get(Long id) {
        return toDto(domainService.get(FinanceLedgerAppService.DEFAULT_OWNER, id), "review");
    }

    public FinanceAiCapturePageDTO history(int limit, int offset, String status) {
        FinanceAiCaptureHistoryPageBO page = domainService.history(FinanceLedgerAppService.DEFAULT_OWNER, limit, offset, status);
        return FinanceAiCapturePageDTO.builder()
                .items(page.getItems().stream().map(item -> toDto(item, item.getProcessingMode())).toList())
                .total(page.getTotal())
                .limit(page.getLimit())
                .offset(page.getOffset())
                .status(page.getStatus())
                .build();
    }

    @Transactional
    public FinanceAiCaptureDTO confirm(Long id, FinanceAiCaptureConfirmRequest request) {
        List<FinanceAiCaptureItemUpdate> updates = request == null || request.getItems() == null
                ? List.of()
                : request.getItems().stream().map(this::toUpdate).toList();
        return toDto(domainService.confirm(FinanceLedgerAppService.DEFAULT_OWNER, id, updates), "review");
    }

    @Transactional
    public FinanceAiCaptureDTO retry(Long id) {
        return toDto(domainService.retry(FinanceLedgerAppService.DEFAULT_OWNER, id), "review");
    }

    @Transactional
    public boolean archive(Long id) {
        return domainService.archive(FinanceLedgerAppService.DEFAULT_OWNER, id);
    }

    private FinanceAiCaptureItemUpdate toUpdate(FinanceAiCaptureConfirmRequest.Item source) {
        FinanceAiCaptureItemUpdate update = new FinanceAiCaptureItemUpdate();
        update.setId(source.getId());
        update.setStatus(source.getStatus());
        update.setTransactionType(source.getTransactionType());
        update.setOccurredAt(source.getOccurredAt());
        update.setAmount(source.getAmount());
        update.setCurrency(source.getCurrency());
        update.setMerchant(source.getMerchant());
        update.setCategoryId(source.getCategoryId());
        update.setAccountId(source.getAccountId());
        update.setInstallmentCount(source.getInstallmentCount());
        update.setInstallmentFee(source.getInstallmentFee());
        update.setNote(source.getNote());
        return update;
    }

    private FinanceAiCaptureDTO toDto(FinanceAiCaptureBO value, String processingMode) {
        long autoPosted = value.getItems() == null ? 0 : value.getItems().stream().filter(item -> "confirmed".equals(item.getStatus())).count();
        long review = value.getItems() == null ? 0 : value.getItems().stream().filter(item -> "draft".equals(item.getStatus())).count();
        return FinanceAiCaptureDTO.builder()
                .id(value.getId())
                .status(value.getStatus())
                .processingMode(value.getProcessingMode() == null ? processingMode : value.getProcessingMode())
                .sourceCount(value.getSourceCount())
                .modelCode(value.getModelCode())
                .errorMessage(value.getErrorMessage())
                .autoPostedCount((int) autoPosted)
                .reviewCount((int) review)
                .createdAt(value.getCreatedAt())
                .updatedAt(value.getUpdatedAt())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(item -> FinanceAiCaptureDTO.Item.builder()
                        .id(item.getId())
                        .imageId(item.getImageId())
                        .itemIndex(item.getItemIndex())
                        .transactionType(item.getTransactionType())
                        .occurredAt(item.getOccurredAt())
                        .amount(item.getAmount())
                        .currency(item.getCurrency())
                        .merchant(item.getMerchant())
                        .categoryHint(item.getCategoryHint())
                        .categoryId(item.getCategoryId())
                        .categoryName(item.getCategoryName())
                        .accountId(item.getAccountId())
                        .accountName(item.getAccountName())
                        .installmentCount(item.getInstallmentCount())
                        .installmentFee(item.getInstallmentFee())
                        .note(item.getNote())
                        .confidence(item.getConfidence())
                        .rawText(item.getRawText())
                        .status(item.getStatus())
                        .transactionId(item.getTransactionId())
                        .build()).toList())
                .build();
    }
}
