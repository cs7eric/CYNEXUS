package cn.cccs7.cynexus.core.domain.food;

import java.util.List;
import cn.cccs7.cynexus.core.domain.food.FridgeModels.AssetMapping;

/** Read-only port for the database-owned fridge artwork catalog. */
public interface FridgeAssetMappingResolver {
    List<AssetMapping> assetMappings();
    AssetMapping assetMapping(String catalogKey);
}
