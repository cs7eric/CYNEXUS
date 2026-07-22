package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceCreditInstallmentDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceCreditInstallmentPaymentRequest;
import cn.cccs7.cynexus.core.application.request.finance.FinanceCreditInstallmentPlanRequest;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentPlanBO;
import cn.cccs7.cynexus.core.domain.service.finance.FinanceInstallmentDomainService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceInstallmentAppService {
    private final FinanceInstallmentDomainService domainService;

    public FinanceInstallmentAppService(FinanceInstallmentDomainService domainService) {
        this.domainService = domainService;
    }

    public List<FinanceCreditInstallmentDTO.Plan> list(Long accountId) {
        return domainService.list(FinanceLedgerAppService.DEFAULT_OWNER, accountId).stream().map(this::plan).toList();
    }

    @Transactional
    public FinanceCreditInstallmentDTO.Plan create(FinanceCreditInstallmentPlanRequest request) {
        if (request == null) throw new BizException(ErrorCode.BAD_REQUEST, "分期计划不能为空");
        return plan(domainService.create(FinanceLedgerAppService.DEFAULT_OWNER, request.getAccountId(), request.getPurchaseTransactionId(),
                request.getTotalAmount(), request.getFeeTotal(), request.getInstallmentCount(), request.getFirstDueDate(),
                request.getIntervalMonths(), request.getMerchant(), request.getNote()));
    }

    @Transactional
    public FinanceCreditInstallmentDTO.Plan pay(Long planId, Integer installmentNo, FinanceCreditInstallmentPaymentRequest request) {
        if (request == null) throw new BizException(ErrorCode.BAD_REQUEST, "还款账户不能为空");
        return plan(domainService.pay(FinanceLedgerAppService.DEFAULT_OWNER, planId, installmentNo, request.getPaymentAccountId(),
                request.getOccurredAt(), request.getNote()));
    }

    @Transactional
    public FinanceCreditInstallmentDTO.Plan createForPurchase(Long accountId, Long purchaseTransactionId, BigDecimal totalAmount,
                                                               BigDecimal feeTotal, Integer installmentCount, OffsetDateTime occurredAt, String merchant) {
        return plan(domainService.createForPurchase(FinanceLedgerAppService.DEFAULT_OWNER, accountId, purchaseTransactionId,
                totalAmount, feeTotal, installmentCount, occurredAt, merchant));
    }

    private FinanceCreditInstallmentDTO.Plan plan(FinanceCreditInstallmentPlanBO value) {
        if (value == null) return null;
        return FinanceCreditInstallmentDTO.Plan.builder().id(value.getId()).accountId(value.getAccountId()).accountName(value.getAccountName()).accountType(value.getAccountType())
                .purchaseTransactionId(value.getPurchaseTransactionId()).merchant(value.getMerchant()).totalAmount(value.getTotalAmount()).feeTotal(value.getFeeTotal())
                .installmentCount(value.getInstallmentCount()).firstDueDate(value.getFirstDueDate()).intervalMonths(value.getIntervalMonths()).status(value.getStatus())
                .note(value.getNote()).createdAt(value.getCreatedAt()).updatedAt(value.getUpdatedAt()).installments(value.getInstallments() == null ? List.of() : value.getInstallments().stream().map(this::installment).toList()).build();
    }

    private FinanceCreditInstallmentDTO installment(FinanceCreditInstallmentBO value) {
        return FinanceCreditInstallmentDTO.builder().id(value.getId()).planId(value.getPlanId()).installmentNo(value.getInstallmentNo()).dueDate(value.getDueDate())
                .principalAmount(value.getPrincipalAmount()).feeAmount(value.getFeeAmount()).amount(value.getAmount()).status(value.getStatus()).paidAt(value.getPaidAt())
                .paymentTransactionId(value.getPaymentTransactionId()).build();
    }

}
