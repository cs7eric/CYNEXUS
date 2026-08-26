package cn.cccs7.cynexus.core.domain.service.finance;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.core.common.constant.CoreConstants;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAccountBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceAiCaptureBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCategoryBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceCreditInstallmentPlanBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceOverviewBO;
import cn.cccs7.cynexus.core.domain.finance.entity.FinanceTransactionBO;
import cn.cccs7.cynexus.core.domain.finance.policy.FinanceInstallmentPolicy;
import cn.cccs7.cynexus.core.domain.finance.policy.FinanceLedgerDomainPolicy;
import cn.cccs7.cynexus.core.domain.finance.port.FinanceRepository;
import cn.cccs7.cynexus.core.domain.finance.port.ReceiptAiParserPort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinanceAiCaptureDomainServiceTest {

    private InMemoryFinanceRepository repository;
    private RecordingParser parser;
    private FinanceAiCaptureDomainService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryFinanceRepository();
        parser = new RecordingParser();
        FinanceLedgerDomainService ledger = new FinanceLedgerDomainService(repository, new FinanceLedgerDomainPolicy());
        service = new FinanceAiCaptureDomainService(repository, parser, ledger,
                new FinanceInstallmentDomainService(repository, ledger, new FinanceInstallmentPolicy()));
    }

    @Test
    void enqueueReturnsQueuedWithoutCallingParser() {
        FinanceAiCaptureBO capture = service.enqueue("default", "receipt.png", "image/png", new byte[] {1, 2, 3},
                3L, "auto", "key-1");

        assertThat(capture.getStatus()).isEqualTo(CoreConstants.STATUS_QUEUED);
        assertThat(capture.getId()).isNotNull();
        assertThat(parser.calls).isZero();
    }

    @Test
    void parseStillRunsRecognition() {
        FinanceAiCaptureBO capture = service.parse("default", "receipt.png", "image/png", new byte[] {1, 2, 3},
                3L, "review", "key-2");

        assertThat(parser.calls).isEqualTo(1);
        assertThat(capture.getStatus()).isEqualTo(CoreConstants.STATUS_REVIEW);
    }

    @Test
    void executeProcessesQueuedCapture() {
        FinanceAiCaptureBO queued = service.enqueue("default", "receipt.png", "image/png", new byte[] {1, 2, 3},
                3L, "review", "key-3");
        assertThat(parser.calls).isZero();

        FinanceAiCaptureBO claimed = service.claimNext();
        assertThat(claimed.getId()).isEqualTo(queued.getId());
        assertThat(claimed.getStatus()).isEqualTo(CoreConstants.STATUS_PROCESSING);

        service.execute(claimed);
        assertThat(parser.calls).isEqualTo(1);
        assertThat(service.get("default", queued.getId()).getStatus()).isEqualTo(CoreConstants.STATUS_REVIEW);
    }

    private static final class RecordingParser implements ReceiptAiParserPort {
        private int calls;

        @Override
        public ParseResult parse(String ownerKey, List<ReceiptImage> images,
                                 List<AccountCandidate> accounts, List<CategoryCandidate> categories) {
            calls += 1;
            return new ParseResult("test-model", List.of(new ParsedItem(
                    0, "expense", null, null, "CNY", "shop", null, null, null,
                    "hint", "note", null, null, null, "raw")), "{}");
        }
    }

    private static final class InMemoryFinanceRepository implements FinanceRepository {
        private final AtomicLong ids = new AtomicLong(1);
        private final Map<Long, FinanceAiCaptureBO> captures = new LinkedHashMap<>();
        private final Map<Long, List<ReceiptAiParserPort.ReceiptImage>> images = new LinkedHashMap<>();
        private final Map<Long, List<FinanceAiCaptureBO.Item>> items = new LinkedHashMap<>();

        @Override
        public Long createCapture(FinanceAiCaptureBO capture) {
            long id = ids.getAndIncrement();
            capture.setId(id);
            captures.put(id, copy(capture));
            items.put(id, new ArrayList<>());
            images.put(id, new ArrayList<>());
            return id;
        }

        @Override
        public FinanceAiCaptureBO claimNextCapture() {
            return captures.values().stream()
                    .filter(item -> CoreConstants.STATUS_QUEUED.equals(item.getStatus()))
                    .findFirst()
                    .map(item -> {
                        item.setStatus(CoreConstants.STATUS_PROCESSING);
                        return findCapture(item.getOwnerKey(), item.getId());
                    })
                    .orElse(null);
        }

        @Override
        public FinanceAiCaptureBO findCaptureByIdempotencyKey(String ownerKey, String idempotencyKey) {
            return captures.values().stream()
                    .filter(item -> ownerKey.equals(item.getOwnerKey()) && idempotencyKey.equals(item.getIdempotencyKey()))
                    .findFirst()
                    .map(item -> findCapture(ownerKey, item.getId()))
                    .orElse(null);
        }

        @Override
        public boolean updateCapture(FinanceAiCaptureBO capture) {
            FinanceAiCaptureBO current = captures.get(capture.getId());
            if (current == null) {
                return false;
            }
            if (capture.getStatus() != null) current.setStatus(capture.getStatus());
            if (capture.getProcessingMode() != null) current.setProcessingMode(capture.getProcessingMode());
            if (capture.getSourceCount() != null) current.setSourceCount(capture.getSourceCount());
            if (capture.getModelCode() != null) current.setModelCode(capture.getModelCode());
            if (capture.getRawResponse() != null) current.setRawResponse(capture.getRawResponse());
            current.setErrorMessage(capture.getErrorMessage());
            return true;
        }

        @Override
        public FinanceAiCaptureBO findCapture(String ownerKey, Long captureId) {
            FinanceAiCaptureBO current = captures.get(captureId);
            if (current == null || !ownerKey.equals(current.getOwnerKey())) {
                return null;
            }
            FinanceAiCaptureBO copy = copy(current);
            copy.setItems(new ArrayList<>(items.getOrDefault(captureId, List.of())));
            return copy;
        }

        @Override
        public Long createCaptureImage(Long captureId, Integer imageIndex, String fileName, String contentType,
                                       long imageSize, String sha256, byte[] imageData) {
            images.computeIfAbsent(captureId, key -> new ArrayList<>())
                    .add(new ReceiptAiParserPort.ReceiptImage(imageIndex, fileName, contentType, imageData));
            return ids.getAndIncrement();
        }

        @Override
        public List<ReceiptAiParserPort.ReceiptImage> listCaptureImages(String ownerKey, Long captureId) {
            return List.copyOf(images.getOrDefault(captureId, List.of()));
        }

        @Override
        public Long createCaptureItem(FinanceAiCaptureBO.Item item) {
            long id = ids.getAndIncrement();
            item.setId(id);
            items.computeIfAbsent(item.getCaptureId(), key -> new ArrayList<>()).add(item);
            return id;
        }

        @Override
        public boolean updateCaptureItem(FinanceAiCaptureBO.Item item) {
            return true;
        }

        @Override
        public List<FinanceAccountBO> listAccounts(String ownerKey, boolean includeArchived) {
            return List.of();
        }

        @Override
        public List<FinanceCategoryBO> listCategories(String ownerKey, boolean includeArchived) {
            return List.of();
        }

        @Override
        public Long saveTransaction(FinanceTransactionBO transaction) {
            return ids.getAndIncrement();
        }

        @Override
        public List<FinanceAiCaptureBO> listCaptures(String ownerKey, int limit, int offset, String status) {
            return captures.values().stream().limit(limit).toList();
        }

        @Override
        public long countCaptures(String ownerKey, String status) {
            return captures.size();
        }

        @Override
        public boolean archiveCapture(String ownerKey, Long captureId) {
            return false;
        }

        @Override
        public boolean deleteCaptureItems(String ownerKey, Long captureId) {
            items.put(captureId, new ArrayList<>());
            return true;
        }

        @Override public FinanceAccountBO findAccount(String ownerKey, Long id) { throw unused(); }
        @Override public Long saveAccount(FinanceAccountBO account) { throw unused(); }
        @Override public boolean updateAccount(FinanceAccountBO account) { throw unused(); }
        @Override public boolean archiveAccount(String ownerKey, Long id) { throw unused(); }
        @Override public boolean restoreAccount(String ownerKey, Long id) { throw unused(); }
        @Override public Long saveCategory(FinanceCategoryBO category) { throw unused(); }
        @Override public boolean updateCategory(FinanceCategoryBO category) { throw unused(); }
        @Override public boolean archiveCategory(String ownerKey, Long id) { throw unused(); }
        @Override public boolean updateTransaction(FinanceTransactionBO transaction) { throw unused(); }
        @Override public boolean archiveTransaction(String ownerKey, Long id) { throw unused(); }
        @Override public boolean deleteTransaction(String ownerKey, Long id) { throw unused(); }
        @Override public boolean deleteInstallmentPurchase(String ownerKey, Long purchaseTransactionId) { throw unused(); }
        @Override public FinanceCreditInstallmentBO findInstallmentByPaymentTransaction(String ownerKey, Long paymentTransactionId) { throw unused(); }
        @Override public boolean deleteInstallmentPayment(String ownerKey, Long paymentTransactionId) { throw unused(); }
        @Override public boolean hasInstallmentReference(String ownerKey, Long transactionId) { return false; }
        @Override public FinanceTransactionBO findTransaction(String ownerKey, Long id) { throw unused(); }
        @Override public List<FinanceTransactionBO> listTransactions(String ownerKey, LocalDate start, LocalDate end, int limit) { throw unused(); }
        @Override public FinanceOverviewBO overview(String ownerKey, LocalDate start, LocalDate end) { throw unused(); }
        @Override public Long saveInstallmentPlan(FinanceCreditInstallmentPlanBO plan) { throw unused(); }
        @Override public Long saveInstallment(FinanceCreditInstallmentBO installment) { throw unused(); }
        @Override public FinanceCreditInstallmentPlanBO findInstallmentPlan(String ownerKey, Long id) { throw unused(); }
        @Override public FinanceCreditInstallmentPlanBO findInstallmentPlanByPurchaseTransaction(String ownerKey, Long transactionId) { throw unused(); }
        @Override public List<FinanceCreditInstallmentPlanBO> listInstallmentPlans(String ownerKey, Long accountId) { throw unused(); }
        @Override public boolean markInstallmentPaid(String ownerKey, Long planId, Integer installmentNo, Long paymentTransactionId) { throw unused(); }
        @Override public boolean updateInstallmentPlanStatus(String ownerKey, Long planId, String status) { throw unused(); }

        private static FinanceAiCaptureBO copy(FinanceAiCaptureBO source) {
            return FinanceAiCaptureBO.builder()
                    .id(source.getId())
                    .ownerKey(source.getOwnerKey())
                    .status(source.getStatus())
                    .processingMode(source.getProcessingMode())
                    .sourceCount(source.getSourceCount())
                    .modelCode(source.getModelCode())
                    .rawResponse(source.getRawResponse())
                    .errorMessage(source.getErrorMessage())
                    .idempotencyKey(source.getIdempotencyKey())
                    .items(source.getItems() == null ? List.of() : new ArrayList<>(source.getItems()))
                    .build();
        }

        private static RuntimeException unused() {
            return new UnsupportedOperationException("unused in this test");
        }
    }
}
