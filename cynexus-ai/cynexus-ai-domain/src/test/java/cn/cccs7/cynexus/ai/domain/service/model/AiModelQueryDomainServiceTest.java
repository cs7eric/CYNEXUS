package cn.cccs7.cynexus.ai.domain.service.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.common.core.exception.BizException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiModelQueryDomainServiceTest {

    @Test
    void returnsOnlyEnabledModelsInRepositoryOrder() {
        AiModelBO disabled = AiModelBO.builder().modelCode("disabled").enabled(false).build();
        AiModelBO enabled = AiModelBO.builder().modelCode("enabled").enabled(true).build();

        List<AiModelBO> result = serviceReturning(List.of(disabled, enabled)).findEnabledModels();

        assertThat(result).containsExactly(enabled);
    }

    @Test
    void preservesCompatibleEmptyResultWhenNoModelIsEnabled() {
        assertThat(serviceReturning(List.of()).findEnabledModels()).isEmpty();
    }

    @Test
    void rejectsInvalidRepositoryResultWithDomainError() {
        assertThatThrownBy(() -> serviceReturning(null).findEnabledModels())
                .isInstanceOf(BizException.class)
                .hasMessage("Enabled AI model query returned no result");
    }

    private AiModelQueryDomainService serviceReturning(List<AiModelBO> models) {
        AiModelRepository repository = () -> models;
        return new AiModelQueryDomainServiceImpl(repository);
    }
}
