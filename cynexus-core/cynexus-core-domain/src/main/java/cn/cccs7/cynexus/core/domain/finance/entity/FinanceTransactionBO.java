package cn.cccs7.cynexus.core.domain.finance.entity;

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
public class FinanceTransactionBO {
    private Long id;
    private String ownerKey;
    private String transactionType;
    private Long accountId;
    private String accountName;
    private String accountNature;
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
    private String sourceRef;
    private Boolean archived;
    private Integer attachmentCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
