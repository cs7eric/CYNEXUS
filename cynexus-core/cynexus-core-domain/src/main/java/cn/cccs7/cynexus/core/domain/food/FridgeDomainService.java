package cn.cccs7.cynexus.core.domain.food;

import cn.cccs7.cynexus.core.domain.food.FridgeModels.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Comparator;
import java.util.Collections;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FridgeDomainService {
    private static final List<String> UNITS = List.of("mg", "g", "kg", "ml", "L", "pcs", "pack", "head", "bunch", "bottle", "carton");
    private final FridgeRepository repository;
    private final FridgeInventoryAiClient aiClient;
    private final FridgeAssetMappingResolver assetResolver;

    public FridgeDomainService(FridgeRepository repository) {
        this(repository, (input, inputMode) -> { throw new IllegalStateException("Fridge AI preview is unavailable"); },
                new FridgeAssetMappingResolver() {
                    public List<AssetMapping> assetMappings() { return List.of(); }
                    public AssetMapping assetMapping(String catalogKey) { return null; }
                });
    }

    public FridgeDomainService(FridgeRepository repository, FridgeInventoryAiClient aiClient) {
        this(repository, aiClient, new FridgeAssetMappingResolver() {
            public List<AssetMapping> assetMappings() { return List.of(); }
            public AssetMapping assetMapping(String catalogKey) { return null; }
        });
    }

    @Autowired
    public FridgeDomainService(FridgeRepository repository, FridgeInventoryAiClient aiClient, FridgeAssetMappingResolver assetResolver) {
        this.repository = repository;
        this.aiClient = aiClient;
        this.assetResolver = assetResolver;
    }

    public Inventory inventory(String owner) { return repository.inventory(owner); }

    public Item item(String owner, String itemId) {
        require(itemId != null && !itemId.isBlank(), "fridgeItemId is required");
        Item item = repository.item(owner, itemId);
        require(item != null, "Fridge item was not found");
        return item;
    }

    public MealSource prepareMeal(String owner, String itemId, BigDecimal quantity, String unit) {
        require(itemId != null && !itemId.isBlank(), "fridgeItemId is required");
        require(quantity != null && quantity.signum() > 0 && quantity.compareTo(new BigDecimal("1000000")) <= 0,
                "quantity must be between 0 and 1000000");
        unit(unit);
        MealStock stock = repository.mealStock(owner, itemId);
        require(stock != null, "Fridge item was not found");
        require(stock.foodId() != null, "Confirm the Food match before logging this fridge item");
        require(!stock.lots().isEmpty(), "Fridge item has no available stock");
        boolean sameUnit = stock.lots().stream().allMatch(lot -> lot.unit().equals(stock.lots().get(0).unit()));
        boolean gramsMode = "g".equals(unit) && (!sameUnit || !"g".equals(stock.lots().get(0).unit()));
        require(gramsMode || (sameUnit && unit.equals(stock.lots().get(0).unit())),
                "Choose the displayed fridge unit for this item");

        BigDecimal remaining = quantity;
        BigDecimal consumedGrams = BigDecimal.ZERO;
        BigDecimal[] consumedNutrients = new BigDecimal[4];
        boolean[] completeNutrition = {true, true, true, true};
        List<MealAllocation> allocations = new ArrayList<>();
        for (MealLot lot : stock.lots()) {
            if (remaining.signum() <= 0) break;
            BigDecimal availableGrams = toGrams(lot.quantity(), lot.unit(), lot.gramsPerUnit(), lot.density());
            require(availableGrams != null && availableGrams.signum() > 0,
                    "This fridge item has no measured nutrition conversion");
            BigDecimal usedQuantity;
            BigDecimal usedGrams;
            if (gramsMode) {
                usedGrams = remaining.min(availableGrams);
                usedQuantity = fromGrams(usedGrams, lot.unit(), lot.gramsPerUnit(), lot.density());
                require(usedQuantity != null, "This fridge item has no measured nutrition conversion");
                if (usedQuantity.compareTo(lot.quantity()) > 0) {
                    usedQuantity = lot.quantity();
                    usedGrams = availableGrams;
                }
            } else {
                usedQuantity = remaining.min(lot.quantity());
                usedGrams = toGrams(usedQuantity, lot.unit(), lot.gramsPerUnit(), lot.density());
            }
            allocations.add(new MealAllocation(lot.id(), usedQuantity, lot.unit(), usedGrams));
            consumedGrams = consumedGrams.add(usedGrams);
            List<BigDecimal> values = lot.nutrients().values();
            for (int index = 0; index < 4; index++) {
                BigDecimal nutrient = values.get(index);
                if (nutrient == null) completeNutrition[index] = false;
                else consumedNutrients[index] = zero(consumedNutrients[index]).add(nutrient.multiply(usedGrams).movePointLeft(2));
            }
            remaining = remaining.subtract(gramsMode ? usedGrams : usedQuantity);
        }
        require(remaining.signum() <= 0, "Quantity exceeds current fridge stock");
        require(consumedGrams.signum() > 0, "Unable to calculate the consumed amount");
        BigDecimal[] per100 = new BigDecimal[4];
        for (int index = 0; index < 4; index++) {
            if (completeNutrition[index] && consumedNutrients[index] != null) {
                per100[index] = consumedNutrients[index].multiply(BigDecimal.valueOf(100))
                        .divide(consumedGrams, MathContext.DECIMAL128);
            }
        }
        FoodModels.Nutrients nutrition = new FoodModels.Nutrients(per100[0], per100[1], per100[2], per100[3]);
        return new MealSource(stock.fridgeItemId(), stock.foodId(), stock.foodKind(), stock.name(), stock.brand(),
                quantity, unit, consumedGrams, nutrition, List.copyOf(allocations));
    }

    public void consumeMeal(String owner, MealSource source, String mealItemId, String idempotencyKey) {
        require(source != null, "Fridge meal source is required");
        require(mealItemId != null && !mealItemId.isBlank(), "mealItemId is required");
        key(idempotencyKey);
        repository.consumeMeal(owner, source, mealItemId, idempotencyKey);
    }

    public MealSource replaceMeal(String owner, String itemId, String mealItemId,
                                  BigDecimal quantityGrams, String idempotencyKey) {
        require(mealItemId != null && !mealItemId.isBlank(), "mealItemId is required");
        require(quantityGrams != null && quantityGrams.signum() > 0
                && quantityGrams.compareTo(new BigDecimal("1000000")) <= 0,
                "quantity must be between 0 and 1000000 grams");
        key(idempotencyKey);
        repository.lockOwner(owner);
        repository.restoreMeal(owner, itemId, mealItemId, idempotencyKey + ":restore");
        MealSource replacement = prepareMeal(owner, itemId, quantityGrams, "g");
        consumeMeal(owner, replacement, mealItemId, idempotencyKey + ":consume");
        return replacement;
    }

    public BigDecimal mealQuantityGrams(BigDecimal quantity, String unit, BigDecimal gramsPerUnit) {
        require(quantity != null && quantity.signum() > 0
                && quantity.compareTo(new BigDecimal("1000000")) <= 0,
                "quantity must be between 0 and 1000000 grams");
        require("g".equals(unit) && gramsPerUnit != null && gramsPerUnit.compareTo(BigDecimal.ONE) == 0,
                "Fridge meal quantities must be entered in grams");
        return quantity;
    }

    public void restoreMeal(String owner, String itemId, String mealItemId, String idempotencyKey) {
        require(itemId != null && !itemId.isBlank(), "fridgeItemId is required");
        require(mealItemId != null && !mealItemId.isBlank(), "mealItemId is required");
        key(idempotencyKey);
        repository.lockOwner(owner);
        repository.restoreMeal(owner, itemId, mealItemId, idempotencyKey);
    }

    private static BigDecimal toGrams(BigDecimal quantity, String unit, BigDecimal gramsPerUnit, BigDecimal density) {
        return switch (unit) {
            case "mg" -> quantity.movePointLeft(3);
            case "g" -> quantity;
            case "kg" -> quantity.multiply(BigDecimal.valueOf(1000));
            case "ml" -> density == null ? null : quantity.multiply(density);
            case "L" -> density == null ? null : quantity.multiply(density).multiply(BigDecimal.valueOf(1000));
            default -> gramsPerUnit == null ? null : quantity.multiply(gramsPerUnit);
        };
    }

    private static BigDecimal fromGrams(BigDecimal grams, String unit, BigDecimal gramsPerUnit, BigDecimal density) {
        return switch (unit) {
            case "mg" -> grams.movePointRight(3);
            case "g" -> grams;
            case "kg" -> grams.movePointLeft(3);
            case "ml" -> density == null ? null : grams.divide(density, 12, RoundingMode.DOWN);
            case "L" -> density == null ? null : grams.divide(density.multiply(BigDecimal.valueOf(1000)), 12, RoundingMode.DOWN);
            default -> gramsPerUnit == null ? null : grams.divide(gramsPerUnit, 12, RoundingMode.DOWN);
        };
    }

    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    public Inventory add(String owner, AddCommand command) {
        require(command != null, "Add request is required");
        text(command.name(), 120, "name");
        require(command.brand() == null || command.brand().trim().length() <= 120, "Invalid brand");
        require(command.quantity() != null && command.quantity().signum() > 0 && command.quantity().compareTo(new BigDecimal("100000")) <= 0, "quantity must be between 0 and 100000");
        require(command.bestBefore() != null, "bestBefore is required");
        require(command.bestBefore().isAfter(LocalDate.now().minusDays(1)), "bestBefore must be today or later");
        unit(command.unit());
        require(!(command.unit().equals("ml") || command.unit().equals("L")) || command.gramsPerUnit() != null, "Volume units require an explicit density in g/ml");
        key(command.idempotencyKey());
        if (command.gramsPerUnit() != null) require(command.gramsPerUnit().signum() > 0, "gramsPerUnit must be positive");
        require(command.catalogFoodId() == null || command.customFoodId() == null, "Choose one Food mapping");
        String catalogKey = command.catalogKey();
        if (catalogKey == null || catalogKey.isBlank()) {
            AssetMappingDecision decision = resolveAsset(command.name());
            catalogKey = decision.mapping() == null ? null : decision.mapping().catalogKey();
        } else {
            require(assetResolver.assetMapping(catalogKey.trim()) != null, "Asset mapping was not found");
            catalogKey = catalogKey.trim();
        }
        repository.lockOwner(owner);
        MappingResolution mappingResolution = resolveNutritionMapping(owner, command);
        return repository.add(owner, new AddCommand(command.id(), command.idempotencyKey(), command.name(),
                command.brand(), command.quantity(), command.unit(), command.bestBefore(), command.gramsPerUnit(),
                command.catalogFoodId(), command.customFoodId(), command.mappingId(), catalogKey, mappingResolution));
    }

    private MappingResolution resolveNutritionMapping(String owner, AddCommand command) {
        if (command.mappingId() != null && !command.mappingId().isBlank()
                || command.catalogFoodId() != null || command.customFoodId() != null) return null;

        MappingCandidate confirmed = repository.findConfirmedMapping(owner, command.name(), command.brand());
        if (confirmed != null) {
            return new MappingResolution(
                    "catalog".equals(confirmed.kind()) ? confirmed.id() : null,
                    "custom".equals(confirmed.kind()) ? confirmed.id() : null,
                    "user_confirmed", BigDecimal.ONE, List.of());
        }

        List<MappingCandidate> candidates = repository.searchMapping(owner, command.name(), 20);
        // Only an unambiguous, repository-scored exact match may be applied
        // automatically. Fuzzy candidates stay pending for explicit review.
        List<MappingCandidate> exact = candidates.stream()
                .filter(candidate -> candidate.confidence() != null
                        && candidate.confidence().compareTo(BigDecimal.ONE) == 0)
                .toList();
        if (exact.size() == 1) {
            MappingCandidate candidate = exact.get(0);
            if ("catalog".equals(candidate.kind())) {
                return new MappingResolution(candidate.id(), null, "matched", BigDecimal.ONE, List.of());
            }
            if ("custom".equals(candidate.kind())) {
                return new MappingResolution(null, candidate.id(), "user_confirmed", BigDecimal.ONE, List.of());
            }
        }
        return new MappingResolution(null, null, candidates.isEmpty() ? "unmatched" : "pending",
                candidates.isEmpty() ? null : candidates.get(0).confidence(), candidates);
    }

    public Inventory adjust(String owner, String itemId, AdjustCommand command) {
        require(itemId != null && !itemId.isBlank(), "itemId is required");
        require(command != null && command.quantityDelta() != null && command.quantityDelta().signum() != 0, "quantityDelta is required");
        unit(command.unit());
        key(command.idempotencyKey());
        repository.lockOwner(owner);
        return repository.adjust(owner, itemId, command);
    }

    public List<MappingCandidate> searchMapping(String owner, String query, int limit) {
        text(query, 120, "query");
        require(limit > 0 && limit <= 20, "limit must be between 1 and 20");
        return repository.searchMapping(owner, query.trim(), limit);
    }

    public Inventory confirmMapping(String owner, String itemId, MappingCommand command) {
        require(itemId != null && !itemId.isBlank(), "itemId is required");
        require(command != null && command.candidateId() != null && !command.candidateId().isBlank(),
                "Food candidate is required");
        String kind = command.kind() == null ? "" : command.kind().trim().toLowerCase(Locale.ROOT);
        require(kind.equals("catalog") || kind.equals("custom"), "Food candidate kind must be catalog or custom");
        repository.lockOwner(owner);
        return repository.confirmMapping(owner, itemId, command.candidateId().trim(), kind);
    }

    /**
     * Converts an AI response into a bounded draft. This method never touches the
     * inventory repository; the caller must explicitly submit each reviewed item.
     */
    public AiPreview previewAi(PreviewCommand command) {
        require(command != null, "AI preview request is required");
        String input = text(command.input(), 2000, "input");
        String inputMode = command.inputMode() == null ? "text" : command.inputMode().trim().toLowerCase(Locale.ROOT);
        require(inputMode.equals("text") || inputMode.equals("voice"), "inputMode must be text or voice");
        FridgeInventoryAiClient.Completion completion = aiClient.preview(input, inputMode,
                command.tenant(), command.principal());
        return parsePreview(completion, inputMode);
    }

    /** Returns owner-scoped candidate suggestions only; inventory is never mutated here. */
    public NutritionMatchPreview previewNutritionMatches(String owner, List<NutritionMatchInput> inputs,
                                                          String tenant, String principal) {
        require(inputs != null && !inputs.isEmpty() && inputs.size() <= 20,
                "items must contain between 1 and 20 foods");
        List<List<MappingCandidate>> byIndex = new ArrayList<>();
        List<NutritionMatch> results = new ArrayList<>();
        List<Map<String, Object>> aiItems = new ArrayList<>();
        List<Integer> aiIndexes = new ArrayList<>();
        for (int index = 0; index < inputs.size(); index++) {
            NutritionMatchInput input = inputs.get(index);
            require(input != null, "Food item is required");
            String name = text(input.name(), 120, "name");
            String brand = clean(input.brand(), 120);
            List<MappingCandidate> candidates = searchNutritionCandidates(owner, name);
            byIndex.add(candidates);
            List<MappingCandidate> exact = candidates.stream()
                    .filter(candidate -> candidate.confidence() != null && candidate.confidence().compareTo(BigDecimal.ONE) == 0)
                    .toList();
            if (exact.size() == 1) {
                MappingCandidate selected = exact.get(0);
                results.add(new NutritionMatch(name, brand, "exact_match", selected.id(), BigDecimal.ONE,
                        nutritionCandidates(candidates), List.of(selected.id()), true));
            } else {
                results.add(null);
                if (!candidates.isEmpty()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("itemIndex", index);
                    row.put("name", name);
                    row.put("brand", brand);
                    row.put("candidates", candidates.stream().map(candidate -> {
                        Map<String, Object> value = new LinkedHashMap<>();
                        value.put("id", candidate.id()); value.put("kind", candidate.kind());
                        value.put("name", candidate.name()); value.put("brand", candidate.brand());
                        value.put("confidence", candidate.confidence());
                        value.put("nutrition", candidate.nutrition());
                        return value;
                    }).toList());
                    aiItems.add(row);
                    aiIndexes.add(index);
                }
            }
        }
        FridgeInventoryAiClient.Completion metadata = null;
        if (!aiItems.isEmpty()) {
            try {
                String context = new ObjectMapper().writeValueAsString(Map.of("items", aiItems));
                metadata = aiClient.matchNutrition(context, tenant, principal);
                applyNutritionMatches(metadata, aiIndexes, inputs, byIndex, results);
            } catch (IllegalArgumentException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("Nutrition candidate matching failed", exception);
            }
        }
        for (int index = 0; index < inputs.size(); index++) {
            if (results.get(index) == null) {
                NutritionMatchInput input = inputs.get(index);
                results.set(index, new NutritionMatch(text(input.name(), 120, "name"), clean(input.brand(), 120),
                        "unmatched", null, null, nutritionCandidates(byIndex.get(index)), List.of(), true));
            }
        }
        return new NutritionMatchPreview(metadata == null ? null : metadata.providerCode(),
                metadata == null ? null : metadata.modelCode(), List.copyOf(results));
    }

    private void applyNutritionMatches(FridgeInventoryAiClient.Completion completion, List<Integer> aiIndexes,
                                       List<NutritionMatchInput> inputs, List<List<MappingCandidate>> candidates,
                                       List<NutritionMatch> results) throws Exception {
        require(completion != null && completion.content() != null && !completion.content().isBlank(),
                "AI returned an empty nutrition match");
        JsonNode root = new ObjectMapper().readTree(stripCodeFence(completion.content()));
        JsonNode matches = root == null ? null : root.path("matches");
        require(matches != null && matches.isArray() && matches.size() <= 20,
                "AI nutrition match must contain a bounded matches array");
        Map<Integer, JsonNode> indexed = new LinkedHashMap<>();
        for (JsonNode match : matches) {
            if (!match.path("itemIndex").canConvertToInt()) continue;
            int index = match.path("itemIndex").asInt(-1);
            if (aiIndexes.contains(index)) indexed.putIfAbsent(index, match);
        }
        for (int index : aiIndexes) {
            NutritionMatchInput input = inputs.get(index);
            List<MappingCandidate> supplied = candidates.get(index);
            JsonNode node = indexed.get(index);
            if (node == null) continue;
            BigDecimal confidence = node.path("confidence").isNumber() ? node.path("confidence").decimalValue() : null;
            if (confidence != null && (confidence.signum() < 0 || confidence.compareTo(BigDecimal.ONE) > 0)) confidence = null;
            String selectedId = clean(node.path("selectedId").asText(null), 80);
            long selectedCount = 0;
            if (selectedId != null) for (MappingCandidate candidate : supplied) {
                if (selectedId.equals(candidate.id())) selectedCount++;
            }
            boolean validSelection = selectedCount == 1;
            if (!validSelection || confidence == null || confidence.compareTo(new BigDecimal("0.85")) < 0) selectedId = null;
            LinkedHashSet<String> ranked = new LinkedHashSet<>();
            JsonNode rankedNode = node.path("rankedIds");
            if (rankedNode.isArray()) for (JsonNode id : rankedNode) {
                String value = clean(id.asText(null), 80);
                if (value != null && supplied.stream().filter(candidate -> value.equals(candidate.id())).count() == 1) ranked.add(value);
                if (ranked.size() == 5) break;
            }
            if (selectedId != null) ranked.add(selectedId);
            results.set(index, new NutritionMatch(text(input.name(), 120, "name"), clean(input.brand(), 120),
                    selectedId == null ? "needs_review" : "ai_suggested", selectedId,
                    selectedId == null ? confidence : confidence, nutritionCandidates(supplied), List.copyOf(ranked), true));
        }
    }

    private static List<NutritionMatchCandidate> nutritionCandidates(List<MappingCandidate> candidates) {
        return candidates.stream().map(candidate -> new NutritionMatchCandidate(candidate.id(), candidate.kind(),
                candidate.name(), candidate.brand(), candidate.confidence(), candidate.nutrition())).toList();
    }

    private List<MappingCandidate> searchNutritionCandidates(String owner, String name) {
        LinkedHashMap<String, MappingCandidate> unique = new LinkedHashMap<>();
        addCandidates(unique, repository.searchMapping(owner, name, 20));
        AssetMappingDecision assetDecision = resolveAsset(name);
        if (assetDecision.mapping() != null) {
            for (String term : assetNames(assetDecision.mapping())) {
                if (unique.size() >= 20) break;
                addCandidates(unique, repository.searchMapping(owner, term, 20));
            }
        }
        return unique.values().stream().limit(20).toList();
    }

    private static void addCandidates(Map<String, MappingCandidate> target, List<MappingCandidate> candidates) {
        if (candidates == null) return;
        for (MappingCandidate candidate : candidates) {
            if (candidate != null && candidate.id() != null && candidate.kind() != null) {
                target.putIfAbsent(candidate.kind() + ":" + candidate.id(), candidate);
                if (target.size() >= 20) return;
            }
        }
    }

    public AssetMappingDecision resolveAsset(String name) {
        String query = clean(name, 120);
        if (query == null) return new AssetMappingDecision("unavailable", "none", null, null, List.of());
        String normalized = normalizeAssetName(query);
        List<AssetMapping> mappings = assetResolver.assetMappings();
        List<AssetMapping> exact = mappings.stream()
                .filter(mapping -> assetNames(mapping).stream().anyMatch(candidate -> normalizeAssetName(candidate).equals(normalized)))
                .toList();
        if (exact.size() == 1) {
            AssetMapping mapping = exact.get(0);
            String method = normalizeAssetName(mapping.canonicalName()).equals(normalized) ? "exact" : "alias";
            return new AssetMappingDecision("matched", method, BigDecimal.ONE, mapping, List.of(mapping));
        }
        if (exact.size() > 1) return new AssetMappingDecision("needs_mapping", "ambiguous", new BigDecimal("0.5"), null, exact);
        List<ScoredAsset> scored = mappings.stream()
                .map(mapping -> new ScoredAsset(mapping, assetNames(mapping).stream().mapToDouble(candidate -> assetScore(normalized, normalizeAssetName(candidate))).max().orElse(0)))
                .filter(value -> value.score() >= 0.78)
                .sorted(Comparator.comparingDouble(ScoredAsset::score).reversed().thenComparing(value -> value.mapping().catalogKey()))
                .limit(5)
                .toList();
        if (scored.isEmpty()) return new AssetMappingDecision("unavailable", "none", null, null, List.of());
        ScoredAsset top = scored.get(0);
        double margin = scored.size() == 1 ? 1 : top.score() - scored.get(1).score();
        if (top.score() >= 0.9 && margin >= 0.08) {
            return new AssetMappingDecision("matched", "fuzzy", decimal(top.score()), top.mapping(), scored.stream().map(ScoredAsset::mapping).toList());
        }
        return new AssetMappingDecision("needs_mapping", "fuzzy", decimal(top.score()), null, scored.stream().map(ScoredAsset::mapping).toList());
    }

    private AiPreview parsePreview(FridgeInventoryAiClient.Completion completion, String inputMode) {
        require(completion != null && completion.content() != null && !completion.content().isBlank(), "AI returned an empty preview");
        List<String> warnings = new ArrayList<>();
        List<AiItem> items = new ArrayList<>();
        try {
            JsonNode root = new ObjectMapper().readTree(stripCodeFence(completion.content()));
            JsonNode values = root != null && root.isObject() ? root.path("items") : root;
            if (values == null || !values.isArray()) throw new IllegalArgumentException("AI response must contain an items array");
            if (values.size() > 20) warnings.add("Only the first 20 items are shown for review.");
            int index = 0;
            for (JsonNode node : values) {
                if (index++ >= 20) break;
                String name = clean(node.path("name").asText(null), 120);
                if (name == null) { warnings.add("Skipped an item without a name."); continue; }
                BigDecimal quantity = node.path("quantity").isNumber() ? node.path("quantity").decimalValue() : null;
                if (quantity == null || quantity.signum() <= 0 || quantity.compareTo(new BigDecimal("100000")) > 0) {
                    quantity = null;
                    warnings.add(name + ": quantity needs review.");
                }
                String unit = normalizeUnit(node.path("unit").asText(null), warnings, name);
                LocalDate bestBefore = parseDate(node.path("bestBefore").asText(null), warnings, name);
                BigDecimal confidence = node.path("confidence").isNumber() ? node.path("confidence").decimalValue() : new BigDecimal("0.5");
                confidence = confidence.max(BigDecimal.ZERO).min(BigDecimal.ONE);
                List<String> notes = new ArrayList<>();
                JsonNode noteNode = node.path("notes");
                if (noteNode.isArray()) noteNode.forEach(value -> { String note = clean(value.asText(null), 240); if (note != null && notes.size() < 5) notes.add(note); });
                String singleNote = clean(node.path("note").asText(null), 240);
                if (singleNote != null && notes.size() < 5 && !notes.contains(singleNote)) notes.add(singleNote);
                AssetMappingDecision mapping = resolveAsset(name);
                AssetMapping asset = "matched".equals(mapping.status()) ? mapping.mapping() : null;
                items.add(new AiItem(name, clean(node.path("brand").asText(null), 120), quantity, unit, bestBefore, confidence, List.copyOf(notes),
                        asset == null ? null : asset.catalogKey(), asset == null ? null : asset.assetKey(),
                        asset == null ? null : asset.assetUrl(), mapping.status(), mapping.confidence()));
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI preview could not be parsed", exception);
        }
        if (items.isEmpty()) warnings.add("No reviewable food items were found.");
        return new AiPreview(
                completion.providerCode() == null || completion.providerCode().isBlank() ? "xtyoo" : completion.providerCode(),
                completion.modelCode() == null || completion.modelCode().isBlank() ? "gpt-6.1-sol" : completion.modelCode(),
                inputMode, List.copyOf(items), List.copyOf(warnings));
    }

    private static String normalizeUnit(String value, List<String> warnings, String name) {
        if (value == null || value.isBlank()) {
            warnings.add(name + ": unit needs review.");
            return null;
        }
        String unit = value.trim().toLowerCase(Locale.ROOT);
        String normalized = switch (unit) {
            case "piece", "pieces", "item", "items", "count" -> "pcs";
            case "liter", "litre", "liters", "litres" -> "L";
            case "milliliter", "millilitre", "milliliters", "millilitres" -> "ml";
            case "bottles" -> "bottle";
            case "cartons" -> "carton";
            case "packs" -> "pack";
            case "heads" -> "head";
            case "bunches" -> "bunch";
            case "mg", "g", "kg", "ml", "l", "pcs", "pack", "head", "bunch", "bottle", "carton" -> "l".equals(unit) ? "L" : unit;
            default -> null;
        };
        if (normalized == null || !UNITS.contains(normalized)) { warnings.add(name + ": unsupported unit needs review."); return null; }
        if (value == null || !normalized.equals(value.trim())) warnings.add(name + ": unit normalized to " + normalized + ".");
        return normalized;
    }

    private static LocalDate parseDate(String value, List<String> warnings, String name) {
        if (value == null || value.isBlank()) { warnings.add(name + ": best-before date needs review."); return null; }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            if (date.isBefore(LocalDate.now())) warnings.add(name + ": best-before date is in the past.");
            return date;
        } catch (RuntimeException exception) { warnings.add(name + ": invalid best-before date needs review."); return null; }
    }

    private static String clean(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }

    private static List<String> assetNames(AssetMapping mapping) {
        List<String> names = new ArrayList<>();
        if (mapping.canonicalName() != null) names.add(mapping.canonicalName());
        if (mapping.englishName() != null) names.add(mapping.englishName());
        if (mapping.aliases() != null) names.addAll(mapping.aliases());
        return names;
    }

    private static String normalizeAssetName(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[\\p{Punct}\\p{IsPunctuation}\\s_-]+", "");
    }

    private static double assetScore(String query, String candidate) {
        if (query.isEmpty() || candidate.isEmpty()) return 0;
        if (query.equals(candidate)) return 1;
        if (candidate.contains(query) || query.contains(candidate)) return 0.9;
        int distance = levenshtein(query, candidate);
        return 1.0 - (double) distance / Math.max(query.length(), candidate.length());
    }

    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) previous[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int[] current = new int[right.length() + 1];
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1));
            }
            previous = current;
        }
        return previous[right.length()];
    }

    private static BigDecimal decimal(double value) { return BigDecimal.valueOf(value).setScale(4, java.math.RoundingMode.HALF_UP); }
    private record ScoredAsset(AssetMapping mapping, double score) {}

    private static String stripCodeFence(String value) {
        String text = value.trim();
        if (text.startsWith("```")) text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        return text;
    }

    public record PreviewCommand(String input, String inputMode, String tenant, String principal) {
        public PreviewCommand(String input, String inputMode) { this(input, inputMode, null, null); }
    }

    private static void unit(String value) { require(value != null && UNITS.contains(value), "Unsupported food unit"); }
    private static void key(String value) { require(value != null && value.trim().length() >= 1 && value.trim().length() <= 128, "idempotencyKey is required"); }
    private static String text(String value, int max, String field) { require(value != null && !value.trim().isEmpty() && value.trim().length() <= max, "Invalid " + field); return value.trim(); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
