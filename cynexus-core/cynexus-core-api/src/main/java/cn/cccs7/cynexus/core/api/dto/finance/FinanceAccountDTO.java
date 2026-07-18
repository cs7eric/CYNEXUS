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
public class FinanceAccountDTO {
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
    private Boolean archived;
    private BigDecimal balance;
    private OffsetDateTime createdAt;
}
