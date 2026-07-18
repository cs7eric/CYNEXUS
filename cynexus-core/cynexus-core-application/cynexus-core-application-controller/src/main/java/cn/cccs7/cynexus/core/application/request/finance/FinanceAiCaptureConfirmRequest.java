package cn.cccs7.cynexus.core.application.request.finance;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
public class FinanceAiCaptureConfirmRequest {
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private String status;
        private String transactionType;
        private OffsetDateTime occurredAt;
        private BigDecimal amount;
        private String currency;
        private String merchant;
        private Long categoryId;
        private Long accountId;
        private String note;
    }
}
