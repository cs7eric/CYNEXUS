package cn.cccs7.cynexus.core.domain.service.finance;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCategoryBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceOverviewBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceTransactionBO;
import cn.cccs7.cynexus.core.domain.finance.policy.FinanceLedgerDomainPolicy;
import cn.cccs7.cynexus.core.domain.finance.port.FinanceRepository;
import java.time.LocalDate;
import java.util.List;

/** Coordinates account, category and ledger use cases across finance aggregates. */
public class FinanceLedgerDomainService {
    private final FinanceRepository repository;
    private final FinanceLedgerDomainPolicy policy;

    public FinanceLedgerDomainService(FinanceRepository repository, FinanceLedgerDomainPolicy policy) {
        this.repository = repository;
        this.policy = policy;
    }

    public List<FinanceAccountBO> listAccounts(String ownerKey, boolean includeArchived) {
        return repository.listAccounts(ownerKey, includeArchived);
    }

    public FinanceAccountBO saveAccount(String ownerKey, FinanceAccountBO account) {
        policy.validateAccount(account);
        Long id = account.getId() == null ? repository.saveAccount(account) : updateAccount(account);
        return repository.findAccount(ownerKey, id);
    }

    public boolean archiveAccount(String ownerKey, Long id) {
        return repository.archiveAccount(ownerKey, id);
    }

    public boolean restoreAccount(String ownerKey, Long id) {
        return repository.restoreAccount(ownerKey, id);
    }

    public List<FinanceCategoryBO> listCategories(String ownerKey, boolean includeArchived) {
        return repository.listCategories(ownerKey, includeArchived);
    }

    public FinanceCategoryBO saveCategory(String ownerKey, FinanceCategoryBO category) {
        policy.validateCategory(category);
        if (category.getId() != null) {
            FinanceCategoryBO existing = findCategory(ownerKey, category.getId());
            if (existing == null) throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
            policy.validateCustomCategory(existing);
            policy.validateCustomCategory(category);
            if (!repository.updateCategory(category)) throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
            return findCategory(ownerKey, category.getId());
        }
        FinanceCategoryBO existing = repository.listCategories(ownerKey, true).stream()
                .filter(item -> item.getName().equals(category.getName())).findFirst().orElse(null);
        if (existing != null) return existing;
        return findCategory(ownerKey, repository.saveCategory(category));
    }

    public boolean archiveCategory(String ownerKey, Long id) {
        FinanceCategoryBO category = findCategory(ownerKey, id);
        if (category == null) throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
        policy.validateCustomCategory(category);
        return repository.archiveCategory(ownerKey, id);
    }

    public FinanceOverviewBO overview(String ownerKey, LocalDate start, LocalDate end) {
        LocalDate normalizedStart = start == null ? LocalDate.now().withDayOfMonth(1) : start;
        LocalDate normalizedEnd = end == null ? normalizedStart.withDayOfMonth(normalizedStart.lengthOfMonth()) : end;
        if (normalizedEnd.isBefore(normalizedStart)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "统计结束日期不能早于开始日期");
        }
        return repository.overview(ownerKey, normalizedStart, normalizedEnd);
    }

    public List<FinanceTransactionBO> listTransactions(String ownerKey, LocalDate start, LocalDate end, Integer limit) {
        LocalDate normalizedStart = start == null ? LocalDate.now().withDayOfMonth(1) : start;
        LocalDate normalizedEnd = end == null ? normalizedStart.withDayOfMonth(normalizedStart.lengthOfMonth()) : end;
        return repository.listTransactions(ownerKey, normalizedStart, normalizedEnd, limit == null ? 100 : limit);
    }

    public FinanceTransactionBO findTransaction(String ownerKey, Long id) {
        FinanceTransactionBO value = repository.findTransaction(ownerKey, id);
        if (value == null) throw new BizException(ErrorCode.NOT_FOUND, "账目不存在");
        return value;
    }

    public FinanceTransactionBO saveTransaction(String ownerKey, FinanceTransactionBO transaction, String source, String sourceRef) {
        FinanceAccountBO account = repository.findAccount(ownerKey, transaction.getAccountId());
        FinanceAccountBO toAccount = transaction.getToAccountId() == null ? null
                : repository.findAccount(ownerKey, transaction.getToAccountId());
        policy.validateTransaction(transaction, account, toAccount);
        if (!"transfer".equals(transaction.getTransactionType())) {
            FinanceCategoryBO category = repository.listCategories(ownerKey, false).stream()
                    .filter(item -> item.getId().equals(transaction.getCategoryId())).findFirst().orElse(null);
            if (category == null || !("both".equals(category.getDirection())
                    || category.getDirection().equals(transaction.getTransactionType()))) {
                throw new BizException(ErrorCode.BAD_REQUEST, "分类与账目方向不匹配");
            }
        }
        transaction.setSource(source);
        transaction.setSourceRef(sourceRef);
        Long id = transaction.getId() == null ? repository.saveTransaction(transaction) : updateTransaction(ownerKey, transaction);
        return repository.findTransaction(ownerKey, id);
    }

    public boolean archiveTransaction(String ownerKey, Long id) {
        policy.validateTransactionMutation(repository.hasInstallmentReference(ownerKey, id));
        return repository.archiveTransaction(ownerKey, id);
    }

    public boolean deleteTransaction(String ownerKey, Long id) {
        if (repository.findInstallmentPlanByPurchaseTransaction(ownerKey, id) != null) {
            if (!repository.deleteInstallmentPurchase(ownerKey, id)) throw new BizException(ErrorCode.NOT_FOUND, "分期消费不存在");
            return true;
        }
        if (repository.findInstallmentByPaymentTransaction(ownerKey, id) != null) {
            if (!repository.deleteInstallmentPayment(ownerKey, id)) throw new BizException(ErrorCode.NOT_FOUND, "还款记录不存在");
            return true;
        }
        policy.validateTransactionMutation(repository.hasInstallmentReference(ownerKey, id));
        if (!repository.deleteTransaction(ownerKey, id)) throw new BizException(ErrorCode.NOT_FOUND, "账目不存在");
        return true;
    }

    private FinanceCategoryBO findCategory(String ownerKey, Long id) {
        return repository.listCategories(ownerKey, true).stream()
                .filter(item -> item.getId().equals(id)).findFirst().orElse(null);
    }

    private Long updateAccount(FinanceAccountBO account) {
        if (!repository.updateAccount(account)) throw new BizException(ErrorCode.NOT_FOUND, "账户不存在");
        return account.getId();
    }

    private Long updateTransaction(String ownerKey, FinanceTransactionBO transaction) {
        policy.validateTransactionMutation(repository.hasInstallmentReference(ownerKey, transaction.getId()));
        if (!repository.updateTransaction(transaction)) throw new BizException(ErrorCode.NOT_FOUND, "账目不存在");
        return transaction.getId();
    }
}
