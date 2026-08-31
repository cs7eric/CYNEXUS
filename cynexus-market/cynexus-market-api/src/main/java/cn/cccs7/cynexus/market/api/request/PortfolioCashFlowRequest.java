package cn.cccs7.cynexus.market.api.request; import java.math.BigDecimal; import java.time.OffsetDateTime; import lombok.Data;
@Data public class PortfolioCashFlowRequest { private OffsetDateTime occurredAt; private BigDecimal amount; private String flowType; private String note; }
