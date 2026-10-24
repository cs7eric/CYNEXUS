package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.api.dto.food.FoodDTO;
import cn.cccs7.cynexus.core.domain.food.FoodDomainService;
import cn.cccs7.cynexus.core.domain.food.FoodModels;
import cn.cccs7.cynexus.core.domain.food.FoodPolicy;
import cn.cccs7.cynexus.core.domain.food.FridgeDomainService;
import cn.cccs7.cynexus.core.domain.food.FridgeModels;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodAppService {
    private final FoodDomainService domain;
    private final FridgeDomainService fridge;
    public FoodAppService(FoodDomainService domain, FridgeDomainService fridge) { this.domain=domain; this.fridge=fridge; }
    public List<FoodDTO.FoodAsset> assets() { return domain.assets().stream().map(this::asset).toList(); }
    public List<FoodDTO.Food> search(String owner,String q,int limit) { return domain.search(owner,q,limit).stream().map(this::food).toList(); }
    public FoodDTO.Day day(String owner,LocalDate date) { return day(domain.day(owner,date)); }
    public List<FoodDTO.Day> history(String owner,LocalDate start,LocalDate end) { return domain.history(owner,start,end).stream().map(this::day).toList(); }
    @Transactional
    public FoodDTO.Day add(String owner,LocalDate date,FoodDTO.EntryRequest r) {
        return day(domain.add(owner,date,new FoodModels.EntryCommand(r.idempotencyKey(),r.timezone(),r.mealType(),r.entryKind(),r.foodId(),r.foodName(),r.quantity(),r.unit(),r.gramsPerUnit(),nutrients(r.nutrients()))));
    }
    @Transactional
    public FoodDTO.Day addFromFridge(String owner, LocalDate date, FoodDTO.FridgeMealRequest r) {
        if (r == null) throw new IllegalArgumentException("Fridge meal request is required");
        if (r.idempotencyKey() == null || r.idempotencyKey().isBlank()) throw new IllegalArgumentException("idempotencyKey is required");
        String timezone = r.timezone() == null || r.timezone().isBlank() ? "Asia/Shanghai" : r.timezone();
        String mealType = r.mealType() == null || r.mealType().isBlank() ? defaultMealType(timezone) : r.mealType();
        FoodModels.Day retry = domain.fridgeMealRetry(owner, date, r.idempotencyKey(), timezone, mealType,
                r.fridgeItemId(), r.quantity(), r.unit());
        if (retry != null) return day(retry);
        FridgeModels.MealSource source = fridge.prepareMeal(owner, r.fridgeItemId(), r.quantity(), r.unit());
        FoodModels.EntryCommand entry = new FoodModels.EntryCommand(r.idempotencyKey(), timezone, mealType,
                source.foodKind(), source.foodId(), source.name(), source.requestedQuantity(), source.requestedUnit(), null, null);
        FoodDomainService.FridgeMealResult created = domain.addFromFridge(owner, date, entry, source);
        if (created.created()) fridge.consumeMeal(owner, source, created.mealItemId(), r.idempotencyKey());
        return day(created.day());
    }
    @Transactional
    public FoodDTO.Day update(String owner,LocalDate date,String id,FoodDTO.EditRequest r) {
        FoodModels.Item existing = domain.day(owner, date).items().stream()
                .filter(item -> item.id().equals(id)).findFirst().orElse(null);
        if (existing == null || existing.sourceFridgeItemId() == null) {
            return day(domain.update(owner,date,id,new FoodModels.EditCommand(r.quantity(),r.unit(),r.gramsPerUnit(),nutrients(r.nutrients()),r.mealType())));
        }
        boolean hasQuantityFields = r.quantity() != null || r.unit() != null || r.gramsPerUnit() != null;
        FridgeModels.MealSource replacement = null;
        BigDecimal quantityGrams = null;
        if (hasQuantityFields) {
            quantityGrams = fridge.mealQuantityGrams(r.quantity(), r.unit(), r.gramsPerUnit());
            if (existing.consumedGrams() == null || quantityGrams.compareTo(existing.consumedGrams()) != 0) {
                replacement = fridge.replaceMeal(owner, existing.sourceFridgeItemId(), id, quantityGrams, UUID.randomUUID().toString());
            }
        }
        FoodModels.EditCommand command = new FoodModels.EditCommand(quantityGrams,
                hasQuantityFields ? "g" : null, hasQuantityFields ? BigDecimal.ONE : null,
                nutrients(r.nutrients()), r.mealType());
        return day(domain.updateFridge(owner, date, id, command, replacement));
    }
    @Transactional
    public FoodDTO.Day delete(String owner,LocalDate date,String id) {
        FoodModels.Item existing = domain.day(owner, date).items().stream()
                .filter(item -> item.id().equals(id)).findFirst().orElse(null);
        if (existing == null || existing.sourceFridgeItemId() == null) return day(domain.delete(owner,date,id));
        fridge.restoreMeal(owner, existing.sourceFridgeItemId(), id, UUID.randomUUID().toString());
        return day(domain.deleteFridge(owner,date,id));
    }
    @Transactional
    public FoodDTO.Day status(String owner,LocalDate date,boolean complete,String timezone) { return day(domain.status(owner,date,complete,timezone)); }
    public List<FoodDTO.Goal> goals(String owner) { return domain.goals(owner).stream().map(this::goal).toList(); }
    @Transactional
    public List<FoodDTO.Goal> saveGoal(String owner,FoodDTO.GoalRequest r) { return domain.saveGoal(owner,r.effectiveFrom(),nutrients(r.nutrients()),r.idempotencyKey()).stream().map(this::goal).toList(); }
    public List<FoodDTO.Food> customFoods(String owner) { return domain.customFoods(owner).stream().map(this::food).toList(); }
    @Transactional
    public FoodDTO.Food saveCustom(String owner,FoodDTO.CustomRequest r) { return food(domain.saveCustom(owner,r.name(),r.brand(),nutrients(r.nutrients()),r.idempotencyKey(),r.assetKey(),r.assetImageDataUrl())); }
    public byte[] customAsset(String owner, String id) { return domain.customAsset(owner, id); }
    public List<FoodDTO.Food> favorites(String owner) { return domain.favorites(owner).stream().map(this::food).toList(); }
    @Transactional
    public FoodDTO.Food saveFavorite(String owner,FoodDTO.FavoriteRequest r) { return food(domain.saveFavorite(owner,r.kind(),r.foodId(),r.idempotencyKey())); }
    @Transactional
    public List<FoodDTO.Food> deleteFavorite(String owner,String id) { return domain.deleteFavorite(owner,id).stream().map(this::food).toList(); }
    private FoodModels.Nutrients nutrients(FoodDTO.Nutrients n) { return n==null?null:new FoodModels.Nutrients(n.caloriesKcal(),n.carbsG(),n.proteinG(),n.fatG()); }
    private FoodDTO.Nutrients nutrients(FoodModels.Nutrients n) { return n==null?null:new FoodDTO.Nutrients(n.caloriesKcal(),n.carbsG(),n.proteinG(),n.fatG()); }
    private FoodDTO.FoodAsset asset(FoodModels.FoodAsset a) { return new FoodDTO.FoodAsset(a.catalogKey(),a.catalogFoodId(),a.assetKey(),a.assetUrl(),a.sourceAtlas(),a.row(),a.column(),a.printedLabel()); }
    private FoodDTO.Food food(FoodModels.Food f) { return new FoodDTO.Food(f.id(),f.kind(),f.name(),f.brand(),nutrients(f.nutrients()),f.source(),f.license(),f.usageScope(),f.isComplete(),f.favoriteId(),assetKey(f.assetKey()),assetUrl(f.assetUrl())); }
    private FoodDTO.Item item(FoodModels.Item i) { return new FoodDTO.Item(i.id(),i.mealType(),i.entryKind(),i.foodId(),i.foodName(),i.brand(),i.quantity(),i.unit(),i.gramsPerUnit(),i.consumedGrams(),nutrients(i.per100g()),nutrients(i.nutrients()),assetKey(i.assetKey()),i.sourceFridgeItemId(),assetUrl(i.assetUrl())); }
    private FoodDTO.Day day(FoodModels.Day d) { return new FoodDTO.Day(d.date(),d.status(),d.timezone(),nutrients(d.targets()),nutrients(d.totals()),nutrients(d.knownTotals()),nutrients(d.missingCounts()),d.items().stream().map(this::item).toList()); }
    private FoodDTO.Goal goal(FoodModels.Goal g) { return new FoodDTO.Goal(g.id(),g.effectiveFrom(),nutrients(g.nutrients())); }
    private String assetKey(String key) { return key!=null && key.matches("[A-Za-z0-9_-]{1,80}")?key:null; }
    private String assetUrl(String url) { return url!=null && url.matches("^/(?:mobile/[A-Za-z0-9_./-]+|api/core/food/custom-foods/[0-9]+/asset)$") && !url.contains("..")?url:null; }
    private static String defaultMealType(String timezone) { java.time.ZoneId zone = java.time.ZoneId.of(timezone == null || timezone.isBlank() ? "Asia/Shanghai" : timezone); return FoodPolicy.defaultMealType(java.time.LocalTime.now(zone).getHour()); }
}
