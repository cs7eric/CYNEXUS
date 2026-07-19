package cn.cccs7.cynexus.core.domain.finance.port;

import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCategoryBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceOverviewBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceTransactionBO;
import java.time.LocalDate;
import java.util.List;

public interface FinanceRepository {
    List<FinanceAccountBO> listAccounts(String ownerKey, boolean includeArchived);
    FinanceAccountBO findAccount(String ownerKey, Long id);
    Long saveAccount(FinanceAccountBO account);
    boolean updateAccount(FinanceAccountBO account);
    boolean archiveAccount(String ownerKey, Long id);

    List<FinanceCategoryBO> listCategories(String ownerKey, boolean includeArchived);
    Long saveCategory(FinanceCategoryBO category);

    Long saveTransaction(FinanceTransactionBO transaction);
    boolean updateTransaction(FinanceTransactionBO transaction);
    boolean archiveTransaction(String ownerKey, Long id);
    FinanceTransactionBO findTransaction(String ownerKey, Long id);
    List<FinanceTransactionBO> listTransactions(String ownerKey, LocalDate start, LocalDate end, int limit);
    FinanceOverviewBO overview(String ownerKey, LocalDate start, LocalDate end);

    Long createCapture(FinanceAiCaptureBO capture);
    FinanceAiCaptureBO findCaptureByIdempotencyKey(String ownerKey, String idempotencyKey);
    boolean updateCapture(FinanceAiCaptureBO capture);
    FinanceAiCaptureBO findCapture(String ownerKey, Long captureId);
    boolean archiveCapture(String ownerKey, Long captureId);
    Long createCaptureImage(Long captureId, Integer imageIndex, String fileName, String contentType,
                            long imageSize, String sha256, byte[] imageData);
    Long createCaptureItem(FinanceAiCaptureBO.Item item);
    boolean updateCaptureItem(FinanceAiCaptureBO.Item item);
}
