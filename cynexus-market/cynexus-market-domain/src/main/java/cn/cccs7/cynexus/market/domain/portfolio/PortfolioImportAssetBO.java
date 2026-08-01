package cn.cccs7.cynexus.market.domain.portfolio;

import java.time.OffsetDateTime;

public record PortfolioImportAssetBO(
        Long id,
        String principalKey,
        String tenantKey,
        String fileName,
        String contentType,
        long byteSize,
        String contentSha256,
        byte[] contentBytes,
        OffsetDateTime retentionUntil
) {
    public PortfolioImportAssetBO {
        if (id == null || principalKey == null || principalKey.isBlank() || tenantKey == null || tenantKey.isBlank()
                || contentType == null || contentType.isBlank() || byteSize <= 0 || contentSha256 == null
                || contentSha256.isBlank() || contentBytes == null || contentBytes.length != byteSize
                || retentionUntil == null) {
            throw new IllegalArgumentException("invalid portfolio import asset");
        }
        contentBytes = contentBytes.clone();
    }

    @Override
    public byte[] contentBytes() {
        return contentBytes.clone();
    }
}
