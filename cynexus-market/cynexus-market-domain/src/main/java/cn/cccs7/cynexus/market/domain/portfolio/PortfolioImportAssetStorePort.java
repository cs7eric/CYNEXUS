package cn.cccs7.cynexus.market.domain.portfolio;

import java.util.Optional;

public interface PortfolioImportAssetStorePort {
    long save(PortfolioImportAssetBO asset);

    Optional<PortfolioImportAssetBO> findById(long assetId);
}
