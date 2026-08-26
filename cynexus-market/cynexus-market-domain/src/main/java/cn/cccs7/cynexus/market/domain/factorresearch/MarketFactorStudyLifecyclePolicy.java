package cn.cccs7.cynexus.market.domain.factorresearch;

import java.util.EnumSet;
import java.util.Map;

/** Governs the durable asynchronous Study state machine. */
public final class MarketFactorStudyLifecyclePolicy {

    private static final Map<MarketFactorResearchBO.StudyStatus, EnumSet<MarketFactorResearchBO.StudyStatus>> TRANSITIONS = Map.of(
            MarketFactorResearchBO.StudyStatus.QUEUED, EnumSet.of(MarketFactorResearchBO.StudyStatus.RUNNING,
                    MarketFactorResearchBO.StudyStatus.CANCELLED),
            MarketFactorResearchBO.StudyStatus.RUNNING, EnumSet.of(MarketFactorResearchBO.StudyStatus.SUCCEEDED,
                    MarketFactorResearchBO.StudyStatus.PARTIAL, MarketFactorResearchBO.StudyStatus.NO_DATA,
                    MarketFactorResearchBO.StudyStatus.FAILED, MarketFactorResearchBO.StudyStatus.CANCELLED)
    );

    public void requireTransition(MarketFactorResearchBO.StudyStatus current,
                                  MarketFactorResearchBO.StudyStatus target) {
        if (current == null || target == null || !TRANSITIONS.getOrDefault(current, EnumSet.noneOf(MarketFactorResearchBO.StudyStatus.class))
                .contains(target)) {
            throw new IllegalStateException("invalid market-factor study transition: " + current + " -> " + target);
        }
    }

    public void requireRetryable(MarketFactorResearchBO.StudyStatus status) {
        if (status != MarketFactorResearchBO.StudyStatus.FAILED
                && status != MarketFactorResearchBO.StudyStatus.CANCELLED
                && status != MarketFactorResearchBO.StudyStatus.PARTIAL
                && status != MarketFactorResearchBO.StudyStatus.NO_DATA) {
            throw new IllegalStateException("only terminal studies may be retried");
        }
    }

    public void requireLeakagePassed(boolean leakageCheckPassed) {
        if (!leakageCheckPassed) {
            throw new IllegalStateException("a study with a failed leakage check must be FAILED");
        }
    }
}
