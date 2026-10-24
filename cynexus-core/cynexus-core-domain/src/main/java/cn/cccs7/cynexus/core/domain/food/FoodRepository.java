package cn.cccs7.cynexus.core.domain.food;

import java.time.LocalDate;
import java.util.List;
import cn.cccs7.cynexus.core.domain.food.FoodModels.*;

public interface FoodRepository {
    void lockOwner(String owner);
    List<FoodAsset> assets();
    FoodAsset asset(String key);
    List<Food> search(String owner, String query, int limit);
    List<Food> customFoods(String owner);
    List<Food> favorites(String owner);
    Food food(String owner, String kind, String id);
    byte[] customAsset(String owner, String id);
    DayState day(String owner, LocalDate date);
    DayState createDay(String owner, LocalDate date, String timezone, Goal goal);
    List<Item> items(String owner, String dayId);
    Item item(String owner, String dayId, String id);
    void insertItem(String owner, String dayId, Item item, String key, String fingerprint, String sourceSnapshot);
    void updateItem(String owner, String dayId, Item item);
    void deleteItem(String owner, String dayId, String id);
    void status(String owner, String dayId, String status);
    StoredRequest request(String owner, String resource, String key);
    Goal effectiveGoal(String owner, LocalDate date);
    List<Goal> goals(String owner);
    String saveGoal(String owner, LocalDate date, Nutrients nutrients, String key, String fingerprint);
    default String saveCustom(String owner, String name, String brand, Nutrients nutrients, String key,
                              String fingerprint, String assetKey) {
        return saveCustom(owner, name, brand, nutrients, key, fingerprint, assetKey, null, null);
    }
    String saveCustom(String owner, String name, String brand, Nutrients nutrients, String key,
                      String fingerprint, String assetKey, byte[] assetImage, String assetDigest);
    String saveFavorite(String owner, String kind, String foodId, String key, String fingerprint);
    void deleteFavorite(String owner, String id);
}
