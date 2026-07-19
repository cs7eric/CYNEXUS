package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceCreditInstallmentDTO;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceTransactionDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceCreditInstallmentPaymentRequest;
import cn.cccs7.cynexus.core.application.request.finance.FinanceCreditInstallmentPlanRequest;
import cn.cccs7.cynexus.core.application.request.finance.FinanceTransactionRequest;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentPlanBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceTransactionBO;
import cn.cccs7.cynexus.core.domain.finance.policy.FinanceInstallmentPolicy;
import cn.cccs7.cynexus.core.domain.finance.port.FinanceRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceInstallmentAppService {
    private final FinanceRepository repository;
    private final FinanceLedgerAppService ledgerAppService;
    private final FinanceInstallmentPolicy policy;

    public FinanceInstallmentAppService(FinanceRepository repository, FinanceLedgerAppService ledgerAppService, FinanceInstallmentPolicy policy) {
        this.repository = repository;
        this.ledgerAppService = ledgerAppService;
        this.policy = policy;
    }

    public List<FinanceCreditInstallmentDTO.Plan> list(Long accountId) {
        return repository.listInstallmentPlans(FinanceLedgerAppService.DEFAULT_OWNER, accountId).stream().map(this::plan).toList();
    }

    @Transactional
    public FinanceCreditInstallmentDTO.Plan create(FinanceCreditInstallmentPlanRequest request) {
        if (request == null) throw new BizException(ErrorCode.BAD_REQUEST, "分期计划不能为空");
        FinanceAccountBO account = repository.findAccount(FinanceLedgerAppService.DEFAULT_OWNER, request.getAccountId());
        validateCreditAccount(account);
        if (request.getPurchaseTransactionId() != null) {
            FinanceCreditInstallmentPlanBO existing = repository.findInstallmentPlanByPurchaseTransaction(FinanceLedgerAppService.DEFAULT_OWNER, request.getPurchaseTransactionId());
            if (existing != null) return plan(existing);
            FinanceTransactionBO purchase = repository.findTransaction(FinanceLedgerAppService.DEFAULT_OWNER, request.getPurchaseTransactionId());
            if (purchase == null || !request.getAccountId().equals(purchase.getAccountId()) || !"expense".equals(purchase.getTransactionType())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "分期计划必须关联同一信用账户的支出账目");
            }
        }
        BigDecimal feeTotal = request.getFeeTotal() == null ? BigDecimal.ZERO : request.getFeeTotal();
        int count = request.getInstallmentCount() == null ? 0 : request.getInstallmentCount();
        int interval = request.getIntervalMonths() == null ? 1 : request.getIntervalMonths();
        FinanceCreditInstallmentPlanBO value = FinanceCreditInstallmentPlanBO.builder().ownerKey(FinanceLedgerAppService.DEFAULT_OWNER)
                .accountId(account.getId()).accountName(account.getName()).accountType(account.getAccountType()).purchaseTransactionId(request.getPurchaseTransactionId())
                .merchant(request.getMerchant()).totalAmount(request.getTotalAmount()).feeTotal(feeTotal).installmentCount(count)
                .firstDueDate(request.getFirstDueDate()).intervalMonths(interval).status("active").note(request.getNote()).build();
        List<FinanceCreditInstallmentBO> installments = policy.schedule(value.getTotalAmount(), value.getFeeTotal(), count, value.getFirstDueDate(), interval, null);
        Long planId = repository.saveInstallmentPlan(value);
        installments.forEach(item -> { item.setPlanId(planId); repository.saveInstallment(item); });
        return plan(repository.findInstallmentPlan(FinanceLedgerAppService.DEFAULT_OWNER, planId));
    }

    @Transactional
    public FinanceCreditInstallmentDTO.Plan pay(Long planId, Integer installmentNo, FinanceCreditInstallmentPaymentRequest request) {
        if (request == null) throw new BizException(ErrorCode.BAD_REQUEST, "还款账户不能为空");
        FinanceCreditInstallmentPlanBO plan = repository.findInstallmentPlan(FinanceLedgerAppService.DEFAULT_OWNER, planId);
        if (plan == null) throw new BizException(ErrorCode.NOT_FOUND, "分期计划不存在");
        FinanceCreditInstallmentBO installment = plan.getInstallments().stream().filter(item -> installmentNo != null && installmentNo.equals(item.getInstallmentNo())).findFirst().orElse(null);
        if (installment == null) throw new BizException(ErrorCode.NOT_FOUND, "分期不存在");
        if (!"pending".equals(installment.getStatus())) throw new BizException(ErrorCode.BAD_REQUEST, "该分期已处理");
        FinanceAccountBO paymentAccount = repository.findAccount(FinanceLedgerAppService.DEFAULT_OWNER, request == null ? null : request.getPaymentAccountId());
        if (paymentAccount == null || !"asset".equals(paymentAccount.getNature())) throw new BizException(ErrorCode.BAD_REQUEST, "还款账户必须是资产账户");
        FinanceTransactionRequest transaction = new FinanceTransactionRequest();
        transaction.setTransactionType("transfer"); transaction.setAccountId(paymentAccount.getId()); transaction.setToAccountId(plan.getAccountId());
        transaction.setAmount(installment.getAmount()); transaction.setCurrency("CNY"); transaction.setOccurredAt(request.getOccurredAt() == null ? OffsetDateTime.now() : request.getOccurredAt());
        transaction.setNote(request.getNote() == null ? "信用分期还款" : request.getNote());
        FinanceTransactionDTO payment = ledgerAppService.saveSystemTransaction(transaction, "installment-" + planId + "-" + installmentNo);
        repository.markInstallmentPaid(FinanceLedgerAppService.DEFAULT_OWNER, planId, installmentNo, payment.getId());
        boolean completed = plan.getInstallments().stream().allMatch(item -> item.getInstallmentNo().equals(installmentNo) || "paid".equals(item.getStatus()));
        if (completed) repository.updateInstallmentPlanStatus(FinanceLedgerAppService.DEFAULT_OWNER, planId, "completed");
        return plan(repository.findInstallmentPlan(FinanceLedgerAppService.DEFAULT_OWNER, planId));
    }

    @Transactional
    public FinanceCreditInstallmentDTO.Plan createForPurchase(Long accountId, Long purchaseTransactionId, BigDecimal totalAmount,
                                                               BigDecimal feeTotal, Integer installmentCount, OffsetDateTime occurredAt, String merchant) {
        if (installmentCount == null || installmentCount <= 1) return null;
        FinanceAccountBO account = repository.findAccount(FinanceLedgerAppService.DEFAULT_OWNER, accountId);
        if (account == null || !"credit".equals(account.getNature())) return null;
        FinanceCreditInstallmentPlanRequest request = new FinanceCreditInstallmentPlanRequest();
        request.setAccountId(accountId); request.setPurchaseTransactionId(purchaseTransactionId); request.setTotalAmount(totalAmount); request.setFeeTotal(feeTotal);
        request.setInstallmentCount(installmentCount); request.setFirstDueDate(firstDueDate(account, occurredAt)); request.setMerchant(merchant);
        return create(request);
    }

    private LocalDate firstDueDate(FinanceAccountBO account, OffsetDateTime occurredAt) {
        LocalDate date = (occurredAt == null ? OffsetDateTime.now() : occurredAt).toLocalDate();
        if (account.getDueDay() == null) return date;
        int day = Math.min(account.getDueDay(), date.lengthOfMonth());
        LocalDate due = date.withDayOfMonth(day);
        return due.isAfter(date) ? due : date.plusMonths(1).withDayOfMonth(Math.min(account.getDueDay(), date.plusMonths(1).lengthOfMonth()));
    }

    private void validateCreditAccount(FinanceAccountBO account) {
        if (account == null || !"credit".equals(account.getNature())) throw new BizException(ErrorCode.BAD_REQUEST, "分期账户必须是信用账户");
    }

    private FinanceCreditInstallmentDTO.Plan plan(FinanceCreditInstallmentPlanBO value) {
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
