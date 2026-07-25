package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceAccountDTO;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceCategoryDTO;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceOverviewDTO;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceTransactionDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceAccountRequest;
import cn.cccs7.cynexus.core.application.request.finance.FinanceCategoryRequest;
import cn.cccs7.cynexus.core.application.request.finance.FinanceTransactionRequest;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCategoryBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceOverviewBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceTransactionBO;
import cn.cccs7.cynexus.core.domain.service.finance.FinanceLedgerDomainService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceLedgerAppService {
    public static final String DEFAULT_OWNER = CommonConstants.DEFAULT_SPACE_CODE;

    private final FinanceLedgerDomainService domainService;

    public FinanceLedgerAppService(FinanceLedgerDomainService domainService) {
        this.domainService = domainService;
    }

    public List<FinanceAccountDTO> listAccounts(boolean includeArchived) {
        return domainService.listAccounts(DEFAULT_OWNER, includeArchived).stream().map(this::account).toList();
    }

    @Transactional
    public FinanceAccountDTO saveAccount(FinanceAccountRequest request) {
        FinanceAccountBO value = FinanceAccountBO.builder().id(request.getId()).ownerKey(DEFAULT_OWNER).name(request.getName())
                .accountType(request.getAccountType()).nature(request.getNature()).currency(request.getCurrency())
                .openingBalance(request.getOpeningBalance()).creditLimit(request.getCreditLimit()).billingDay(request.getBillingDay())
                .dueDay(request.getDueDay()).color(request.getColor()).notes(request.getNotes()).build();
        return account(domainService.saveAccount(DEFAULT_OWNER, value));
    }

    @Transactional
    public boolean archiveAccount(Long id) { return domainService.archiveAccount(DEFAULT_OWNER, id); }

    @Transactional
    public boolean restoreAccount(Long id) { return domainService.restoreAccount(DEFAULT_OWNER, id); }

    public List<FinanceCategoryDTO> listCategories(boolean includeArchived) {
        return domainService.listCategories(DEFAULT_OWNER, includeArchived).stream().map(this::category).toList();
    }

    @Transactional
    public FinanceCategoryDTO saveCategory(FinanceCategoryRequest request) {
        FinanceCategoryBO value = FinanceCategoryBO.builder().id(request.getId()).ownerKey(DEFAULT_OWNER).parentId(request.getParentId())
                .name(request.getName()).direction(request.getDirection()).icon(request.getIcon()).color(request.getColor())
                .sortOrder(request.getSortOrder()).build();
        return category(domainService.saveCategory(DEFAULT_OWNER, value));
    }

    @Transactional
    public boolean archiveCategory(Long id) { return domainService.archiveCategory(DEFAULT_OWNER, id); }

    public FinanceOverviewDTO overview(LocalDate start, LocalDate end) {
        return overview(domainService.overview(DEFAULT_OWNER, start, end));
    }

    public List<FinanceTransactionDTO> listTransactions(LocalDate start, LocalDate end, Integer limit) {
        return domainService.listTransactions(DEFAULT_OWNER, start, end, limit).stream().map(this::transaction).toList();
    }

    public FinanceTransactionDTO findTransaction(Long id) {
        return transaction(domainService.findTransaction(DEFAULT_OWNER, id));
    }

    @Transactional
    public FinanceTransactionDTO saveTransaction(FinanceTransactionRequest request) {
        return saveTransaction(request, "manual", null);
    }

    @Transactional
    public FinanceTransactionDTO saveCapturedTransaction(FinanceTransactionRequest request, String sourceRef) {
        return saveTransaction(request, "ai_capture", sourceRef);
    }

    @Transactional
    public FinanceTransactionDTO saveSystemTransaction(FinanceTransactionRequest request, String sourceRef) {
        return saveTransaction(request, "system", sourceRef);
    }

    private FinanceTransactionDTO saveTransaction(FinanceTransactionRequest request, String source, String sourceRef) {
        OffsetDateTime occurredAt = request.getOccurredAt() == null ? OffsetDateTime.now() : request.getOccurredAt();
        FinanceTransactionBO value = FinanceTransactionBO.builder().id(request.getId()).ownerKey(DEFAULT_OWNER)
                .transactionType(request.getTransactionType()).accountId(request.getAccountId()).toAccountId(request.getToAccountId())
                .categoryId(request.getCategoryId()).amount(request.getAmount()).currency(request.getCurrency()).occurredAt(occurredAt)
                .merchant(request.getMerchant()).note(request.getNote()).build();
        return transaction(domainService.saveTransaction(DEFAULT_OWNER, value, source, sourceRef));
    }

    @Transactional
    public boolean archiveTransaction(Long id) { return domainService.archiveTransaction(DEFAULT_OWNER, id); }

    @Transactional
    public boolean deleteTransaction(Long id) { return domainService.deleteTransaction(DEFAULT_OWNER, id); }

    private FinanceAccountDTO account(FinanceAccountBO value) {
        if (value == null) return null;
        return FinanceAccountDTO.builder().id(value.getId()).name(value.getName()).accountType(value.getAccountType()).nature(value.getNature())
                .currency(value.getCurrency()).openingBalance(value.getOpeningBalance()).creditLimit(value.getCreditLimit()).billingDay(value.getBillingDay())
                .dueDay(value.getDueDay()).color(value.getColor()).notes(value.getNotes()).archived(value.getArchived()).balance(value.getBalance()).createdAt(value.getCreatedAt()).build();
    }

    private FinanceCategoryDTO category(FinanceCategoryBO value) {
        if (value == null) return null;
        return FinanceCategoryDTO.builder().id(value.getId()).parentId(value.getParentId()).name(value.getName()).direction(value.getDirection())
                .icon(value.getIcon()).color(value.getColor()).sortOrder(value.getSortOrder()).system(value.getSystem()).archived(value.getArchived()).createdAt(value.getCreatedAt()).build();
    }

    private FinanceTransactionDTO transaction(FinanceTransactionBO value) {
        if (value == null) return null;
        return FinanceTransactionDTO.builder().id(value.getId()).transactionType(value.getTransactionType()).accountId(value.getAccountId())
                .accountName(value.getAccountName()).toAccountId(value.getToAccountId()).toAccountName(value.getToAccountName()).categoryId(value.getCategoryId())
                .categoryName(value.getCategoryName()).amount(value.getAmount()).currency(value.getCurrency()).occurredAt(value.getOccurredAt())
                .merchant(value.getMerchant()).note(value.getNote()).source(value.getSource()).archived(value.getArchived()).attachmentCount(value.getAttachmentCount()).createdAt(value.getCreatedAt()).build();
    }

    private FinanceOverviewDTO overview(FinanceOverviewBO value) {
        return FinanceOverviewDTO.builder().startDate(value.getStartDate()).endDate(value.getEndDate()).income(value.getIncome()).expense(value.getExpense()).net(value.getNet())
                .assetTotal(value.getAssetTotal()).liabilityTotal(value.getLiabilityTotal()).netAssets(value.getNetAssets())
                .accounts(value.getAccounts().stream().map(this::account).toList())
                .categories(value.getCategories().stream().map(item -> FinanceOverviewDTO.CategoryStat.builder().categoryId(item.getCategoryId()).categoryName(item.getCategoryName()).direction(item.getDirection()).amount(item.getAmount()).percentage(item.getPercentage()).build()).toList())
                .daily(value.getDaily().stream().map(item -> FinanceOverviewDTO.DailyStat.builder().date(item.getDate()).income(item.getIncome()).expense(item.getExpense()).activityCount(item.getActivityCount()).build()).toList())
                .recentTransactions(value.getRecentTransactions().stream().map(this::transaction).toList()).build();
    }
}
