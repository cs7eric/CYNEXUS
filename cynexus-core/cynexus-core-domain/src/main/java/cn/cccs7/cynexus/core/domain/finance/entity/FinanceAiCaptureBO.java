package cn.cccs7.cynexus.core.domain.finance.entity;

import java.math.BigDecimal;
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
public class FinanceAiCaptureBO {
    private Long id;
    private String ownerKey;
    private String status;
    private Integer sourceCount;
    private String modelCode;
    private String rawResponse;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long id;
        private Long captureId;
        private Long imageId;
        private Integer itemIndex;
        private String transactionType;
        private OffsetDateTime occurredAt;
        private BigDecimal amount;
        private String currency;
        private String merchant;
        private String categoryHint;
        private Long categoryId;
        private String categoryName;
        private Long accountId;
        private String accountName;
        private String note;
        private BigDecimal confidence;
        private String rawText;
        private String status;
        private Long transactionId;
    }
}
