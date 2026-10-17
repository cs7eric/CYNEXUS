package cn.cccs7.cynexus.ai.infra.ai.langchain4j;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
public record UserReceiptOutput(List<ReceiptItem> items) {
    public record ReceiptItem(Integer imageIndex, String transactionType, String occurredAt,
                              BigDecimal amount, String currency, String merchant, Long accountId,
                              String accountHint, Long categoryId, String categoryHint, String note,
                              Integer installmentCount, BigDecimal installmentFee, Double confidence,
                              String rawText) { }
}
