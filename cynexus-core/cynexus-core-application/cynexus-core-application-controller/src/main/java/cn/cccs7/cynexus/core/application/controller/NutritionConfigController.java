package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.food.NutritionConfigDTO;
import cn.cccs7.cynexus.core.application.service.FoodIdentityResolver;
import cn.cccs7.cynexus.core.application.service.NutritionConfigAppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Owner-scoped nutrition settings used by the configuration center. */
@RestController
@RequestMapping("/api/core/food/nutrition-config")
public class NutritionConfigController {
    private final NutritionConfigAppService app;
    private final FoodIdentityResolver identity;

    public NutritionConfigController(NutritionConfigAppService app, FoodIdentityResolver identity) {
        this.app = app;
        this.identity = identity;
    }

    @GetMapping
    public Result<NutritionConfigDTO.Config> get(HttpServletRequest request) {
        return Result.success(app.get(identity.owner(request)));
    }

    @PutMapping
    public Result<NutritionConfigDTO.Config> save(HttpServletRequest request,
                                                   @RequestBody NutritionConfigDTO.SaveRequest body) {
        return Result.success(app.save(identity.owner(request), body));
    }

    @DeleteMapping
    public Result<NutritionConfigDTO.Config> delete(HttpServletRequest request) {
        return Result.success(app.delete(identity.owner(request)));
    }
}
