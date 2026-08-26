package cn.cccs7.cynexus.ai.domain.marketfactor;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketEventFactorAnalysisPolicyTest {
    private final MarketEventFactorAnalysisPolicy policy = new MarketEventFactorAnalysisPolicy();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void keepsOnlyReferencesAndEventCodesSuppliedByTheTrustedEvidence() {
        var analysis = policy.parseAndValidate("""
                {"factor_analysis":{"summary":"样本存在可继续验证的组合。","hypotheses":[
                  {"label":"有效事件","scope":"EVENT","event_code":"300017","evidence_refs":["market:msi_9.breadth","event:300017:daily.return10d","external:made_up"],"counter_evidence_refs":["market:unknown"],"research_conditions":["分层回测"]},
                  {"label":"无效事件","scope":"EVENT","event_code":"999999","evidence_refs":["market:msi_9.breadth"],"counter_evidence_refs":[],"research_conditions":[]}
                ],"caveats":["并非因果结论"]}}
                """, Set.of("market:msi_9.breadth", "event:300017:daily.return10d"), Set.of("300017"), mapper);

        assertThat(analysis.path("hypotheses")).hasSize(1);
        assertThat(analysis.path("hypotheses").get(0).path("evidence_refs"))
                .extracting(node -> node.asText()).containsExactly("market:msi_9.breadth", "event:300017:daily.return10d");
        assertThat(analysis.path("hypotheses").get(0).path("counter_evidence_refs")).isEmpty();
    }
}
