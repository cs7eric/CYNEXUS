package cn.cccs7.cynexus.core.domain.food;

import cn.cccs7.cynexus.core.domain.food.FoodModels.*;
import cn.cccs7.cynexus.core.domain.food.FridgeModels.MealSource;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import static cn.cccs7.cynexus.core.domain.food.FoodPolicy.*;

@Service
public class FoodDomainService {
    private static final int MAX_CUSTOM_ASSET_BYTES = 8 * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private final FoodRepository repository;
    public FoodDomainService(FoodRepository repository) { this.repository = repository; }

    public List<FoodAsset> assets() { return repository.assets(); }

    public List<Food> search(String owner, String query, int limit) {
        require(limit > 0 && limit <= 30, "limit must be between 1 and 30");
        if (query == null || query.isBlank()) return List.of();
        return repository.search(owner, text(query, 80, "query"), limit);
    }
    public Day day(String owner, LocalDate date) {
        date(date);
        DayState state = repository.day(owner, date);
        return summary(date, state, state == null ? repository.effectiveGoal(owner, date) : null,
                state == null ? List.of() : repository.items(owner, state.id()));
    }
    public List<Day> history(String owner, LocalDate start, LocalDate end) {
        date(start); date(end);
        require(!end.isBefore(start) && ChronoUnit.DAYS.between(start, end) < 366, "History range must be 1 to 366 days");
        List<Day> values = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            DayState state = repository.day(owner, date);
            if (state != null) values.add(summary(date, state, null, repository.items(owner, state.id())));
        }
        return values;
    }
    private DayState ensureDay(String owner, LocalDate date, String zone) {
        date(date);
        DayState state = repository.day(owner, date);
        return state == null ? repository.createDay(owner, date, timezone(zone), repository.effectiveGoal(owner, date)) : state;
    }
    public Day add(String owner, LocalDate date, EntryCommand command) {
        date(date); key(command.idempotencyKey()); timezone(command.timezone());
        repository.lockOwner(owner);
        String fingerprint = fingerprint(date, command);
        StoredRequest retry = repository.request(owner, "item", command.idempotencyKey());
        if (retry != null) { replay(retry, fingerprint); return day(owner, date); }
        DayState state = ensureDay(owner, date, command.timezone());
        open(state);
        require(repository.items(owner, state.id()).size() < 200, "Maximum 200 entries per day");
        Food food = "quick".equals(command.entryKind()) ? null : repository.food(owner, command.entryKind(), command.foodId());
        Item item = entry(command, food);
        String snapshot = food == null ? "{}" : food.sourceSnapshot();
        repository.insertItem(owner, state.id(), item, command.idempotencyKey(), fingerprint, snapshot);
        return day(owner, date);
    }

    public Day fridgeMealRetry(String owner, LocalDate date, String key, String zone, String mealType,
                               String fridgeItemId, java.math.BigDecimal quantity, String unit) {
        date(date); key(key); timezone(zone);
        repository.lockOwner(owner);
        String fingerprint = fridgeMealFingerprint(date, zone, mealType, fridgeItemId, quantity, unit);
        StoredRequest retry = repository.request(owner, "item", key);
        if (retry == null) return null;
        replay(retry, fingerprint);
        return day(owner, date);
    }

    public FridgeMealResult addFromFridge(String owner, LocalDate date, EntryCommand command, MealSource source) {
        date(date); key(command.idempotencyKey()); timezone(command.timezone());
        require(source != null, "Fridge meal source is required");
        repository.lockOwner(owner);
        String fingerprint = fridgeMealFingerprint(date, command.timezone(), command.mealType(),
                source.fridgeItemId(), source.requestedQuantity(), source.requestedUnit());
        StoredRequest retry = repository.request(owner, "item", command.idempotencyKey());
        if (retry != null) {
            replay(retry, fingerprint);
            return new FridgeMealResult(day(owner, date), retry.id(), false);
        }
        DayState state = ensureDay(owner, date, command.timezone());
        open(state);
        require(repository.items(owner, state.id()).size() < 200, "Maximum 200 entries per day");
        Food food = repository.food(owner, source.foodKind(), source.foodId());
        require(food != null, "Mapped Food record was not found");
        require(food.nutrients() != null && food.nutrients().values().stream().anyMatch(java.util.Objects::nonNull),
                "A nutrition value is required before logging this fridge item");
        Food snapshot = new Food(food.id(), food.kind(), source.name(), source.brand(), source.per100g(),
                food.source(), food.license(), food.usageScope(), food.isComplete(), food.favoriteId(),
                food.sourceSnapshot(), food.assetKey(), food.assetUrl());
        EntryCommand gramEntry = new EntryCommand(command.idempotencyKey(), command.timezone(), command.mealType(),
                command.entryKind(), command.foodId(), command.foodName(), source.consumedGrams(), "g", BigDecimal.ONE, null);
        Item item = entry(gramEntry, snapshot);
        var sourceSnapshotNode = new ObjectMapper().createObjectNode();
        sourceSnapshotNode.put("fridgeItemId", source.fridgeItemId());
        sourceSnapshotNode.put("source", "food_fridge_stock_lot");
        if (food.assetKey() != null) sourceSnapshotNode.put("assetKey", food.assetKey());
        if (food.assetUrl() != null) sourceSnapshotNode.put("assetUrl", food.assetUrl());
        String sourceSnapshot = sourceSnapshotNode.toString();
        repository.insertItem(owner, state.id(), item, command.idempotencyKey(), fingerprint, sourceSnapshot);
        StoredRequest stored = repository.request(owner, "item", command.idempotencyKey());
        require(stored != null && stored.id() != null, "The diary item could not be resolved");
        return new FridgeMealResult(day(owner, date), stored.id(), true);
    }
    public Day update(String owner, LocalDate date, String id, EditCommand command) {
        date(date); repository.lockOwner(owner);
        DayState state = repository.day(owner, date); open(state);
        Item existing = repository.item(owner, state.id(), id);
        if (existing != null && existing.sourceFridgeItemId() != null) {
            require(command.quantity() == null && command.unit() == null && command.gramsPerUnit() == null,
                    "Fridge-origin quantities must be changed from the fridge to keep stock and diary aligned");
        }
        Item item = edit(existing, command);
        repository.updateItem(owner, state.id(), item);
        return day(owner, date);
    }
    public Day updateFridge(String owner, LocalDate date, String id, EditCommand command, MealSource replacement) {
        date(date); repository.lockOwner(owner);
        DayState state = repository.day(owner, date); open(state);
        Item existing = repository.item(owner, state.id(), id);
        require(existing != null && existing.sourceFridgeItemId() != null,
                "A fridge-origin diary entry was not found");
        require(command != null && command.nutrients() == null,
                "Fridge meal nutrition comes from consumed stock");
        boolean hasQuantity = command.quantity() != null || command.unit() != null || command.gramsPerUnit() != null;
        if (hasQuantity) {
            require(command.quantity() != null && command.quantity().signum() > 0
                            && "g".equals(command.unit()) && command.gramsPerUnit() != null
                            && command.gramsPerUnit().compareTo(BigDecimal.ONE) == 0,
                    "Fridge meal quantities must be entered in grams");
            require(replacement != null || (existing.consumedGrams() != null
                            && command.quantity().compareTo(existing.consumedGrams()) == 0),
                    "Changing a fridge meal quantity requires a stock adjustment");
        }
        Item edited = edit(existing, command);
        if (replacement != null) {
            require(existing.sourceFridgeItemId().equals(replacement.fridgeItemId()),
                    "Replacement stock does not match this diary entry");
            require(replacement.consumedGrams() != null && replacement.consumedGrams().signum() > 0,
                    "Replacement meal quantity must be positive");
            require(command.quantity() != null && command.quantity().compareTo(replacement.consumedGrams()) == 0,
                    "Replacement stock quantity does not match the diary entry");
            edited = new Item(edited.id(), edited.mealType(), edited.entryKind(), edited.foodId(),
                    edited.foodName(), edited.brand(), replacement.consumedGrams(), "g", BigDecimal.ONE,
                    replacement.consumedGrams(), replacement.per100g(),
                    replacement.per100g().scale(replacement.consumedGrams()), edited.assetKey(),
                    existing.sourceFridgeItemId(), edited.assetUrl());
        }
        repository.updateItem(owner, state.id(), edited);
        return day(owner, date);
    }
    public Day delete(String owner, LocalDate date, String id) {
        date(date); repository.lockOwner(owner);
        DayState state = repository.day(owner, date); open(state);
        Item item = repository.item(owner, state.id(), id);
        require(item == null || item.sourceFridgeItemId() == null,
                "Fridge-origin entries cannot be deleted until the consumed stock can be restored");
        repository.deleteItem(owner, state.id(), id);
        return day(owner, date);
    }
    public Day deleteFridge(String owner, LocalDate date, String id) {
        date(date); repository.lockOwner(owner);
        DayState state = repository.day(owner, date); open(state);
        Item item = repository.item(owner, state.id(), id);
        require(item != null && item.sourceFridgeItemId() != null,
                "A fridge-origin diary entry was not found");
        repository.deleteItem(owner, state.id(), id);
        return day(owner, date);
    }
    public Day status(String owner, LocalDate date, boolean complete, String zone) {
        date(date); timezone(zone); repository.lockOwner(owner);
        DayState state = ensureDay(owner, date, zone);
        repository.status(owner, state.id(), complete ? "complete" : "open");
        return day(owner, date);
    }
    public List<Goal> goals(String owner) { return repository.goals(owner); }
    public List<Goal> saveGoal(String owner, LocalDate effectiveFrom, Nutrients nutrients, String key) {
        date(effectiveFrom); nutrition(nutrients); key(key); repository.lockOwner(owner);
        String fingerprint = fingerprint(effectiveFrom, nutrients);
        StoredRequest retry = repository.request(owner, "goal", key);
        if (retry != null) replay(retry, fingerprint);
        else {
            require(repository.goals(owner).size()<200, "Maximum 200 goal changes");
            require(repository.goals(owner).stream().noneMatch(g -> g.effectiveFrom().equals(effectiveFrom)), "A goal already exists for this date; choose another effective date");
            repository.saveGoal(owner, effectiveFrom, nutrients, key, fingerprint);
        }
        return goals(owner);
    }
    public List<Food> customFoods(String owner) { return repository.customFoods(owner); }
    public Food saveCustom(String owner, String name, String brand, Nutrients nutrients, String key, String assetKey,
                           String assetImageDataUrl) {
        name = text(name, 200, "name"); require(brand == null || brand.length() <= 200, "Invalid brand");
        nutrition(nutrients); key(key); repository.lockOwner(owner);
        String resolvedAssetKey = resolveAssetKey(assetKey);
        CustomAsset asset = decodeCustomAsset(assetImageDataUrl);
        String fingerprint;
        if (asset != null) fingerprint = fingerprint(name, brand, nutrients, resolvedAssetKey, asset.sha256());
        else if (resolvedAssetKey != null) fingerprint = fingerprint(name, brand, nutrients, resolvedAssetKey);
        else fingerprint = fingerprint(name, brand, nutrients);
        StoredRequest retry = repository.request(owner, "custom", key);
        String id;
        if (retry != null) { replay(retry, fingerprint); id=retry.id(); }
        else {
            require(repository.customFoods(owner).size()<200, "Maximum 200 custom foods");
            id=repository.saveCustom(owner, name, brand, nutrients, key, fingerprint, resolvedAssetKey,
                    asset == null ? null : asset.bytes(), asset == null ? null : asset.sha256());
        }
        return repository.food(owner,"custom",id);
    }
    public Food saveCustom(String owner, String name, String brand, Nutrients nutrients, String key, String assetKey) {
        return saveCustom(owner, name, brand, nutrients, key, assetKey, null);
    }
    public Food saveCustom(String owner, String name, String brand, Nutrients nutrients, String key) {
        return saveCustom(owner, name, brand, nutrients, key, null);
    }
    public byte[] customAsset(String owner, String id) {
        require(id != null && id.matches("[0-9]{1,40}"), "Invalid food id");
        return repository.customAsset(owner, id);
    }

    private CustomAsset decodeCustomAsset(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) return null;
        String value = dataUrl.trim();
        String prefix = "data:image/png;base64,";
        require(value.regionMatches(true, 0, prefix, 0, prefix.length()),
                "Food image must be a PNG data URL");
        String encoded = value.substring(prefix.length());
        require(!encoded.isBlank() && encoded.length() <= ((MAX_CUSTOM_ASSET_BYTES + 2) / 3) * 4 + 16,
                "Food image is too large");
        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Food image is not valid base64", e);
        }
        require(bytes.length > PNG_SIGNATURE.length && bytes.length <= MAX_CUSTOM_ASSET_BYTES,
                "Food image must be a PNG no larger than 8 MB");
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            require(bytes[i] == PNG_SIGNATURE[i], "Food image must be a PNG");
        }
        try {
            return new CustomAsset(bytes, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to fingerprint food image", e);
        }
    }

    private record CustomAsset(byte[] bytes, String sha256) {}
    private String resolveAssetKey(String key) {
        if (key == null || key.isBlank()) return null;
        String value = text(key, 80, "assetKey");
        require(value.matches("[A-Za-z0-9_-]{1,80}"), "Invalid assetKey");
        FoodAsset asset = repository.asset(value);
        require(asset != null, "Food artwork mapping was not found");
        return asset.catalogKey();
    }
    public List<Food> favorites(String owner) { return repository.favorites(owner); }
    public Food saveFavorite(String owner, String kind, String foodId, String key) {
        key(key); repository.lockOwner(owner);
        String fingerprint = fingerprint(kind, foodId);
        StoredRequest retry = repository.request(owner, "favorite", key);
        if (retry != null) replay(retry, fingerprint);
        else {
            require(repository.food(owner, kind, foodId) != null, "Food not found");
            require(repository.favorites(owner).size()<100, "Maximum 100 favorites");
            require(repository.favorites(owner).stream().noneMatch(f -> kind.equals(f.kind()) && foodId.equals(f.id())), "Food is already a favorite");
            repository.saveFavorite(owner, kind, foodId, key, fingerprint);
        }
        return repository.food(owner,kind,foodId);
    }
    public List<Food> deleteFavorite(String owner, String id) {
        repository.lockOwner(owner); repository.deleteFavorite(owner, id); return favorites(owner);
    }
    private void replay(StoredRequest request, String fingerprint) {
        require(request.fingerprint().equals(fingerprint), "Idempotency key was already used for a different request");
        require(!request.deleted(), "The original request was deleted; use a new idempotency key");
    }

    private static String fridgeMealFingerprint(LocalDate date, String zone, String mealType, String fridgeItemId,
                                                java.math.BigDecimal quantity, String unit) {
        return fingerprint(date, "fridge", zone, mealType, fridgeItemId, quantity, unit);
    }

    public record FridgeMealResult(Day day, String mealItemId, boolean created) {}
}
