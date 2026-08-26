package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import java.util.List;
import java.util.Map;

/**
 * Deterministic interpreter for the first-version discovered recipes.  It is
 * deliberately separate from the Screener's primitive criteria: callers must
 * first enforce the factor's PROMOTED/screenable lifecycle and provide only
 * point-in-time materialized sequences.
 */
public final class PromotedFactorRecipeEvaluator {

    public boolean matches(Map<String, Object> recipe, Map<String, Object> learnedBounds,
                           Map<String, List<Double>> primitives, Map<String, String> labels) {
        if (recipe == null || primitives == null) return false;
        String primitive = text(recipe.get("primitive"));
        List<Double> values = primitive == null ? null : primitives.get(primitive);
        if ("ABS_DELTA_GTE".equals(recipe.get("operator"))) return changePoint(recipe, learnedBounds, values);
        if (recipe.containsKey("shapeSignature")) return signature(values).equals(String.valueOf(recipe.get("shapeSignature")));
        if ("LOCAL_LOW_THEN_RECOVERY".equals(recipe.get("sequence"))) return localLowThenRecovery(values);
        if (recipe.containsKey("label") && recipe.containsKey("value")) {
            return labels != null && String.valueOf(recipe.get("value")).equals(labels.get(String.valueOf(recipe.get("label"))));
        }
        return false;
    }

    private boolean changePoint(Map<String, Object> recipe, Map<String, Object> bounds, List<Double> values) {
        if (values == null || bounds == null) return false;
        Integer offset = integer(recipe.get("changeOffset"));
        Double threshold = decimal(bounds.get("eventMeanAbsDelta"));
        if (offset == null || threshold == null || offset < 1 || offset >= values.size()) return false;
        Double current = values.get(offset); Double previous = values.get(offset - 1);
        return current != null && previous != null && Math.abs(current - previous) >= threshold;
    }

    private boolean localLowThenRecovery(List<Double> values) {
        if (values == null || values.size() < 3 || values.stream().anyMatch(value -> value == null)) return false;
        int lowest = 0;
        for (int index = 1; index < values.size() - 1; index++) if (values.get(index) < values.get(lowest)) lowest = index;
        return lowest > 0 && lowest < values.size() - 1 && values.getLast() > values.get(lowest);
    }

    private String signature(List<Double> values) {
        if (values == null || values.size() < 2 || values.stream().anyMatch(value -> value == null)) return "";
        StringBuilder result = new StringBuilder();
        for (int index = 1; index < values.size(); index++) result.append(values.get(index) > values.get(index - 1) ? 'U'
                : values.get(index) < values.get(index - 1) ? 'D' : 'F');
        return result.toString();
    }

    private String text(Object value) { return value instanceof String text && !text.isBlank() ? text : null; }
    private Integer integer(Object value) { return value instanceof Number valueNumber ? valueNumber.intValue() : null; }
    private Double decimal(Object value) { return value instanceof Number valueNumber ? valueNumber.doubleValue() : null; }
}
