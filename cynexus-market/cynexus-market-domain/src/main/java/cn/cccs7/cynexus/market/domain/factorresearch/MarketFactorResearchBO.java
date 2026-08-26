package cn.cccs7.cynexus.market.domain.factorresearch;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Domain contract shared by deterministic study creation, persistence and API mapping. */
public final class MarketFactorResearchBO {

    private MarketFactorResearchBO() {
    }

    public enum EventType { LIMIT_UP_CLOSE, LIMIT_UP_TOUCH, LARGE_MOVE_CLOSE }
    public enum Stage { PRE_EVENT, EVENT_OPEN, EVENT_INTRADAY, EVENT_CLOSE, POST_EVENT }
    public enum StudyStatus { QUEUED, RUNNING, SUCCEEDED, PARTIAL, NO_DATA, FAILED, CANCELLED }
    public enum DiscoveryStatus { GENERATED, EXPLORATORY, VALIDATED, PROMOTED, WEAKENING, RETIRED }
    public enum DiscoveryScope { COMMON, SUBGROUP, RARE_SPECIAL, UNEXPLAINED }

    public record StudyConfig(EventType eventType, int cohortDefinitionVersion, LocalDate startDate, LocalDate endDate,
                              List<Integer> outcomeHorizons, int controlsPerEvent, int dailyPreWindowSessions,
                              int minutePreWindowMinutes, List<String> discoveryMethods, Map<String, Object> eventParams) {
        public StudyConfig {
            outcomeHorizons = outcomeHorizons == null ? List.of(1, 3, 5, 10, 20) : List.copyOf(outcomeHorizons);
            discoveryMethods = discoveryMethods == null ? List.of() : List.copyOf(discoveryMethods);
            eventParams = eventParams == null ? Map.of() : Map.copyOf(eventParams);
        }

        public StudyConfig(EventType eventType, int cohortDefinitionVersion, LocalDate startDate, LocalDate endDate,
                           List<Integer> outcomeHorizons, int controlsPerEvent, int dailyPreWindowSessions,
                           int minutePreWindowMinutes, List<String> discoveryMethods) {
            this(eventType, cohortDefinitionVersion, startDate, endDate, outcomeHorizons, controlsPerEvent,
                    dailyPreWindowSessions, minutePreWindowMinutes, discoveryMethods, Map.of());
        }
    }

    public record Study(String id, String tenantId, String ownerId, StudyConfig config, StudyStatus status,
                        OffsetDateTime dataCutoffAt, String featureRegistryVersion, boolean leakageCheckPassed,
                        int eventSampleCount, int controlSampleCount, List<String> warnings) {
        public Study {
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    public record StudyCreate(String tenantId, String ownerId, StudyConfig config, OffsetDateTime dataCutoffAt,
                              String featureRegistryVersion, String idempotencyKey) {
    }

    public record StudyProgress(int completedSteps, int totalSteps, String stage) {
    }

    public record ClaimedStudy(String id, String tenantId, String ownerId) {
    }

    /** Persisted, immutable study input. Feature values and outcomes are added only by the execution pipeline. */
    public record StudySample(String code, String market, LocalDate eventTradeDate, OffsetDateTime anchorAt,
                              EventType eventType, SampleType sampleType, Map<String, Object> matchingMetadata,
                              Map<String, Object> source, Map<String, Object> coverage) {
        public StudySample {
            matchingMetadata = matchingMetadata == null ? Map.of() : Map.copyOf(matchingMetadata);
            source = source == null ? Map.of() : Map.copyOf(source);
            coverage = coverage == null ? Map.of() : Map.copyOf(coverage);
        }
    }

    public enum SampleType { EVENT, CONTROL }

    public record SequencePoint(String code, String market, LocalDate eventTradeDate, SampleType sampleType,
                                String frequency, String sequenceCode, int offset, OffsetDateTime observedAt,
                                OffsetDateTime availableAt, java.math.BigDecimal rawValue, java.math.BigDecimal normalizedValue,
                                Map<String, Object> source) {
        public SequencePoint { source = source == null ? Map.of() : Map.copyOf(source); }
    }

    public record FactorResult(String discoveryCode, int recipeVersion, String method, DiscoveryScope scope,
                               Map<String, Object> recipe, Map<String, Object> learnedBounds,
                               Map<String, Object> statistics, Map<String, Object> validation,
                               Map<String, Object> evidence, String verdict, int rank) {
        public FactorResult {
            recipe = recipe == null ? Map.of() : Map.copyOf(recipe);
            learnedBounds = learnedBounds == null ? Map.of() : Map.copyOf(learnedBounds);
            statistics = statistics == null ? Map.of() : Map.copyOf(statistics);
            validation = validation == null ? Map.of() : Map.copyOf(validation);
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    public record PrimitiveFeatureDefinition(String code, int version, String label, String family, Stage stage,
                                             String valueType, String unit, String samplingFrequency,
                                             String normalizationPolicy, String asOfPolicy, String source) {
    }

    public record DiscoveredFactorDefinition(String discoveryCode, int recipeVersion, String name,
                                             DiscoveryScope scope, String discoveryMethod,
                                             Map<String, Object> recipe, Map<String, Object> learnedBounds,
                                             String sourceStudyId, String primitiveRegistryVersion,
                                             DiscoveryStatus validationStatus, boolean screenable) {
        public DiscoveredFactorDefinition {
            recipe = recipe == null ? Map.of() : Map.copyOf(recipe);
            learnedBounds = learnedBounds == null ? Map.of() : Map.copyOf(learnedBounds);
        }
    }
}
