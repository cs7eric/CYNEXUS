package cn.cccs7.cynexus.market.domain.factorresearch.control;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketFactorControlMatchPolicyTest {
    private final MarketFactorControlMatchPolicy policy = new MarketFactorControlMatchPolicy();
    private final LocalDate date = LocalDate.of(2026, 8, 14);

    @Test
    void usesPreEventComparableControlsBeforeRecordedFallbacks() {
        MarketFactorControlMatchPolicy.Event event = new MarketFactorControlMatchPolicy.Event("600000", "SH", date, "BANK", 3, 4);
        List<MarketFactorControlMatchPolicy.Match> matches = policy.match(event, List.of(
                candidate("600001", "BANK", 3, 4, false, false),
                candidate("600002", "BANK", 4, 3, false, false),
                candidate("600003", "REAL_ESTATE", 3, 4, false, false),
                candidate("600004", "BANK", 3, 4, true, false),
                candidate("600005", "BANK", 3, 4, false, true)), 3);
        assertEquals(3, matches.size());
        assertEquals(MarketFactorControlMatchPolicy.MatchQuality.EXACT, matches.getFirst().quality());
        assertEquals(MarketFactorControlMatchPolicy.MatchQuality.RELAX_INDUSTRY, matches.getLast().quality());
        assertTrue(matches.stream().noneMatch(match -> match.candidate().st() || match.candidate().eventMember()));
    }

    private MarketFactorControlMatchPolicy.Candidate candidate(String code, String industry, int cap, int liquidity, boolean st, boolean eventMember) {
        return new MarketFactorControlMatchPolicy.Candidate(code, "SH", date, industry, cap, liquidity, st, eventMember);
    }
}
