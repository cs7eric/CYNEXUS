package cn.cccs7.cynexus.market.domain.factorresearch;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates the persisted, typed Factor Set expression rather than accepting arbitrary JSON. */
public final class MarketFactorExpressionPolicy {
    private static final Set<String> COMBINERS = Set.of("AND", "OR");
    private static final Set<String> OPERATORS = Set.of("EQUALS", "BETWEEN", "GTE", "LTE", "IN", "RECIPE_MATCH");

    public void validate(Map<String, Object> expression) {
        if (expression == null || expression.isEmpty()) throw new IllegalArgumentException("expression is required");
        Number schemaVersion = number(expression.get("schemaVersion"));
        if (schemaVersion == null || schemaVersion.intValue() != 1) {
            throw new IllegalArgumentException("expression schemaVersion 1 is required");
        }
        String combiner = text(expression.get("operator"), "expression operator", 8);
        if (!COMBINERS.contains(combiner)) throw new IllegalArgumentException("expression operator is unsupported");
        Object rawClauses = expression.get("clauses");
        if (!(rawClauses instanceof List<?> clauses) || clauses.isEmpty() || clauses.size() > 20) {
            throw new IllegalArgumentException("expression requires 1..20 clauses");
        }
        for (Object rawClause : clauses) validateClause(rawClause);
    }

    @SuppressWarnings("unchecked")
    private void validateClause(Object rawClause) {
        if (!(rawClause instanceof Map<?, ?>)) throw new IllegalArgumentException("expression clause is invalid");
        Map<String, Object> clause = (Map<String, Object>) rawClause;
        text(clause.get("field"), "expression field", 64);
        String operator = text(clause.get("operator"), "clause operator", 16);
        if (!OPERATORS.contains(operator)) throw new IllegalArgumentException("clause operator is unsupported");
        if ("BETWEEN".equals(operator)) {
            if (number(clause.get("lower")) == null || number(clause.get("upper")) == null) {
                throw new IllegalArgumentException("BETWEEN clause requires lower and upper bounds");
            }
        } else if (clause.get("value") == null || String.valueOf(clause.get("value")).isBlank()) {
            throw new IllegalArgumentException("expression clause value is required");
        }
        Number weight = number(clause.get("weight"));
        if (weight != null && (new BigDecimal(weight.toString()).compareTo(BigDecimal.ZERO) <= 0
                || new BigDecimal(weight.toString()).compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("expression clause weight must be in (0,100]");
        }
    }

    private String text(Object value, String field, int maxLength) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isEmpty() || text.length() > maxLength) throw new IllegalArgumentException(field + " is required and must be at most " + maxLength + " characters");
        return text;
    }

    private Number number(Object value) {
        return value instanceof Number number ? number : null;
    }
}
