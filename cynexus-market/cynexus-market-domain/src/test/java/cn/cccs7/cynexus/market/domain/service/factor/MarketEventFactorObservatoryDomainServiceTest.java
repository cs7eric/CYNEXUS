package cn.cccs7.cynexus.market.domain.service.factor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cccs7.cynexus.market.domain.entity.factor.MarketEventFactorObservatoryBO;
import cn.cccs7.cynexus.market.domain.service.factor.impl.MarketEventFactorObservatoryDomainServiceImpl;
import org.junit.jupiter.api.Test;

class MarketEventFactorObservatoryDomainServiceTest {

    @Test
    void rejectsAnUnsupportedCohortBeforeReadingAnyDataSource() {
        MarketEventFactorObservatoryDomainService service = new MarketEventFactorObservatoryDomainServiceImpl(
                null, null, null, null, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.observe(
                new MarketEventFactorObservatoryBO.Query("2026-08-16", "UNKNOWN", null, 20, null)));
        assertEquals("cohort must be LIMIT_UP or LARGE_MOVE", exception.getMessage());
    }
}
