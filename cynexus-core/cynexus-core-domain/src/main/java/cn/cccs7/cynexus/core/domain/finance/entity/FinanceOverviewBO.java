package cn.cccs7.cynexus.core.domain.finance.entity;

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
public class FinanceOverviewBO {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal net;
    private BigDecimal assetTotal;
    private BigDecimal liabilityTotal;
    private BigDecimal netAssets;
    private List<FinanceAccountBO> accounts;
    private List<CategoryStatBO> categories;
    private List<DailyStatBO> daily;
    private List<FinanceTransactionBO> recentTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStatBO {
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
    public static class DailyStatBO {
        private LocalDate date;
        private BigDecimal income;
        private BigDecimal expense;
        private Integer activityCount;
    }
}
