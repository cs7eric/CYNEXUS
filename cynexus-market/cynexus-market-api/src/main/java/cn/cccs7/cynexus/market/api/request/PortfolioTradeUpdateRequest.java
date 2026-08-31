package cn.cccs7.cynexus.market.api.request;

import lombok.Data;

@Data
public class PortfolioTradeUpdateRequest {
    private String symbol;
    private String securityName;
    private String instrumentType;
    private String side;
    private String tradedAt;
    private String quantity;
    private String price;
    private String totalFee;
    private String strategy;
    private String tags;
    private String industryCode;
    private String industryName;
}
