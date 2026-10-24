package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.api.dto.food.FridgeDTO;
import cn.cccs7.cynexus.core.domain.food.FridgeDomainService;
import cn.cccs7.cynexus.core.domain.food.FridgeModels;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FridgeAppService {
    private final FridgeDomainService domain;
    public FridgeAppService(FridgeDomainService domain) { this.domain = domain; }

    public FridgeDTO.Inventory inventory(String owner) { return inventory(domain.inventory(owner)); }

    @Transactional
    public FridgeDTO.Inventory add(String owner, FridgeDTO.AddRequest request) {
        return inventory(domain.add(owner, new FridgeModels.AddCommand(request.id(), request.idempotencyKey(), request.name(), request.brand(), request.quantity(), request.unit(), request.bestBefore(), request.gramsPerUnit(), request.catalogFoodId(), request.customFoodId(), request.mappingId(), request.catalogKey())));
    }

    @Transactional
    public FridgeDTO.Inventory adjust(String owner, String itemId, FridgeDTO.AdjustRequest request) {
        return inventory(domain.adjust(owner, itemId, new FridgeModels.AdjustCommand(request.quantityDelta(), request.unit(), request.idempotencyKey(), request.source(), request.mealItemId())));
    }

    public FridgeDTO.MappingSearch searchMapping(String owner, String query, int limit) {
        return new FridgeDTO.MappingSearch(query, domain.searchMapping(owner, query, limit).stream().map(this::candidate).toList());
    }

    @Transactional
    public FridgeDTO.Inventory confirmMapping(String owner, String itemId, FridgeDTO.MappingRequest request) {
        if (request == null) throw new IllegalArgumentException("Food mapping request is required");
        return inventory(domain.confirmMapping(owner, itemId,
                new FridgeModels.MappingCommand(request.candidateId(), request.kind())));
    }

    public FridgeDTO.AiPreview previewAi(FridgeDTO.AiPreviewRequest request, String tenant, String principal) {
        FridgeModels.AiPreview preview = domain.previewAi(new FridgeDomainService.PreviewCommand(
                request == null ? null : request.input(), request == null ? null : request.inputMode(), tenant, principal));
        return new FridgeDTO.AiPreview(preview.provider(), preview.model(), preview.inputMode(),
                preview.items().stream().map(item -> new FridgeDTO.AiItem(item.name(), item.brand(), item.quantity(),
                        item.unit(), item.bestBefore(), item.confidence(), item.notes(), item.catalogKey(), item.assetKey(),
                        item.assetUrl(), item.assetMappingStatus(), item.assetMappingConfidence())).toList(), preview.warnings());
    }

    public FridgeDTO.NutritionMatchPreview previewNutritionMatches(String owner,
                                                                   FridgeDTO.NutritionMatchRequest request,
                                                                   String tenant, String principal) {
        List<FridgeModels.NutritionMatchInput> inputs = request == null || request.items() == null ? null
                : request.items().stream().map(item -> item == null ? null
                        : new FridgeModels.NutritionMatchInput(item.name(), item.brand())).toList();
        FridgeModels.NutritionMatchPreview preview = domain.previewNutritionMatches(owner, inputs, tenant, principal);
        return new FridgeDTO.NutritionMatchPreview(preview.provider(), preview.model(), preview.items().stream()
                .map(item -> new FridgeDTO.NutritionMatch(item.name(), item.brand(), item.status(), item.selectedId(),
                        item.confidence(), item.candidates().stream().map(candidate -> new FridgeDTO.NutritionMatchCandidate(
                                candidate.id(), candidate.kind(), candidate.name(), candidate.brand(), candidate.confidence(),
                                nutrients(candidate.nutrition()))).toList(), item.rankedIds(), item.requiresConfirmation()))
                .toList());
    }

    private FridgeDTO.Inventory inventory(FridgeModels.Inventory value) {
        return new FridgeDTO.Inventory(value.version(), value.isSample(), value.items().stream().map(this::item).toList(), value.operations().stream().map(this::operation).toList());
    }
    private FridgeDTO.Item item(FridgeModels.Item value) {
        return new FridgeDTO.Item(value.id(), value.catalogFoodId(), value.customFoodId(), value.name(), value.quantity(), value.unit(), value.bestBefore(), value.createdAt(), value.updatedAt(), value.mappingStatus(), value.mappingCandidates().stream().map(this::candidate).toList(), value.mappingConfidence(), value.grams(), value.gramsPerUnit(), nutrients(value.nutrition()), value.nutritionStatus(), value.stackCount(), value.overflowCount(), value.assetCatalogKey(), value.assetKey(), value.assetUrl());
    }
    private FridgeDTO.Operation operation(FridgeModels.Operation value) { return new FridgeDTO.Operation(value.id(), value.itemId(), value.itemName(), value.kind(), value.quantityDelta(), value.quantityAfter(), value.at()); }
    private FridgeDTO.MappingCandidate candidate(FridgeModels.MappingCandidate value) { return new FridgeDTO.MappingCandidate(value.id(), value.kind(), value.name(), value.brand(), value.confidence(), nutrients(value.nutrition())); }
    private FridgeDTO.Nutrients nutrients(FridgeModels.Nutrients value) { return value == null ? null : new FridgeDTO.Nutrients(value.caloriesKcal(), value.carbsG(), value.proteinG(), value.fatG()); }
}
