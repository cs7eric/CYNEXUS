package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;

/** Typed transport shape for factor-study synthesis. */
public record FactorStudySynthesisOutput(StudySynthesis study_synthesis) {
    public record StudySynthesis(String summary, List<Finding> findings, List<String> caveats) {
    }
    public record Finding(String label, List<String> evidence_refs,
                          List<String> counter_evidence_refs, String interpretation,
                          JsonNode metadata) {
    }
}
