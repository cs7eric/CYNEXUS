package cn.cccs7.cynexus.market.domain.factorresearch.discovery;

import java.util.List;
import java.util.Map;

public record PatternDiscoveryInput(List<Series> events, List<Series> controls) {
    public PatternDiscoveryInput {
        events = events == null ? List.of() : List.copyOf(events);
        controls = controls == null ? List.of() : List.copyOf(controls);
    }

    public record Series(String sampleId, Map<String, List<Double>> primitives, Map<String, String> labels) {
        public Series {
            if (sampleId == null || sampleId.isBlank()) throw new IllegalArgumentException("series sampleId is required");
            primitives = primitives == null ? Map.of() : Map.copyOf(primitives);
            labels = labels == null ? Map.of() : Map.copyOf(labels);
        }
    }
}
