package cn.cccs7.cynexus.core.domain.service.finance;

import cn.cccs7.cynexus.core.common.constant.CoreConstants;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentPlanBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceTransactionBO;
import cn.cccs7.cynexus.core.domain.finance.policy.FinanceInstallmentPolicy;
import cn.cccs7.cynexus.core.domain.finance.port.FinanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Coordinates installment lifecycle and its linked ledger transactions. */
public class FinanceInstallmentDomainService {
    private final FinanceRepository repository;
    private final FinanceLedgerDomainService ledgerService;
    private final FinanceInstallmentPolicy policy;

    public FinanceInstallmentDomainService(FinanceRepository repository, FinanceLedgerDomainService ledgerService,
                                           FinanceInstallmentPolicy policy) {
        this.repository = repository;
        this.ledgerService = ledgerService;
        this.policy = policy;
    }

    public List<FinanceCreditInstallmentPlanBO> list(String ownerKey, Long accountId) {
        return repository.listInstallmentPlans(ownerKey, accountId);
    }

    public FinanceCreditInstallmentPlanBO create(String ownerKey, Long accountId, Long purchaseTransactionId,
                                                  BigDecimal totalAmount, BigDecimal feeTotal, Integer installmentCount,
                                                  LocalDate firstDueDate, Integer intervalMonths, String merchant, String note) {
        FinanceAccountBO account = repository.findAccount(ownerKey, accountId);
        validateCreditAccount(account);
        if (purchaseTransactionId != null) {
            FinanceCreditInstallmentPlanBO existing = repository.findInstallmentPlanByPurchaseTransaction(ownerKey, purchaseTransactionId);
            if (existing != null) return existing;
            FinanceTransactionBO purchase = repository.findTransaction(ownerKey, purchaseTransactionId);
            if (purchase == null || !accountId.equals(purchase.getAccountId()) || !"expense".equals(purchase.getTransactionType())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "分期计划必须关联同一信用账户的支出账目");
            }
        }
        BigDecimal normalizedFee = feeTotal == null ? BigDecimal.ZERO : feeTotal;
        int count = installmentCount == null ? 0 : installmentCount;
        int interval = intervalMonths == null ? 1 : intervalMonths;
        FinanceCreditInstallmentPlanBO plan = FinanceCreditInstallmentPlanBO.builder().ownerKey(ownerKey)
                .accountId(account.getId()).accountName(account.getName()).accountType(account.getAccountType())
                .purchaseTransactionId(purchaseTransactionId).merchant(merchant).totalAmount(totalAmount)
                .feeTotal(normalizedFee).installmentCount(count).firstDueDate(firstDueDate).intervalMonths(interval)
                .status(CoreConstants.STATUS_ACTIVE).note(note).build();
        List<FinanceCreditInstallmentBO> installments = policy.schedule(totalAmount, normalizedFee, count, firstDueDate, interval, null);
        Long planId = repository.saveInstallmentPlan(plan);
        installments.forEach(item -> { item.setPlanId(planId); repository.saveInstallment(item); });
        return repository.findInstallmentPlan(ownerKey, planId);
    }

    public FinanceCreditInstallmentPlanBO pay(String ownerKey, Long planId, Integer installmentNo, Long paymentAccountId,
                                               OffsetDateTime occurredAt, String note) {
        FinanceCreditInstallmentPlanBO plan = repository.findInstallmentPlan(ownerKey, planId);
        if (plan == null) throw new BizException(ErrorCode.NOT_FOUND, "分期计划不存在");
        FinanceCreditInstallmentBO installment = plan.getInstallments().stream()
                .filter(item -> installmentNo != null && installmentNo.equals(item.getInstallmentNo())).findFirst().orElse(null);
        if (installment == null) throw new BizException(ErrorCode.NOT_FOUND, "分期不存在");
        if (!CoreConstants.STATUS_PENDING.equals(installment.getStatus())) throw new BizException(ErrorCode.BAD_REQUEST, "该分期已处理");
        FinanceAccountBO paymentAccount = repository.findAccount(ownerKey, paymentAccountId);
        if (paymentAccount == null || !"asset".equals(paymentAccount.getNature())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "还款账户必须是资产账户");
        }
        FinanceTransactionBO payment = FinanceTransactionBO.builder().ownerKey(ownerKey).transactionType("transfer")
                .accountId(paymentAccount.getId()).toAccountId(plan.getAccountId()).amount(installment.getAmount())
                .currency("CNY").occurredAt(occurredAt == null ? OffsetDateTime.now() : occurredAt)
                .note(note == null ? "信用分期还款" : note).build();
        FinanceTransactionBO savedPayment = ledgerService.saveTransaction(ownerKey, payment, "system", "installment-" + planId + "-" + installmentNo);
        repository.markInstallmentPaid(ownerKey, planId, installmentNo, savedPayment.getId());
        boolean completed = plan.getInstallments().stream().allMatch(item -> item.getInstallmentNo().equals(installmentNo)
                || "paid".equals(item.getStatus()));
        if (completed) repository.updateInstallmentPlanStatus(ownerKey, planId, "completed");
        return repository.findInstallmentPlan(ownerKey, planId);
    }

    public FinanceCreditInstallmentPlanBO createForPurchase(String ownerKey, Long accountId, Long purchaseTransactionId,
                                                             BigDecimal totalAmount, BigDecimal feeTotal, Integer installmentCount,
                                                             OffsetDateTime occurredAt, String merchant) {
        if (installmentCount == null || installmentCount <= 1) return null;
        FinanceAccountBO account = repository.findAccount(ownerKey, accountId);
        if (account == null || !"credit".equals(account.getNature())) return null;
        return create(ownerKey, accountId, purchaseTransactionId, totalAmount, feeTotal, installmentCount,
                firstDueDate(account, occurredAt), null, merchant, null);
    }

    private LocalDate firstDueDate(FinanceAccountBO account, OffsetDateTime occurredAt) {
        LocalDate date = (occurredAt == null ? OffsetDateTime.now() : occurredAt).toLocalDate();
        if (account.getDueDay() == null) return date;
        int day = Math.min(account.getDueDay(), date.lengthOfMonth());
        LocalDate due = date.withDayOfMonth(day);
        return due.isAfter(date) ? due : date.plusMonths(1).withDayOfMonth(Math.min(account.getDueDay(), date.plusMonths(1).lengthOfMonth()));
    }

    private void validateCreditAccount(FinanceAccountBO account) {
        if (account == null || !"credit".equals(account.getNature())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "分期账户必须是信用账户");
        }
    }
}
