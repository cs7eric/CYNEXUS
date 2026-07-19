package cn.cccs7.cynexus.core.domain.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceCreditInstallmentBO {
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
}
