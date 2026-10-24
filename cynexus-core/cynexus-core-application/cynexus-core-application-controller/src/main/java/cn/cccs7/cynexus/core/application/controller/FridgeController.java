package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.food.FridgeDTO;
import cn.cccs7.cynexus.core.application.service.FoodIdentityResolver;
import cn.cccs7.cynexus.core.application.service.FridgeAppService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/food/fridge")
public class FridgeController {
    private final FridgeAppService app;
    private final FoodIdentityResolver identity;
    public FridgeController(FridgeAppService app, FoodIdentityResolver identity) { this.app = app; this.identity = identity; }

    @GetMapping("/inventory")
    public Result<FridgeDTO.Inventory> inventory(HttpServletRequest request) { return Result.success(app.inventory(identity.owner(request))); }

    @PostMapping("/items")
    public Result<FridgeDTO.Inventory> add(HttpServletRequest request, @RequestBody FridgeDTO.AddRequest body) { return Result.success(app.add(identity.owner(request), body)); }

    @PostMapping("/items/{id}/adjust")
    public Result<FridgeDTO.Inventory> adjust(HttpServletRequest request, @PathVariable String id, @RequestBody FridgeDTO.AdjustRequest body) { return Result.success(app.adjust(identity.owner(request), id, body)); }

    @PostMapping("/items/{id}/mapping")
    public Result<FridgeDTO.Inventory> confirmMapping(HttpServletRequest request, @PathVariable String id, @RequestBody FridgeDTO.MappingRequest body) { return Result.success(app.confirmMapping(identity.owner(request), id, body)); }

    @GetMapping("/mapping/search")
    public Result<FridgeDTO.MappingSearch> mapping(HttpServletRequest request, @RequestParam String q, @RequestParam(defaultValue = "10") int limit) { return Result.success(app.searchMapping(identity.owner(request), q, limit)); }

    @PostMapping("/ai/preview")
    public Result<FridgeDTO.AiPreview> previewAi(HttpServletRequest request, @RequestBody FridgeDTO.AiPreviewRequest body) {
        FoodIdentityResolver.Identity owner = identity.resolve(request);
        return Result.success(app.previewAi(body, owner.tenant(), owner.principal()));
    }

    @PostMapping("/ai/mapping-preview")
    public Result<FridgeDTO.NutritionMatchPreview> previewNutritionMapping(
            HttpServletRequest request, @RequestBody FridgeDTO.NutritionMatchRequest body) {
        FoodIdentityResolver.Identity owner = identity.resolve(request);
        return Result.success(app.previewNutritionMatches(owner.owner(), body, owner.tenant(), owner.principal()));
    }
}
