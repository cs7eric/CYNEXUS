package cn.cccs7.cynexus.market.domain.factorresearch.cohort;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;

/**
 * Resolves a versioned, point-in-time event population.  Providers deliberately
 * own event definitions; callers must never infer an event from a study type.
 */
public interface MarketEventCohortProvider {
    MarketFactorResearchBO.EventType eventType();

    int definitionVersion();

    MarketEventCohort resolve(MarketEventCohortQuery query);
}
