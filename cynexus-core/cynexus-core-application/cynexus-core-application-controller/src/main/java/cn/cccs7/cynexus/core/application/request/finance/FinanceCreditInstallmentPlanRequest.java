package cn.cccs7.cynexus.core.application.request.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class FinanceCreditInstallmentPlanRequest {
    private Long accountId;
    private Long purchaseTransactionId;
    private String merchant;
    private BigDecimal totalAmount;
    private BigDecimal feeTotal;
    private Integer installmentCount;
    private LocalDate firstDueDate;
    private Integer intervalMonths;
    private String note;
}
