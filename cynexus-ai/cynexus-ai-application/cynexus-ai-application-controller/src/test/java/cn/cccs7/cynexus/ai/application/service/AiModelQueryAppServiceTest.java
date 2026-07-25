package cn.cccs7.cynexus.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.api.dto.AiModelDTO;
import cn.cccs7.cynexus.ai.application.assembler.AiModelAssembler;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.service.model.AiModelQueryDomainService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiModelQueryAppServiceTest {

    @Test
    void delegatesQueryToDomainAndConvertsTheResult() {
        AiModelQueryDomainService domainService = mock(AiModelQueryDomainService.class);
        AiModelAssembler assembler = mock(AiModelAssembler.class);
        AiModelBO model = AiModelBO.builder().modelCode("gpt").enabled(true).build();
        AiModelDTO dto = AiModelDTO.builder().modelCode("gpt").enabled(true).build();
        when(domainService.findEnabledModels()).thenReturn(List.of(model));
        when(assembler.toDTOList(List.of(model))).thenReturn(List.of(dto));

        List<AiModelDTO> result = new AiModelQueryAppService(domainService, assembler).listModels();

        assertThat(result).containsExactly(dto);
        verify(domainService).findEnabledModels();
        verify(assembler).toDTOList(List.of(model));
    }
}
