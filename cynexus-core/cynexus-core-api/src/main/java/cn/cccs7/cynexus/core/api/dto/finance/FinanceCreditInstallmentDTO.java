package cn.cccs7.cynexus.core.api.dto.finance;

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
public class FinanceCreditInstallmentDTO {
    private Long id;
    private Long planId;
    private Integer installmentNo;
    private LocalDate dueDate;
    private BigDecimal principalAmount;
    private BigDecimal feeAmount;
    private BigDecimal amount;
    private String status;
    private OffsetDateTime paidAt;
    private Long paymentTransactionId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Plan {
        private Long id;
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
        private List<FinanceCreditInstallmentDTO> installments;
    }
}
