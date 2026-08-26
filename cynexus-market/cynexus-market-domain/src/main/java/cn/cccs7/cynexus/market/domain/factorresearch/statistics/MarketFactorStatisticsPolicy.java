package cn.cccs7.cynexus.market.domain.factorresearch.statistics;

import java.util.List;

/** Deterministic, bounded statistics for a discovered boolean recipe. */
public final class MarketFactorStatisticsPolicy {
    public Comparison compare(BinaryEvidence event, BinaryEvidence control) {
        require(event, "event"); require(control, "control");
        double eventRate = rate(event.support(), event.available());
        double controlRate = rate(control.support(), control.available());
        double lift = controlRate == 0d ? (eventRate == 0d ? 1d : Double.POSITIVE_INFINITY) : eventRate / controlRate;
        double effect = eventRate - controlRate;
        ConfidenceInterval interval = wilsonDifference(event.support(), event.available(), control.support(), control.available());
        return new Comparison(event, control, eventRate, controlRate, lift, effect, interval,
                Math.max(0, event.available() - event.support()), Math.max(0, control.available() - control.support()));
    }

    public List<Double> benjaminiHochberg(List<Double> pValues) {
        if (pValues == null || pValues.isEmpty()) return List.of();
        if (pValues.stream().anyMatch(value -> value == null || value < 0 || value > 1)) {
            throw new IllegalArgumentException("p-values must be in [0, 1]");
        }
        int n = pValues.size();
        Integer[] order = java.util.stream.IntStream.range(0, n).boxed()
                .sorted(java.util.Comparator.comparing(pValues::get)).toArray(Integer[]::new);
        double[] adjusted = new double[n];
        double previous = 1d;
        for (int index = n - 1; index >= 0; index--) {
            int original = order[index];
            previous = Math.min(previous, pValues.get(original) * n / (index + 1d));
            adjusted[original] = Math.min(1d, previous);
        }
        return java.util.Arrays.stream(adjusted).boxed().toList();
    }

    private ConfidenceInterval wilsonDifference(int eventSupport, int eventAvailable, int controlSupport, int controlAvailable) {
        Interval event = wilson(eventSupport, eventAvailable);
        Interval control = wilson(controlSupport, controlAvailable);
        return new ConfidenceInterval(event.lower() - control.upper(), event.upper() - control.lower());
    }

    private Interval wilson(int support, int available) {
        double z = 1.959963984540054d;
        double proportion = rate(support, available);
        double denominator = 1 + z * z / available;
        double center = (proportion + z * z / (2 * available)) / denominator;
        double spread = z * Math.sqrt((proportion * (1 - proportion) + z * z / (4d * available)) / available) / denominator;
        return new Interval(Math.max(0, center - spread), Math.min(1, center + spread));
    }

    private double rate(int support, int available) { return (double) support / available; }
    private void require(BinaryEvidence evidence, String name) {
        if (evidence == null || evidence.available() < 1 || evidence.support() < 0 || evidence.support() > evidence.available()) {
            throw new IllegalArgumentException(name + " evidence is invalid");
        }
    }

    public record BinaryEvidence(int support, int available) { }
    public record ConfidenceInterval(double lower, double upper) { }
    public record Comparison(BinaryEvidence event, BinaryEvidence control, double eventSupportRate, double controlSupportRate,
                             double lift, double effect, ConfidenceInterval effectConfidenceInterval,
                             int eventCounterCount, int controlCounterCount) { }
    private record Interval(double lower, double upper) { }
}
