package cn.cccs7.cynexus.core.domain.food;

import java.util.List;
import cn.cccs7.cynexus.core.domain.food.FridgeModels.*;

public interface FridgeRepository {
    Inventory inventory(String owner);
    Item item(String owner, String itemId);
    MealStock mealStock(String owner, String itemId);
    void consumeMeal(String owner, MealSource source, String mealItemId, String idempotencyKey);
    void restoreMeal(String owner, String fridgeItemId, String mealItemId, String idempotencyKey);
    Inventory add(String owner, AddCommand command);
    Inventory adjust(String owner, String itemId, AdjustCommand command);
    Inventory confirmMapping(String owner, String itemId, String candidateId, String kind);
    MappingCandidate findConfirmedMapping(String owner, String name, String brand);
    List<MappingCandidate> searchMapping(String owner, String query, int limit);
    void lockOwner(String owner);
}
