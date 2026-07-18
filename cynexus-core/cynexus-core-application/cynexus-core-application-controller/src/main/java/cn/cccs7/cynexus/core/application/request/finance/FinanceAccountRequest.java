package cn.cccs7.cynexus.core.application.request.finance;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinanceAccountRequest {
    private Long id;
    private String name;
    private String accountType;
    private String nature;
    private String currency;
    private BigDecimal openingBalance;
    private BigDecimal creditLimit;
    private Integer billingDay;
    private Integer dueDay;
    private String color;
    private String notes;
}
