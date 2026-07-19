package cn.cccs7.cynexus.core.application.request.finance;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class FinanceCreditInstallmentPaymentRequest {
    private Long paymentAccountId;
    private OffsetDateTime occurredAt;
    private String note;
}
