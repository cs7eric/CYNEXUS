package cn.cccs7.cynexus.core.domain.finance.port;

import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCategoryBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentPlanBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceOverviewBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceTransactionBO;
import cn.cccs7.cynexus.core.domain.finance.port.ReceiptAiParserPort.ReceiptImage;
import java.time.LocalDate;
import java.util.List;

public interface FinanceRepository {
    List<FinanceAccountBO> listAccounts(String ownerKey, boolean includeArchived);
    FinanceAccountBO findAccount(String ownerKey, Long id);
    Long saveAccount(FinanceAccountBO account);
    boolean updateAccount(FinanceAccountBO account);
    boolean archiveAccount(String ownerKey, Long id);
    boolean restoreAccount(String ownerKey, Long id);

    List<FinanceCategoryBO> listCategories(String ownerKey, boolean includeArchived);
    Long saveCategory(FinanceCategoryBO category);
    boolean updateCategory(FinanceCategoryBO category);
    boolean archiveCategory(String ownerKey, Long id);

    Long saveTransaction(FinanceTransactionBO transaction);
    boolean updateTransaction(FinanceTransactionBO transaction);
    boolean archiveTransaction(String ownerKey, Long id);
    boolean deleteTransaction(String ownerKey, Long id);
    boolean deleteInstallmentPurchase(String ownerKey, Long purchaseTransactionId);
    FinanceCreditInstallmentBO findInstallmentByPaymentTransaction(String ownerKey, Long paymentTransactionId);
    boolean deleteInstallmentPayment(String ownerKey, Long paymentTransactionId);
    boolean hasInstallmentReference(String ownerKey, Long transactionId);
    FinanceTransactionBO findTransaction(String ownerKey, Long id);
    List<FinanceTransactionBO> listTransactions(String ownerKey, LocalDate start, LocalDate end, int limit);
    FinanceOverviewBO overview(String ownerKey, LocalDate start, LocalDate end);

    Long saveInstallmentPlan(FinanceCreditInstallmentPlanBO plan);
    Long saveInstallment(FinanceCreditInstallmentBO installment);
    FinanceCreditInstallmentPlanBO findInstallmentPlan(String ownerKey, Long id);
    FinanceCreditInstallmentPlanBO findInstallmentPlanByPurchaseTransaction(String ownerKey, Long transactionId);
    List<FinanceCreditInstallmentPlanBO> listInstallmentPlans(String ownerKey, Long accountId);
    boolean markInstallmentPaid(String ownerKey, Long planId, Integer installmentNo, Long paymentTransactionId);
    boolean updateInstallmentPlanStatus(String ownerKey, Long planId, String status);

    Long createCapture(FinanceAiCaptureBO capture);
    FinanceAiCaptureBO claimNextCapture();
    FinanceAiCaptureBO findCaptureByIdempotencyKey(String ownerKey, String idempotencyKey);
    List<FinanceAiCaptureBO> listCaptures(String ownerKey, int limit, int offset, String status);
    long countCaptures(String ownerKey, String status);
    boolean updateCapture(FinanceAiCaptureBO capture);
    FinanceAiCaptureBO findCapture(String ownerKey, Long captureId);
    boolean archiveCapture(String ownerKey, Long captureId);
    List<ReceiptImage> listCaptureImages(String ownerKey, Long captureId);
    boolean deleteCaptureItems(String ownerKey, Long captureId);
    Long createCaptureImage(Long captureId, Integer imageIndex, String fileName, String contentType,
                            long imageSize, String sha256, byte[] imageData);
    Long createCaptureItem(FinanceAiCaptureBO.Item item);
    boolean updateCaptureItem(FinanceAiCaptureBO.Item item);
}
