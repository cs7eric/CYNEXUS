package cn.cccs7.cynexus.market.domain.factorresearch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketFactorStudyLifecyclePolicyTest {
    private final MarketFactorStudyLifecyclePolicy policy = new MarketFactorStudyLifecyclePolicy();

    @Test
    void permitsOnlyDurableStudyTransitions() {
        assertDoesNotThrow(() -> policy.requireTransition(MarketFactorResearchBO.StudyStatus.QUEUED,
                MarketFactorResearchBO.StudyStatus.RUNNING));
        assertDoesNotThrow(() -> policy.requireTransition(MarketFactorResearchBO.StudyStatus.RUNNING,
                MarketFactorResearchBO.StudyStatus.SUCCEEDED));
        assertThrows(IllegalStateException.class, () -> policy.requireTransition(MarketFactorResearchBO.StudyStatus.SUCCEEDED,
                MarketFactorResearchBO.StudyStatus.RUNNING));
    }

    @Test
    void permitsRetryOnlyAfterATerminalRun() {
        assertDoesNotThrow(() -> policy.requireRetryable(MarketFactorResearchBO.StudyStatus.FAILED));
        assertThrows(IllegalStateException.class, () -> policy.requireRetryable(MarketFactorResearchBO.StudyStatus.RUNNING));
    }

    @Test
    void forbidsSuccessWhenLeakageHasFailed() {
        assertThrows(IllegalStateException.class, () -> policy.requireLeakagePassed(false));
    }
}
