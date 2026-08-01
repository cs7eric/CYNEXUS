package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportAssetBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportAssetStorePort;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportAssetPO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioImportMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioImportAssetStoreAdapter implements PortfolioImportAssetStorePort {
    private final PortfolioImportMapper mapper;

    public PortfolioImportAssetStoreAdapter(PortfolioImportMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long save(PortfolioImportAssetBO asset) {
        PortfolioImportAssetPO po = new PortfolioImportAssetPO();
        po.setId(asset.id());
        po.setPrincipalKey(asset.principalKey());
        po.setTenantKey(asset.tenantKey());
        po.setFileName(asset.fileName());
        po.setContentType(asset.contentType());
        po.setByteSize(asset.byteSize());
        po.setContentSha256(asset.contentSha256());
        po.setContentBytes(asset.contentBytes());
        po.setRetentionUntil(asset.retentionUntil());
        mapper.insertAsset(po);
        return asset.id();
    }
}
