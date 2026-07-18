package cn.cccs7.cynexus.core.application.request.finance;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class FinanceTransactionRequest {
    private Long id;
    private String transactionType;
    private Long accountId;
    private Long toAccountId;
    private Long categoryId;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime occurredAt;
    private String merchant;
    private String note;
}
