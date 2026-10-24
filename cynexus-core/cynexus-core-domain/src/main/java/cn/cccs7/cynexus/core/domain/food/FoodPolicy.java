package cn.cccs7.cynexus.core.domain.food;

import cn.cccs7.cynexus.core.domain.food.FoodModels.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import cn.cccs7.cynexus.core.domain.common.RequestFingerprint;

public final class FoodPolicy {
    private FoodPolicy() {}
    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    public static String text(String value, int max, String field) {
        require(value != null && !value.isBlank() && value.length() <= max, "Invalid " + field);
        return value.trim();
    }
    public static String key(String key) { return text(key, 128, "idempotencyKey"); }
    public static String defaultMealType(int hour) {
        require(hour >= 0 && hour <= 23, "Invalid hour");
        if (hour < 5 || hour >= 22) return "snack";
        if (hour < 11) return "breakfast";
        if (hour < 15) return "lunch";
        if (hour < 18) return "snack";
        return "dinner";
    }
    public static void date(LocalDate date) { require(date != null && date.getYear() >= 1900 && date.getYear() <= 2200, "Invalid diary date"); }
    public static String timezone(String value) {
        String zone = value == null ? "Asia/Shanghai" : text(value, 64, "timezone");
        try { ZoneId.of(zone); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid timezone"); }
        return zone;
    }
    public static void nutrition(Nutrients value) {
        require(value != null && value.values().stream().anyMatch(v -> v != null), "At least one nutrient is required");
        for (BigDecimal v : value.values()) require(v == null || (v.signum() >= 0 && v.compareTo(new BigDecimal("10000000")) <= 0 && v.scale() <= 12), "Invalid nutrient value");
    }
    public static BigDecimal conversion(String unit, BigDecimal conversion) {
        text(unit, 32, "unit");
        BigDecimal expected = switch (unit) { case "g" -> BigDecimal.ONE; case "kg" -> new BigDecimal("1000"); case "mg" -> new BigDecimal("0.001"); default -> null; };
        if (expected != null) {
            require(conversion == null || conversion.compareTo(expected) == 0, "Mass unit conversion mismatch");
            return expected;
        }
        positive(conversion, "gramsPerUnit");
        return conversion;
    }
    public static void positive(BigDecimal v, String field) {
        require(v != null && v.signum() > 0 && v.compareTo(new BigDecimal("1000000")) <= 0 && v.scale() <= 12, "Invalid " + field);
    }
    public static void open(DayState state) { require(state != null && "open".equals(state.status()), "Reopen the completed day before editing"); }
    public static String fingerprint(Object... values) {
        return RequestFingerprint.of(values);
    }
    public static Item entry(EntryCommand command, Food food) {
        require(Set.of("breakfast", "lunch", "dinner", "snack").contains(command.mealType() == null ? "" : command.mealType()), "Invalid mealType");
        String kind = command.entryKind();
        require(Set.of("catalog", "custom", "quick").contains(kind == null ? "" : kind), "Invalid entryKind");
        if ("quick".equals(kind)) {
            require(command.foodId() == null, "Quick entries cannot reference a food");
            nutrition(command.nutrients());
            return new Item(null, command.mealType(), kind, null, text(command.foodName(), 200, "foodName"), null,
                    null, null, null, null, Nutrients.unknown(), command.nutrients(), null);
        }
        require(food != null, "Food not found");
        positive(command.quantity(), "quantity");
        String unit = text(command.unit(), 32, "unit");
        BigDecimal conversion = conversion(unit, command.gramsPerUnit());
        BigDecimal grams = command.quantity().multiply(conversion);
        return new Item(null, command.mealType(), kind, food.id(), food.name(), food.brand(), command.quantity(), unit,
                conversion, grams, food.nutrients(), food.nutrients().scale(grams), food.assetKey(), null, food.assetUrl());
    }
    public static Item edit(Item existing, EditCommand command) {
        require(existing != null, "Entry not found");
        String mealType = command.mealType() == null ? existing.mealType() : command.mealType();
        require(Set.of("breakfast", "lunch", "dinner", "snack").contains(mealType), "Invalid mealType");
        if (existing.sourceFridgeItemId() != null) {
            return new Item(existing.id(), mealType, existing.entryKind(), existing.foodId(), existing.foodName(),
                    existing.brand(), existing.quantity(), existing.unit(), existing.gramsPerUnit(),
                    existing.consumedGrams(), existing.per100g(), existing.nutrients(), existing.assetKey(),
                    existing.sourceFridgeItemId(), existing.assetUrl());
        }
        if ("quick".equals(existing.entryKind())) {
            nutrition(command.nutrients());
            return new Item(existing.id(), mealType, "quick", null, existing.foodName(), existing.brand(), null, null, null, null, existing.per100g(), command.nutrients(), existing.assetKey(), existing.sourceFridgeItemId(), existing.assetUrl());
        }
        positive(command.quantity(), "quantity");
        String unit = command.unit() == null ? existing.unit() : text(command.unit(), 32, "unit");
        BigDecimal conversion = conversion(unit, command.gramsPerUnit() == null && unit.equals(existing.unit()) ? existing.gramsPerUnit() : command.gramsPerUnit());
        BigDecimal grams = command.quantity().multiply(conversion);
        return new Item(existing.id(), mealType, existing.entryKind(), existing.foodId(), existing.foodName(), existing.brand(), command.quantity(), unit, conversion, grams, existing.per100g(), existing.per100g().scale(grams), existing.assetKey(), existing.sourceFridgeItemId(), existing.assetUrl());
    }
    public static Day summary(LocalDate date, DayState state, Goal goal, List<Item> items) {
        BigDecimal[] known = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        BigDecimal[] missing = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        for (Item item : items) for (int n = 0; n < 4; n++) {
            BigDecimal value = item.nutrients().values().get(n);
            if (value == null) missing[n] = missing[n].add(BigDecimal.ONE); else known[n] = known[n].add(value);
        }
        BigDecimal[] totals = known.clone();
        for (int n = 0; n < 4; n++) {
            if (missing[n].signum() > 0) totals[n] = null;
            if (!items.isEmpty() && missing[n].intValue() == items.size()) known[n] = null;
        }
        return new Day(date, state == null ? "open" : state.status(), state == null ? "Asia/Shanghai" : state.timezone(),
                state == null ? (goal == null ? Nutrients.unknown() : goal.nutrients()) : state.targets(),
                nutrients(totals), nutrients(known), nutrients(missing), List.copyOf(items));
    }
    private static Nutrients nutrients(BigDecimal[] values) { return new Nutrients(values[0], values[1], values[2], values[3]); }
}
