package cn.cccs7.cynexus.core.domain.finance.port;

import java.time.OffsetDateTime;
import java.util.List;

public interface ReceiptAiParserPort {
    ParseResult parse(String ownerKey, List<ReceiptImage> images);

    record ReceiptImage(Integer imageIndex, String fileName, String contentType, byte[] content) {
    }

    record ParsedItem(Integer imageIndex, String transactionType, OffsetDateTime occurredAt,
                      java.math.BigDecimal amount, String currency, String merchant,
                      String categoryHint, String note, java.math.BigDecimal confidence,
                      String rawText) {
    }

    record ParseResult(String modelCode, List<ParsedItem> items, String rawResponse) {
    }
}
