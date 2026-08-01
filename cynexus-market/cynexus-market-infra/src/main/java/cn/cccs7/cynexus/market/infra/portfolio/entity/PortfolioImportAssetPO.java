package cn.cccs7.cynexus.market.infra.portfolio.entity;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PortfolioImportAssetPO {
    private Long id;
    private String principalKey;
    private String tenantKey;
    private String fileName;
    private String contentType;
    private Long byteSize;
    private String contentSha256;
    private byte[] contentBytes;
    private OffsetDateTime retentionUntil;
}
