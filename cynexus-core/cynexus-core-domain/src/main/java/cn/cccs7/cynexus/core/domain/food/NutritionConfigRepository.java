package cn.cccs7.cynexus.core.domain.food;

import cn.cccs7.cynexus.core.domain.food.NutritionConfigModels.Config;

/** Persistence port for the owner-scoped nutrition configuration aggregate. */
public interface NutritionConfigRepository {
    void lockOwner(String owner);
    Config find(String owner);
    Config upsert(String owner, Config config);
    void delete(String owner);
}
