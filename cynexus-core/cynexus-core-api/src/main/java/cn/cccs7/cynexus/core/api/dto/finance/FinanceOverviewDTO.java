package cn.cccs7.cynexus.core.api.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceOverviewDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal net;
    private List<FinanceAccountDTO> accounts;
    private List<CategoryStat> categories;
    private List<DailyStat> daily;
    private List<FinanceTransactionDTO> recentTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private Long categoryId;
        private String categoryName;
        private String direction;
        private BigDecimal amount;
        private BigDecimal percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStat {
        private LocalDate date;
        private BigDecimal income;
        private BigDecimal expense;
    }
}
