package cn.cccs7.cynexus.core.api.dto.finance;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceTransactionDTO {
    private Long id;
    private String transactionType;
    private Long accountId;
    private String accountName;
    private Long toAccountId;
    private String toAccountName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime occurredAt;
    private String merchant;
    private String note;
    private String source;
    private Boolean archived;
    private Integer attachmentCount;
    private OffsetDateTime createdAt;
}
