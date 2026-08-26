package cn.cccs7.cynexus.ai.domain.marketfactor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

public interface MarketFactorStudySynthesisDomainService {
    String CAPABILITY = "market_factor_study_synthesis";
    String PROMPT_KEY = CAPABILITY;
    Result synthesize(Command command);

    record Command(String studyId, String principal, String tenant, String signature) { }
    record Result(String studyId, String studyStatus, String dataCutoffAt, String leakageCheckStatus, List<String> warnings,
                  Map<String, String> evidenceRefs, ObjectNode synthesis, String providerCode, String modelCode, String promptVersion) { }
}
