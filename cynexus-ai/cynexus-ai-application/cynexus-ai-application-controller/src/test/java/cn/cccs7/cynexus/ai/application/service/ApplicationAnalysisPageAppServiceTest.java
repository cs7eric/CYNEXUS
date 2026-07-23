package cn.cccs7.cynexus.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisPageDTO;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisPageDomainService;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationAnalysisPageAppServiceTest {

    @Test
    void applicationServiceOnlyMapsTheDomainViewToTheApiContract() {
        ApplicationAnalysisPageDomainService domainService = mock(ApplicationAnalysisPageDomainService.class);
        ApplicationAnalysisPageBO.Config config = ApplicationAnalysisPageBO.Config.builder().horizonDays(30)
                .horizonSegments(2).monthlyKeep(3).weeklyKeep(4).build();
        ApplicationAnalysisPageBO.Target target = ApplicationAnalysisPageBO.Target.builder().targetKey("stock-600000")
                .targetType("stock").symbol("600000").name("Test").enabled(true).tagsJson("[]").build();
        when(domainService.getTargets()).thenReturn(new ApplicationAnalysisPageDomainService.TargetsView(
                config, List.of(new ApplicationAnalysisPageDomainService.TargetView(target, true))));

        ApplicationAnalysisPageDTO.TargetsResponse response = new ApplicationAnalysisPageAppService(domainService).getTargets();

        assertThat(response.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo("stock-600000");
            assertThat(item.getLastResultPath()).contains("application_analysis_result_current");
        });
        assertThat(response.getConfig().getHorizon().getDays()).isEqualTo(30);
        verify(domainService).getTargets();
    }
}
