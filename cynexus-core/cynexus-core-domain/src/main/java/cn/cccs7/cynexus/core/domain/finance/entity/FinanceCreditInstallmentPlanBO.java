package cn.cccs7.cynexus.core.domain.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceCreditInstallmentPlanBO {
    private Long id;
    private String ownerKey;
    private Long accountId;
    private String accountName;
    private String accountType;
    private Long purchaseTransactionId;
    private String merchant;
    private BigDecimal totalAmount;
    private BigDecimal feeTotal;
    private Integer installmentCount;
    private LocalDate firstDueDate;
    private Integer intervalMonths;
    private String status;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<FinanceCreditInstallmentBO> installments;
}
