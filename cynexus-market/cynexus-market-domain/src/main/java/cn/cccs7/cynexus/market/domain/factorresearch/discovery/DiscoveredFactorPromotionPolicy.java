package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

/** Versioned default gates that separate an interesting pattern from a screenable factor. */
public final class DiscoveredFactorPromotionPolicy {
    public Verdict evaluate(Evaluation value) {
        if (value == null || value.eventSamples() < 1 || value.controlSamples() < 1) return Verdict.UNAVAILABLE;
        if (value.eventSamples() < 60 || value.controlSamples() < 180 || value.eventCoverage() < .80d || value.controlCoverage() < .80d) {
            return Verdict.EXPLORATORY;
        }
        if (value.qValue() > .05d || value.trainEffect() <= 0 || value.validationEffect() <= 0
                || !value.monthlyStable() || !value.regimeStable()) return Verdict.WEAK;
        return Verdict.CANDIDATE;
    }

    public boolean mayPromote(Evaluation value) { return evaluate(value) == Verdict.CANDIDATE && value.outcomeStable(); }

    public record Evaluation(int eventSamples, int controlSamples, double eventCoverage, double controlCoverage,
                             double qValue, double trainEffect, double validationEffect, boolean monthlyStable,
                             boolean regimeStable, boolean outcomeStable) {
        public Evaluation {
            if (eventCoverage < 0 || eventCoverage > 1 || controlCoverage < 0 || controlCoverage > 1
                    || qValue < 0 || qValue > 1) throw new IllegalArgumentException("coverage and qValue must be normalized");
        }
    }
    public enum Verdict { EXPLORATORY, CANDIDATE, WEAK, REJECTED, UNAVAILABLE }
}
