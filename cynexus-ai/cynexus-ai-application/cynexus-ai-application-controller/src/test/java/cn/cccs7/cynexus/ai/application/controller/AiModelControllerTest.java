package cn.cccs7.cynexus.ai.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.api.dto.AiModelDTO;
import cn.cccs7.cynexus.ai.application.usecase.AiModelQueryUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiModelControllerTest {

    @Test
    void keepsTheExistingSuccessfulListResponseContract() {
        AiModelQueryUseCase queryUseCase = mock(AiModelQueryUseCase.class);
        List<AiModelDTO> models = List.of(AiModelDTO.builder().modelCode("gpt").enabled(true).build());
        when(queryUseCase.listModels()).thenReturn(models);

        var response = new AiModelController(queryUseCase).listModels();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsExactlyElementsOf(models);
    }
}
