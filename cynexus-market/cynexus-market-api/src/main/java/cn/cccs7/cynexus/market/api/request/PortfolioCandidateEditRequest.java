package cn.cccs7.cynexus.market.api.request;

import lombok.Data;

@Data
public class PortfolioCandidateEditRequest {
    private Long version;
    private String symbol;
    private String securityName;
    private String side;
    private String tradedAt;
    private String quantity;
    private String price;
    private String totalFee;
    private String externalTradeNo;
}
