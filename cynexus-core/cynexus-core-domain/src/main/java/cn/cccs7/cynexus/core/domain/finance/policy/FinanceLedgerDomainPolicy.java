package cn.cccs7.cynexus.core.domain.finance.policy;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCategoryBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceTransactionBO;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FinanceLedgerDomainPolicy {
    private static final Set<String> ACCOUNT_TYPES = Set.of("cash", "bank", "e_wallet", "huabei", "baitiao", "douyin_monthly", "credit_card", "investment", "other");
    private static final Set<String> TRANSACTION_TYPES = Set.of("expense", "income", "transfer");

    public void validateAccount(FinanceAccountBO account) {
        require(account != null, "账户不能为空");
        requireText(account.getName(), "账户名称不能为空");
        require(ACCOUNT_TYPES.contains(account.getAccountType()), "账户类型不受支持");
        require("asset".equals(account.getNature()) || "credit".equals(account.getNature()), "账户性质不正确");
        require(account.getOpeningBalance() == null || account.getOpeningBalance().signum() >= 0, "期初余额不能为负数");
        if (Set.of("huabei", "baitiao", "douyin_monthly", "credit_card").contains(account.getAccountType())) {
            require("credit".equals(account.getNature()), "信用账户必须是信用性质");
        } else {
            require("asset".equals(account.getNature()), "非信用账户必须是资产性质");
        }
    }

    public void validateCategory(FinanceCategoryBO category) {
        require(category != null, "分类不能为空");
        requireText(category.getName(), "分类名称不能为空");
        require(Set.of("expense", "income", "both").contains(category.getDirection()), "分类方向不正确");
    }

    public void validateTransaction(FinanceTransactionBO transaction, FinanceAccountBO account, FinanceAccountBO toAccount) {
        require(transaction != null, "账目不能为空");
        require(TRANSACTION_TYPES.contains(transaction.getTransactionType()), "账目类型不受支持");
        require(account != null, "资金账户不存在");
        require(transaction.getAmount() != null && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0, "金额必须大于 0");
        require(transaction.getOccurredAt() != null && !transaction.getOccurredAt().isAfter(OffsetDateTime.now().plusMinutes(5)), "发生时间不正确");
        if ("transfer".equals(transaction.getTransactionType())) {
            require(toAccount != null, "转账目标账户不存在");
            require(!account.getId().equals(toAccount.getId()), "转账账户不能相同");
            require(transaction.getCategoryId() == null, "转账不应绑定收支分类");
        } else {
            require(transaction.getToAccountId() == null, "收支账目不能有转入账户");
            require(transaction.getCategoryId() != null, "收支账目需要分类");
        }
    }

    public void validateTransactionMutation(boolean hasInstallmentReference) {
        require(!hasInstallmentReference, "账目已关联分期计划或分期还款，不能直接修改或删除");
    }

    public void validateCustomCategory(FinanceCategoryBO category) {
        require(category != null && !Boolean.TRUE.equals(category.getSystem()), "系统分类不能修改或删除");
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new BizException(ErrorCode.BAD_REQUEST, message);
    }

    private void requireText(String value, String message) {
        require(value != null && !value.isBlank(), message);
    }
}
