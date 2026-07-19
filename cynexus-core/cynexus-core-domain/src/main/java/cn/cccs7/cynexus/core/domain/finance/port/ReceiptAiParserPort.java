package cn.cccs7.cynexus.core.domain.finance.port;

import java.time.OffsetDateTime;
import java.util.List;

public interface ReceiptAiParserPort {
    ParseResult parse(String ownerKey, List<ReceiptImage> images, List<AccountCandidate> accounts,
                      List<CategoryCandidate> categories);

    record ReceiptImage(Integer imageIndex, String fileName, String contentType, byte[] content) {
    }

    record ParsedItem(Integer imageIndex, String transactionType, OffsetDateTime occurredAt,
                      java.math.BigDecimal amount, String currency, String merchant, Long accountId,
                      String accountHint, Long categoryId,
                      String categoryHint, String note, Integer installmentCount, java.math.BigDecimal installmentFee,
                      java.math.BigDecimal confidence,
                      String rawText) {
    }

    record AccountCandidate(Long id, String name, String accountType, String nature, String notes) {
    }

    record CategoryCandidate(Long id, String name, String direction) {
    }

    record ParseResult(String modelCode, List<ParsedItem> items, String rawResponse) {
    }
}
