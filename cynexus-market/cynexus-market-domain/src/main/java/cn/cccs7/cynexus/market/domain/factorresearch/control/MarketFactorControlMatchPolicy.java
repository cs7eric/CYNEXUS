package cn.cccs7.cynexus.market.domain.factorresearch.control;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Matches controls exclusively from frozen pre-event attributes.  Fallbacks
 * are explicit so a study cannot silently compare dissimilar securities.
 */
public final class MarketFactorControlMatchPolicy {
    public List<Match> match(Event event, List<Candidate> universe, int requiredControls) {
        if (event == null || universe == null || requiredControls < 1 || requiredControls > 5) {
            throw new IllegalArgumentException("event, universe and 1-5 requested controls are required");
        }
        List<Candidate> eligible = universe.stream().filter(candidate -> eligible(event, candidate)).toList();
        List<Match> matches = take(event, eligible, requiredControls, MatchQuality.EXACT, candidate ->
                same(candidate.industry(), event.industry()) && close(candidate.marketCapBucket(), event.marketCapBucket())
                        && close(candidate.liquidityBucket(), event.liquidityBucket()));
        if (matches.size() < requiredControls) add(matches, event, eligible, requiredControls, MatchQuality.RELAX_INDUSTRY,
                candidate -> close(candidate.marketCapBucket(), event.marketCapBucket()) && close(candidate.liquidityBucket(), event.liquidityBucket()));
        if (matches.size() < requiredControls) add(matches, event, eligible, requiredControls, MatchQuality.RELAX_MARKET_CAP,
                candidate -> close(candidate.liquidityBucket(), event.liquidityBucket()));
        if (matches.size() < requiredControls) add(matches, event, eligible, requiredControls, MatchQuality.RELAX_LIQUIDITY,
                candidate -> true);
        return List.copyOf(matches);
    }

    private List<Match> take(Event event, List<Candidate> candidates, int max, MatchQuality quality,
                             java.util.function.Predicate<Candidate> criterion) {
        List<Match> matches = new ArrayList<>();
        add(matches, event, candidates, max, quality, criterion);
        return matches;
    }

    private void add(List<Match> matches, Event event, List<Candidate> candidates, int max, MatchQuality quality,
                     java.util.function.Predicate<Candidate> criterion) {
        candidates.stream().filter(criterion).filter(candidate -> matches.stream().noneMatch(match -> match.candidate().code().equals(candidate.code())))
                .sorted(Comparator.comparingInt(candidate -> distance(event, candidate))).limit(max - matches.size())
                .forEach(candidate -> matches.add(new Match(candidate, quality, distance(event, candidate))));
    }

    private boolean eligible(Event event, Candidate candidate) {
        return candidate != null && !candidate.eventMember() && !candidate.st() && candidate.tradeDate().equals(event.tradeDate())
                && candidate.market().equals(event.market()) && !candidate.code().equals(event.code());
    }

    private int distance(Event event, Candidate candidate) {
        return Math.abs(event.marketCapBucket() - candidate.marketCapBucket())
                + Math.abs(event.liquidityBucket() - candidate.liquidityBucket())
                + (same(event.industry(), candidate.industry()) ? 0 : 4);
    }

    private boolean close(int value, int other) { return Math.abs(value - other) <= 1; }
    private boolean same(String value, String other) { return Objects.equals(value, other); }

    public enum MatchQuality { EXACT, RELAX_INDUSTRY, RELAX_MARKET_CAP, RELAX_LIQUIDITY }

    public record Event(String code, String market, LocalDate tradeDate, String industry, int marketCapBucket,
                        int liquidityBucket) {
        public Event {
            if (blank(code) || blank(market) || tradeDate == null || marketCapBucket < 0 || liquidityBucket < 0) {
                throw new IllegalArgumentException("event requires pre-event matching attributes");
            }
        }
    }

    public record Candidate(String code, String market, LocalDate tradeDate, String industry, int marketCapBucket,
                            int liquidityBucket, boolean st, boolean eventMember) {
        public Candidate {
            if (blank(code) || blank(market) || tradeDate == null || marketCapBucket < 0 || liquidityBucket < 0) {
                throw new IllegalArgumentException("candidate requires pre-event matching attributes");
            }
        }
    }

    public record Match(Candidate candidate, MatchQuality quality, int distance) { }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
