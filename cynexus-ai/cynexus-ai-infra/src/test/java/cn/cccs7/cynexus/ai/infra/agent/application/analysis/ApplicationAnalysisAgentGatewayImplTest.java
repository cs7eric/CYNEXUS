package cn.cccs7.cynexus.ai.infra.agent.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisPageDomainService;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicationAnalysisAgentGatewayImplTest {

    @Test
    void mapsDomainResultToStableAgentPayload() {
        ApplicationAnalysisPageDomainService pageService = mock(ApplicationAnalysisPageDomainService.class);
        when(pageService.getResult("stock-TEST")).thenReturn(new ApplicationAnalysisPageDomainService.ResultView(
                "stock-TEST", "{\"symbol\":\"TEST\"}", "{\"score\":1}", "{}", List.of()));

        Map<String, Object> result = new ApplicationAnalysisAgentGatewayImpl(pageService, new ObjectMapper())
                .getResult("stock-TEST");

        assertThat(result).containsKeys("analysis_input", "analysis_result", "raw_result", "_meta_history");
        assertThat(result.get("analysis_input").toString()).contains("TEST");
    }

    @Test
    void mapsRecentSnapshotsWithoutDependingOnApplicationDtos() {
        ApplicationAnalysisPageDomainService pageService = mock(ApplicationAnalysisPageDomainService.class);
        ApplicationAnalysisPageBO.Target target = ApplicationAnalysisPageBO.Target.builder()
                .targetKey("stock-TEST").targetType("stock").symbol("TEST").enabled(true).build();
        ApplicationAnalysisPageBO.DailySnapshot snapshot = ApplicationAnalysisPageBO.DailySnapshot.builder()
                .tradeDate(LocalDate.of(2026, 9, 6)).updatedAt(java.time.OffsetDateTime.now())
                .payloadJson("{\"summary\":{\"ok\":true}}") .build();
        ApplicationAnalysisPageDomainService.SnapshotDetail detail =
                new ApplicationAnalysisPageDomainService.SnapshotDetail(target, snapshot, "{}", "{}", "{\"ok\":true}", List.of());
        when(pageService.listRecent30Full("stock-TEST", 1)).thenReturn(
                new ApplicationAnalysisPageDomainService.FullView(true, "stock-TEST",
                        List.of(new ApplicationAnalysisPageDomainService.FullItem("2026-09-06.json", "postgres://snapshot",
                                snapshot.getTradeDate(), 32, snapshot.getUpdatedAt(), detail)), null));

        Map<String, Object> result = new ApplicationAnalysisAgentGatewayImpl(pageService, new ObjectMapper())
                .getRecent30("stock-TEST", 1);

        assertThat(result.get("ok")).isEqualTo(true);
        assertThat((List<?>) result.get("items")).hasSize(1);
        assertThat(((Map<?, ?>) ((List<?>) result.get("items")).get(0)).get("filename"))
                .isEqualTo("2026-09-06.json");
    }
}
