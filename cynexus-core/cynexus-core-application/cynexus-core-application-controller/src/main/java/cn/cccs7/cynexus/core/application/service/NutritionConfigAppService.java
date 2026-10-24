package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.api.dto.food.NutritionConfigDTO;
import cn.cccs7.cynexus.core.domain.food.NutritionConfigDomainService;
import cn.cccs7.cynexus.core.domain.food.NutritionConfigModels.Config;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maps the nutrition configuration API to the owner-scoped domain service. */
@Service
public class NutritionConfigAppService {
    private final NutritionConfigDomainService domain;

    public NutritionConfigAppService(NutritionConfigDomainService domain) {
        this.domain = domain;
    }

    public NutritionConfigDTO.Config get(String owner) {
        return map(domain.get(owner));
    }

    @Transactional
    public NutritionConfigDTO.Config save(String owner, NutritionConfigDTO.SaveRequest request) {
        if (request == null) throw new IllegalArgumentException("Nutrition configuration is required");
        return map(domain.save(owner, new Config(request.sex(), request.bodyFatPct(), request.ageYears(),
                request.activityFactor(), request.calorieDeficit(), request.carbsMultiplier(),
                request.proteinMultiplier(), request.fatMultiplier())));
    }

    @Transactional
    public NutritionConfigDTO.Config delete(String owner) {
        domain.delete(owner);
        return map(domain.get(owner));
    }

    private static NutritionConfigDTO.Config map(Config value) {
        return new NutritionConfigDTO.Config(value.sex(), value.bodyFatPct(), value.ageYears(),
                value.activityFactor(), value.calorieDeficit(), value.carbsMultiplier(),
                value.proteinMultiplier(), value.fatMultiplier());
    }
}
