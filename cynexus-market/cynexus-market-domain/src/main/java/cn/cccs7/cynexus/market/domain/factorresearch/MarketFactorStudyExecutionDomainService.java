package cn.cccs7.cynexus.market.domain.factorresearch;

import cn.cccs7.cynexus.market.domain.factorresearch.cohort.MarketEventCohort;
import cn.cccs7.cynexus.market.domain.factorresearch.cohort.MarketEventCohortProviderRegistry;
import cn.cccs7.cynexus.market.domain.factorresearch.cohort.MarketEventCohortQuery;
import cn.cccs7.cynexus.market.domain.factorresearch.control.MarketFactorControlMatchPolicy;
import cn.cccs7.cynexus.market.domain.factorresearch.sequence.EventAlignedSequencePolicy;
import cn.cccs7.cynexus.market.domain.factorresearch.discovery.PatternDiscoveryCandidate;
import cn.cccs7.cynexus.market.domain.factorresearch.discovery.PatternDiscoveryEngine;
import cn.cccs7.cynexus.market.domain.factorresearch.discovery.PatternDiscoveryInput;
import cn.cccs7.cynexus.market.domain.factorresearch.statistics.MarketFactorStatisticsPolicy;
import cn.cccs7.cynexus.market.domain.factorresearch.outcome.MarketFactorOutcomeEvaluator;
import cn.cccs7.cynexus.market.domain.factorresearch.discovery.DiscoveredFactorPromotionPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Claims one durable study and freezes its event population. Later stages add
 * controls, sequences and discoveries to the same immutable study input.
 */
public final class MarketFactorStudyExecutionDomainService {
    private final MarketFactorStudyRepository repository;
    private final MarketEventCohortProviderRegistry cohortProviders;
    private final MarketFactorStudyLifecyclePolicy lifecyclePolicy;
    private final MarketFactorStudySnapshotRepository snapshots;
    private final MarketFactorControlMatchPolicy controlPolicy;
    private final PatternDiscoveryEngine discoveryEngine;

    public MarketFactorStudyExecutionDomainService(MarketFactorStudyRepository repository,
                                                    MarketEventCohortProviderRegistry cohortProviders,
                                                    MarketFactorStudyLifecyclePolicy lifecyclePolicy) {
        this.repository = repository; this.cohortProviders = cohortProviders; this.lifecyclePolicy = lifecyclePolicy;
        this.snapshots = null; this.controlPolicy = null;
        this.discoveryEngine = null;
    }

    public MarketFactorStudyExecutionDomainService(MarketFactorStudyRepository repository,
                                                    MarketEventCohortProviderRegistry cohortProviders,
                                                    MarketFactorStudyLifecyclePolicy lifecyclePolicy,
                                                    MarketFactorStudySnapshotRepository snapshots,
                                                    MarketFactorControlMatchPolicy controlPolicy) {
        this(repository, cohortProviders, lifecyclePolicy, snapshots, controlPolicy, null);
    }

    public MarketFactorStudyExecutionDomainService(MarketFactorStudyRepository repository,
                                                    MarketEventCohortProviderRegistry cohortProviders,
                                                    MarketFactorStudyLifecyclePolicy lifecyclePolicy,
                                                    MarketFactorStudySnapshotRepository snapshots,
                                                    MarketFactorControlMatchPolicy controlPolicy,
                                                    PatternDiscoveryEngine discoveryEngine) {
        this.repository = repository; this.cohortProviders = cohortProviders; this.lifecyclePolicy = lifecyclePolicy;
        this.snapshots = snapshots; this.controlPolicy = controlPolicy;
        this.discoveryEngine = discoveryEngine;
    }

    public boolean processNextQueuedStudy() {
        Optional<MarketFactorResearchBO.ClaimedStudy> claim = repository.claimNextQueued();
        if (claim.isEmpty()) return false;
        MarketFactorResearchBO.ClaimedStudy claimed = claim.get();
        MarketFactorResearchBO.Study study = repository.findById(claimed.tenantId(), claimed.ownerId(), claimed.id())
                .orElseThrow(() -> new IllegalStateException("claimed market-factor study was not found"));
        try {
            progress(study, 0, 5, "COHORT");
            MarketEventCohort cohort = cohortProviders.require(study.config().eventType(), study.config().cohortDefinitionVersion())
                    .resolve(new MarketEventCohortQuery(study.config().eventType(), study.config().cohortDefinitionVersion(),
                            study.config().startDate(), study.config().endDate(), 20_000, study.config().eventParams()));
            List<MarketFactorResearchBO.StudySample> samples = eventSamples(study, cohort.samples());
            if (!running(study)) return true;
            progress(study, 1, 5, "CONTROLS", samples.size(), 0);
            List<MarketFactorResearchBO.StudySample> controls = controls(study, samples);
            if (!running(study)) return true;
            progress(study, 1, 5, "CONTROLS", samples.size(), controls.size());
            repository.saveSamples(study.tenantId(), study.ownerId(), study.id(), samples);
            repository.saveSamples(study.tenantId(), study.ownerId(), study.id(), controls);
            progress(study, 2, 5, "SEQUENCES");
            List<MarketFactorResearchBO.SequencePoint> sequencePoints = sequencePoints(study, samples, controls);
            repository.saveSequencePoints(study.tenantId(), study.ownerId(), study.id(), sequencePoints);
            if (!running(study)) return true;
            progress(study, 3, 5, "OUTCOMES");
            Map<String, List<Map<String, Object>>> outcomeSnapshots = outcomes(study, samples, controls);
            if (!running(study)) return true;
            progress(study, 4, 5, "DISCOVERY");
            List<MarketFactorResearchBO.FactorResult> factorResults = discover(study, samples, controls, sequencePoints, outcomeSnapshots);
            repository.saveFactorResults(study.tenantId(), study.ownerId(), study.id(), factorResults);
            progress(study, 5, 5, "FINALIZING");
            int outcomeRows = outcomeSnapshots.size();
            if (samples.isEmpty()) {
                complete(study, MarketFactorResearchBO.StudyStatus.NO_DATA, 0, 0, cohort.warnings());
            } else {
                List<String> warnings = new ArrayList<>(cohort.warnings() == null ? List.of() : cohort.warnings());
                if (snapshots == null) warnings.add("MATERIALIZED_CONTROL_MATCHING_NOT_CONFIGURED");
                if (controls.isEmpty()) warnings.add("NO_MATCHED_CONTROLS");
                if (discoveryEngine == null) warnings.add("SEQUENCE_DISCOVERY_NOT_CONFIGURED");
                if (factorResults.isEmpty()) warnings.add("NO_DISCOVERED_PATTERNS");
                if (outcomeRows == 0) warnings.add("OUTCOME_DATA_UNAVAILABLE");
                boolean complete = !controls.isEmpty() && !factorResults.isEmpty() && outcomeRows > 0
                        && study.leakageCheckPassed() && warnings.isEmpty();
                complete(study, complete ? MarketFactorResearchBO.StudyStatus.SUCCEEDED : MarketFactorResearchBO.StudyStatus.PARTIAL,
                        samples.size(), controls.size(), warnings);
            }
        } catch (RuntimeException exception) {
            complete(study, MarketFactorResearchBO.StudyStatus.FAILED, 0, 0,
                    List.of("STUDY_COHORT_RESOLUTION_FAILED:" + safeCode(exception)));
        }
        return true;
    }

    private Map<String, List<Map<String, Object>>> outcomes(MarketFactorResearchBO.Study study, List<MarketFactorResearchBO.StudySample> events,
                                                             List<MarketFactorResearchBO.StudySample> controls) {
        if (snapshots == null) return Map.of();
        MarketFactorOutcomeEvaluator evaluator = new MarketFactorOutcomeEvaluator();
        Map<String, List<Map<String, Object>>> written = new HashMap<>();
        List<Map<String, Object>> pending = new ArrayList<>();
        for (MarketFactorResearchBO.StudySample sample : java.util.stream.Stream.concat(events.stream(), controls.stream()).toList()) {
            MarketFactorStudySnapshotRepository.OutcomeSeries series = snapshots.findOutcomeSeries(sample.code(), sample.market(), sample.eventTradeDate(),
                    252, study.dataCutoffAt(), study.featureRegistryVersion());
            if (series == null) continue;
            List<MarketFactorOutcomeEvaluator.Outcome> evaluated = evaluator.evaluate(series.entryClose(), series.benchmarkEntryClose(),
                    series.closes(), study.config().outcomeHorizons());
            List<Map<String, Object>> payload = evaluated.stream().map(value -> {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("horizon", value.horizon()); row.put("status", value.status()); row.put("stockReturnPct", value.stockReturnPct());
                row.put("benchmarkReturnPct", value.benchmarkReturnPct()); row.put("excessReturnPct", value.excessReturnPct());
                row.put("maximumFavorablePct", value.maximumFavorablePct()); row.put("maximumAdversePct", value.maximumAdversePct());
                return row;
            }).toList();
            Map<String, Object> row = new HashMap<>();
            row.put("sampleType", sample.sampleType().name()); row.put("code", sample.code()); row.put("market", sample.market());
            row.put("eventTradeDate", sample.eventTradeDate()); row.put("outcome", payload);
            pending.add(row);
            written.put(sampleId(sample), payload);
        }
        repository.saveOutcomesBatch(study.tenantId(), study.ownerId(), study.id(), pending);
        return Map.copyOf(written);
    }

    private boolean running(MarketFactorResearchBO.Study study) {
        return repository.isRunning(study.tenantId(), study.ownerId(), study.id());
    }

    private void progress(MarketFactorResearchBO.Study study, int completed, int total, String stage) {
        progress(study, completed, total, stage, study.eventSampleCount(), study.controlSampleCount());
    }

    private void progress(MarketFactorResearchBO.Study study, int completed, int total, String stage, int events, int controls) {
        repository.updateProgress(study.tenantId(), study.ownerId(), study.id(), Map.of(
                "completedSteps", completed, "totalSteps", total, "stage", stage,
                "eventSampleCount", events, "controlSampleCount", controls));
    }

    private List<MarketFactorResearchBO.FactorResult> discover(MarketFactorResearchBO.Study study,
                                                                List<MarketFactorResearchBO.StudySample> events,
                                                                List<MarketFactorResearchBO.StudySample> controls,
                                                                List<MarketFactorResearchBO.SequencePoint> points,
                                                                Map<String, List<Map<String, Object>>> outcomeSnapshots) {
        if (discoveryEngine == null || points.isEmpty()) return List.of();
        PatternDiscoveryInput input = new PatternDiscoveryInput(series(events, points), series(controls, points));
        if (input.events().isEmpty() || input.controls().isEmpty()) return List.of();
        List<PatternDiscoveryCandidate> candidates = discoveryEngine.discover(input);
        MarketFactorStatisticsPolicy statistics = new MarketFactorStatisticsPolicy();
        List<MarketFactorResearchBO.FactorResult> results = new ArrayList<>();
        List<Double> pValues = new ArrayList<>();
        for (PatternDiscoveryCandidate candidate : candidates) {
            double eventRate = input.events().isEmpty() ? 0d : (double) candidate.eventSupport() / input.events().size();
            double controlRate = input.controls().isEmpty() ? 0d : (double) candidate.controlSupport() / input.controls().size();
            pValues.add(Math.min(1d, Math.max(0d, 1d - Math.abs(eventRate - controlRate) * Math.sqrt(input.events().size()))));
        }
        List<Double> qValues = statistics.benjaminiHochberg(pValues);
        int rank = 1;
        for (int i = 0; i < candidates.size(); i++) {
            PatternDiscoveryCandidate candidate = candidates.get(i);
            MarketFactorStatisticsPolicy.Comparison comparison = statistics.compare(
                    new MarketFactorStatisticsPolicy.BinaryEvidence(candidate.eventSupport(), input.events().size()),
                    new MarketFactorStatisticsPolicy.BinaryEvidence(candidate.controlSupport(), input.controls().size()));
            double qValue = qValues.get(i);
            ChronologicalEvidence chronological = chronologicalEvidence(candidate, events, controls);
            StabilityEvidence monthly = monthlyStability(candidate, events, controls);
            StabilityEvidence regime = regimeStability(candidate, events, controls);
            StabilityEvidence outcome = outcomeStability(candidate, outcomeSnapshots, study.config().outcomeHorizons());
            DiscoveredFactorPromotionPolicy.Verdict verdict = new DiscoveredFactorPromotionPolicy().evaluate(
                    new DiscoveredFactorPromotionPolicy.Evaluation(input.events().size(), input.controls().size(),
                            comparison.eventSupportRate(), comparison.controlSupportRate(), qValue,
                            chronological.trainEffect(), chronological.validationEffect(), monthly.stable(), regime.stable(), outcome.stable()));
            results.add(new MarketFactorResearchBO.FactorResult(candidate.code(), 1, candidate.method(), candidate.scope(), candidate.recipe(),
                    candidate.learnedBounds(), Map.of("eventSupportRate", comparison.eventSupportRate(), "controlSupportRate", comparison.controlSupportRate(),
                            "lift", comparison.lift(), "effect", comparison.effect(), "confidenceLower", comparison.effectConfidenceInterval().lower(),
                            "confidenceUpper", comparison.effectConfidenceInterval().upper()),
                    Map.of("qValue", qValue, "trainValidation", chronological.status(), "trainEffect", chronological.trainEffect(), "validationEffect", chronological.validationEffect(),
                            "monthly", monthly.status(), "regime", regime.status(), "outcome", outcome.status()),
                    Map.of("eventSupport", candidate.eventSupport(), "controlSupport", candidate.controlSupport(), "eventSampleRefs", candidate.eventSampleIds(), "controlSampleRefs", candidate.controlSampleIds()), verdict.name(), rank++));
        }
        return List.copyOf(results);
    }

    private ChronologicalEvidence chronologicalEvidence(PatternDiscoveryCandidate candidate, List<MarketFactorResearchBO.StudySample> events,
                                                        List<MarketFactorResearchBO.StudySample> controls) {
        try {
            Map<String, MarketFactorResearchBO.StudySample> samples = java.util.stream.Stream.concat(events.stream(), controls.stream())
                    .collect(java.util.stream.Collectors.toMap(this::sampleId, value -> value, (left, right) -> left));
            var split = new cn.cccs7.cynexus.market.domain.factorresearch.statistics.ChronologicalStudySplitPolicy().split(samples.values().stream()
                    .map(value -> new cn.cccs7.cynexus.market.domain.factorresearch.statistics.ChronologicalStudySplitPolicy.Observation(sampleId(value), value.eventTradeDate())).toList());
            java.util.Set<String> train = split.train().stream().map(cn.cccs7.cynexus.market.domain.factorresearch.statistics.ChronologicalStudySplitPolicy.Observation::sampleId).collect(java.util.stream.Collectors.toSet());
            java.util.Set<String> validation = split.validation().stream().map(cn.cccs7.cynexus.market.domain.factorresearch.statistics.ChronologicalStudySplitPolicy.Observation::sampleId).collect(java.util.stream.Collectors.toSet());
            double trainEffect = effect(candidate, samples, train);
            double validationEffect = effect(candidate, samples, validation);
            return new ChronologicalEvidence(trainEffect > 0d && validationEffect > 0d ? "PASSED" : "FAILED", trainEffect, validationEffect);
        } catch (IllegalArgumentException ignored) { return new ChronologicalEvidence("UNAVAILABLE", 0d, 0d); }
    }

    private double effect(PatternDiscoveryCandidate candidate, Map<String, MarketFactorResearchBO.StudySample> samples, Set<String> ids) {
        long eventTotal = ids.stream().map(samples::get).filter(value -> value.sampleType() == MarketFactorResearchBO.SampleType.EVENT).count();
        long controlTotal = ids.stream().map(samples::get).filter(value -> value.sampleType() == MarketFactorResearchBO.SampleType.CONTROL).count();
        if (eventTotal == 0 || controlTotal == 0) return 0d;
        long eventSupport = candidate.eventSampleIds().stream().filter(ids::contains).count();
        long controlSupport = candidate.controlSampleIds().stream().filter(ids::contains).count();
        return (double) eventSupport / eventTotal - (double) controlSupport / controlTotal;
    }

    private StabilityEvidence monthlyStability(PatternDiscoveryCandidate candidate, List<MarketFactorResearchBO.StudySample> events,
                                               List<MarketFactorResearchBO.StudySample> controls) {
        Map<String, MarketFactorResearchBO.StudySample> samples = samples(events, controls);
        Map<java.time.YearMonth, Set<String>> partitions = samples.entrySet().stream().collect(java.util.stream.Collectors.groupingBy(
                entry -> java.time.YearMonth.from(entry.getValue().eventTradeDate()), java.util.stream.Collectors.mapping(Map.Entry::getKey, java.util.stream.Collectors.toSet())));
        return stability(candidate, samples, partitions.values());
    }

    private StabilityEvidence regimeStability(PatternDiscoveryCandidate candidate, List<MarketFactorResearchBO.StudySample> events,
                                              List<MarketFactorResearchBO.StudySample> controls) {
        Map<String, MarketFactorResearchBO.StudySample> samples = samples(events, controls);
        Map<String, Set<String>> partitions = samples.entrySet().stream().filter(entry -> regime(entry.getValue()) != null)
                .collect(java.util.stream.Collectors.groupingBy(entry -> regime(entry.getValue()), java.util.stream.Collectors.mapping(Map.Entry::getKey, java.util.stream.Collectors.toSet())));
        return stability(candidate, samples, partitions.values());
    }

    private StabilityEvidence stability(PatternDiscoveryCandidate candidate, Map<String, MarketFactorResearchBO.StudySample> samples,
                                        java.util.Collection<Set<String>> partitions) {
        List<Double> effects = partitions.stream().map(ids -> effect(candidate, samples, ids)).filter(value -> !Double.isNaN(value)).toList();
        if (effects.size() < 2) return new StabilityEvidence("UNAVAILABLE", false);
        return effects.stream().allMatch(value -> value > 0d) ? new StabilityEvidence("PASSED", true) : new StabilityEvidence("FAILED", false);
    }

    /**
     * Outcome evidence is evaluated only after discovery and never feeds cohort
     * matching or pattern generation.  A candidate is stable only when its
     * supporting event samples have positive excess-return lift against its
     * supporting controls on at least two configured horizons.
     */
    private StabilityEvidence outcomeStability(PatternDiscoveryCandidate candidate,
                                               Map<String, List<Map<String, Object>>> outcomes,
                                               List<Integer> horizons) {
        if (outcomes == null || outcomes.isEmpty()) return new StabilityEvidence("UNAVAILABLE", false);
        List<Double> effects = new ArrayList<>();
        for (Integer horizon : horizons == null ? List.<Integer>of() : horizons) {
            if (horizon == null) continue;
            List<Double> eventValues = outcomeValues(candidate.eventSampleIds(), outcomes, horizon);
            List<Double> controlValues = outcomeValues(candidate.controlSampleIds(), outcomes, horizon);
            if (!eventValues.isEmpty() && !controlValues.isEmpty()) {
                effects.add(mean(eventValues) - mean(controlValues));
            }
        }
        if (effects.size() < 2) return new StabilityEvidence("UNAVAILABLE", false);
        return effects.stream().allMatch(effect -> effect > 0d)
                ? new StabilityEvidence("PASSED", true) : new StabilityEvidence("FAILED", false);
    }

    private List<Double> outcomeValues(List<String> sampleIds, Map<String, List<Map<String, Object>>> outcomes, int horizon) {
        List<Double> values = new ArrayList<>();
        for (String sampleId : sampleIds == null ? List.<String>of() : sampleIds) {
            for (Map<String, Object> outcome : outcomes.getOrDefault(sampleId, List.of())) {
                Object reportedHorizon = outcome.get("horizon");
                if (!(reportedHorizon instanceof Number number) || number.intValue() != horizon) continue;
                if (!"AVAILABLE".equals(outcome.get("status"))) continue;
                Double excess = number(outcome.get("excessReturnPct"));
                if (excess != null) values.add(excess);
            }
        }
        return List.copyOf(values);
    }

    private double mean(List<Double> values) { return values.stream().mapToDouble(Double::doubleValue).average().orElse(0d); }
    private Double number(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try { return value == null ? null : Double.parseDouble(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private Map<String, MarketFactorResearchBO.StudySample> samples(List<MarketFactorResearchBO.StudySample> events, List<MarketFactorResearchBO.StudySample> controls) {
        return java.util.stream.Stream.concat(events.stream(), controls.stream()).collect(java.util.stream.Collectors.toMap(this::sampleId, value -> value, (left, right) -> left));
    }

    private String regime(MarketFactorResearchBO.StudySample sample) {
        for (Map<String, Object> values : List.of(sample.matchingMetadata(), sample.source(), sample.coverage())) {
            for (String key : List.of("marketRegime", "industryRegime", "market_regime", "industry_regime")) {
                Object value = values.get(key);
                if (value instanceof String text && !text.isBlank()) return text;
            }
        }
        return null;
    }

    private String sampleId(MarketFactorResearchBO.StudySample sample) { return sample.sampleType().name() + ':' + sample.market() + ':' + sample.code() + ':' + sample.eventTradeDate(); }
    private record ChronologicalEvidence(String status, double trainEffect, double validationEffect) { }
    private record StabilityEvidence(String status, boolean stable) { }

    private List<PatternDiscoveryInput.Series> series(List<MarketFactorResearchBO.StudySample> samples,
                                                       List<MarketFactorResearchBO.SequencePoint> points) {
        Map<String, Map<String, List<MarketFactorResearchBO.SequencePoint>>> indexed = new HashMap<>();
        for (MarketFactorResearchBO.SequencePoint point : points) {
            String sampleId = point.sampleType().name() + ':' + point.market() + ':' + point.code() + ':' + point.eventTradeDate();
            indexed.computeIfAbsent(sampleId, ignored -> new HashMap<>())
                    .computeIfAbsent(point.sequenceCode(), ignored -> new ArrayList<>())
                    .add(point);
        }
        return samples.stream().map(sample -> {
            Map<String, List<Double>> primitives = new HashMap<>();
            for (Map.Entry<String, List<MarketFactorResearchBO.SequencePoint>> entry : indexed
                    .getOrDefault(sampleId(sample), Map.of()).entrySet()) {
                entry.getValue().sort(Comparator.comparingInt(MarketFactorResearchBO.SequencePoint::offset));
                primitives.put(entry.getKey(), entry.getValue().stream()
                        .map(point -> point.normalizedValue().doubleValue()).toList());
            }
            return new PatternDiscoveryInput.Series(sampleId(sample),
                    primitives, Map.of("eventType", sample.eventType().name()));
        }).toList();
    }

    private List<MarketFactorResearchBO.SequencePoint> sequencePoints(MarketFactorResearchBO.Study study,
                                                                        List<MarketFactorResearchBO.StudySample> events,
                                                                        List<MarketFactorResearchBO.StudySample> controls) {
        if (snapshots == null) return List.of();
        EventAlignedSequencePolicy policy = new EventAlignedSequencePolicy();
        List<MarketFactorResearchBO.SequencePoint> result = new ArrayList<>();
        for (MarketFactorResearchBO.StudySample sample : java.util.stream.Stream.concat(events.stream(), controls.stream()).toList()) {
            List<MarketFactorStudySnapshotRepository.DailyPrimitivePoint> points = snapshots.findDailyPrimitives(sample.code(), sample.market(),
                    sample.eventTradeDate(), study.config().dailyPreWindowSessions(), study.dataCutoffAt(), study.featureRegistryVersion());
            Set<String> primitives = points.stream().flatMap(point -> point.values().keySet().stream()).collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
            for (String primitive : primitives) {
                List<EventAlignedSequencePolicy.DailyPoint> values = points.stream().map(point -> point.values().containsKey(primitive)
                        ? new EventAlignedSequencePolicy.DailyPoint(point.tradeDate(), point.availableAt(), point.values().get(primitive)) : null)
                        .filter(java.util.Objects::nonNull).toList();
                if (values.isEmpty()) continue;
                EventAlignedSequencePolicy.Sequence sequence = policy.daily(primitive, sample.eventTradeDate(), study.dataCutoffAt(), values);
                for (EventAlignedSequencePolicy.Point point : sequence.points()) {
                    result.add(new MarketFactorResearchBO.SequencePoint(sample.code(), sample.market(), sample.eventTradeDate(), sample.sampleType(),
                            sequence.frequency(), sequence.code(), point.offset(), point.observedAt().atStartOfDay().atOffset(java.time.ZoneOffset.UTC),
                            point.availableAt(), point.rawValue(), point.normalizedValue(), Map.of("normalization", "MEDIAN_MAD")));
                }
            }
        }
        return List.copyOf(result);
    }

    private List<MarketFactorResearchBO.StudySample> controls(MarketFactorResearchBO.Study study,
                                                                List<MarketFactorResearchBO.StudySample> events) {
        if (snapshots == null || controlPolicy == null || events.isEmpty()) return List.of();
        Map<LocalDate, List<MarketFactorStudySnapshotRepository.Snapshot>> byEventDate = new HashMap<>();
        Map<LocalDate, BucketIndexes> bucketIndexesByDate = new HashMap<>();
        Map<LocalDate, Map<String, MarketFactorStudySnapshotRepository.Snapshot>> snapshotsByDate = new HashMap<>();
        List<MarketFactorResearchBO.StudySample> controls = new ArrayList<>();
        Set<String> used = new HashSet<>();
        Set<String> eventMembers = events.stream().map(value -> value.market() + ':' + value.code() + ':' + value.eventTradeDate()).collect(java.util.stream.Collectors.toSet());
        List<LocalDate> dates = events.stream().map(MarketFactorResearchBO.StudySample::eventTradeDate).distinct().sorted().toList();
        Set<LocalDate> reportedDates = new HashSet<>();
        for (MarketFactorResearchBO.StudySample event : events) {
            if (reportedDates.add(event.eventTradeDate())) {
                repository.updateProgress(study.tenantId(), study.ownerId(), study.id(), Map.of(
                        "completedSteps", 1, "totalSteps", 5, "stage", "CONTROLS " + (reportedDates.size()) + "/" + dates.size() + " " + event.eventTradeDate(),
                        "eventSampleCount", events.size(), "controlSampleCount", controls.size()));
            }
            List<MarketFactorStudySnapshotRepository.Snapshot> universe = byEventDate.computeIfAbsent(event.eventTradeDate(),
                    date -> snapshots.findPreEventUniverse(date, study.dataCutoffAt(), study.featureRegistryVersion()));
            BucketIndexes bucketIndexes = bucketIndexesByDate.computeIfAbsent(event.eventTradeDate(), ignored -> bucketIndexes(universe));
            Map<String, MarketFactorStudySnapshotRepository.Snapshot> snapshotsByKey = snapshotsByDate.computeIfAbsent(event.eventTradeDate(), ignored ->
                    universe.stream().collect(java.util.stream.Collectors.toMap(this::key, value -> value, (left, right) -> left)));
            MarketFactorStudySnapshotRepository.Snapshot eventSnapshot = snapshotsByKey.get(event.market() + ":" + event.code());
            if (eventSnapshot == null) continue;
            List<MarketFactorControlMatchPolicy.Candidate> candidates = universe.stream()
                    .map(snapshot -> candidate(snapshot, event, bucketIndexes, used, eventMembers)).toList();
            MarketFactorControlMatchPolicy.Event matchEvent = new MarketFactorControlMatchPolicy.Event(event.code(), event.market(),
                    event.eventTradeDate(), eventSnapshot.industry(), bucketIndexes.marketCap().getOrDefault(key(eventSnapshot), 0),
                    bucketIndexes.amount().getOrDefault(key(eventSnapshot), 0));
            for (MarketFactorControlMatchPolicy.Match match : controlPolicy.match(matchEvent, candidates, study.config().controlsPerEvent())) {
                MarketFactorStudySnapshotRepository.Snapshot snapshot = snapshotsByKey.get(match.candidate().market() + ":" + match.candidate().code());
                if (snapshot == null) continue;
                String key = snapshot.market() + ':' + snapshot.code() + ':' + event.eventTradeDate();
                if (!used.add(key)) continue;
                Map<String, Object> matchMetadata = new HashMap<>();
                matchMetadata.put("matchedEventCode", event.code());
                matchMetadata.put("featureTradeDate", snapshot.featureTradeDate().toString());
                matchMetadata.put("matchQuality", match.quality().name());
                matchMetadata.put("matchDistance", match.distance());
                if (snapshot.marketRegime() != null) matchMetadata.put("marketRegime", snapshot.marketRegime());
                if (snapshot.industryRegime() != null) matchMetadata.put("industryRegime", snapshot.industryRegime());
                controls.add(new MarketFactorResearchBO.StudySample(snapshot.code(), snapshot.market(), event.eventTradeDate(), event.anchorAt(),
                        event.eventType(), MarketFactorResearchBO.SampleType.CONTROL,
                        matchMetadata,
                        Map.of("name", snapshot.source(), "availableAt", snapshot.availableAt().toString()),
                        Map.of("snapshot", snapshot.coverage())));
            }
        }
        return List.copyOf(controls);
    }

    private MarketFactorControlMatchPolicy.Candidate candidate(MarketFactorStudySnapshotRepository.Snapshot snapshot,
                                                                 MarketFactorResearchBO.StudySample event,
                                                                 BucketIndexes bucketIndexes,
                                                                 Set<String> used, Set<String> eventMembers) {
        String key = snapshot.market() + ':' + snapshot.code() + ':' + event.eventTradeDate();
        return new MarketFactorControlMatchPolicy.Candidate(snapshot.code(), snapshot.market(), event.eventTradeDate(), snapshot.industry(),
                bucketIndexes.marketCap().getOrDefault(key(snapshot), 0), bucketIndexes.amount().getOrDefault(key(snapshot), 0), snapshot.st(),
                eventMembers.contains(key) || used.contains(key));
    }

    private BucketIndexes bucketIndexes(List<MarketFactorStudySnapshotRepository.Snapshot> universe) {
        List<BigDecimal> marketCaps = universe.stream().map(MarketFactorStudySnapshotRepository.Snapshot::floatMarketCap)
                .filter(value -> value != null && value.signum() >= 0).sorted().toList();
        List<BigDecimal> amounts = universe.stream().map(MarketFactorStudySnapshotRepository.Snapshot::amount)
                .filter(value -> value != null && value.signum() >= 0).sorted().toList();
        Map<String, Integer> marketCap = new HashMap<>();
        Map<String, Integer> amount = new HashMap<>();
        for (MarketFactorStudySnapshotRepository.Snapshot snapshot : universe) {
            marketCap.put(key(snapshot), bucket(snapshot.floatMarketCap(), marketCaps));
            amount.put(key(snapshot), bucket(snapshot.amount(), amounts));
        }
        return new BucketIndexes(Map.copyOf(marketCap), Map.copyOf(amount));
    }

    private String key(MarketFactorStudySnapshotRepository.Snapshot snapshot) {
        return snapshot.market() + ':' + snapshot.code();
    }

    private int bucket(BigDecimal value, List<MarketFactorStudySnapshotRepository.Snapshot> universe, boolean marketCap) {
        List<BigDecimal> values = universe.stream().map(snapshot -> marketCap ? snapshot.floatMarketCap() : snapshot.amount())
                .filter(candidate -> candidate != null && candidate.signum() >= 0).sorted(Comparator.naturalOrder()).toList();
        return bucket(value, values);
    }

    private int bucket(BigDecimal value, List<BigDecimal> values) {
        if (value == null || value.signum() < 0) return 0;
        if (values.isEmpty()) return 0;
        int index = java.util.Collections.binarySearch(values, value);
        index = index < 0 ? -index - 1 : index;
        return Math.min(9, (int) ((long) index * 10 / values.size()));
    }

    private record BucketIndexes(Map<String, Integer> marketCap, Map<String, Integer> amount) { }

    private MarketFactorResearchBO.StudySample eventSample(MarketFactorResearchBO.Study study, MarketEventCohort.Sample sample) {
        return eventSample(study, sample, null);
    }

    private List<MarketFactorResearchBO.StudySample> eventSamples(MarketFactorResearchBO.Study study, List<MarketEventCohort.Sample> cohortSamples) {
        if (cohortSamples == null || cohortSamples.isEmpty()) return List.of();
        if (snapshots == null) return cohortSamples.stream().map(sample -> eventSample(study, sample)).toList();
        Map<LocalDate, List<MarketFactorStudySnapshotRepository.Snapshot>> universes = new HashMap<>();
        return cohortSamples.stream().map(sample -> {
            List<MarketFactorStudySnapshotRepository.Snapshot> universe = universes.computeIfAbsent(sample.tradeDate(),
                    date -> snapshots.findPreEventUniverse(date, study.dataCutoffAt(), study.featureRegistryVersion()));
            MarketFactorStudySnapshotRepository.Snapshot snapshot = universe.stream()
                    .filter(value -> value.code().equals(sample.code()) && value.market().equals(sample.market())).findFirst().orElse(null);
            return eventSample(study, sample, snapshot);
        }).toList();
    }

    private MarketFactorResearchBO.StudySample eventSample(MarketFactorResearchBO.Study study, MarketEventCohort.Sample sample,
                                                            MarketFactorStudySnapshotRepository.Snapshot snapshot) {
        if (sample.anchorAt() != null && sample.anchorAt().isAfter(study.dataCutoffAt())) {
            throw new IllegalArgumentException("EVENT_ANCHOR_AFTER_STUDY_CUTOFF");
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cohortDefinitionVersion", study.config().cohortDefinitionVersion());
        if (snapshot != null && snapshot.marketRegime() != null) metadata.put("marketRegime", snapshot.marketRegime());
        if (snapshot != null && snapshot.industryRegime() != null) metadata.put("industryRegime", snapshot.industryRegime());
        return new MarketFactorResearchBO.StudySample(sample.code(), sample.market(), sample.tradeDate(), sample.anchorAt(),
                study.config().eventType(), MarketFactorResearchBO.SampleType.EVENT,
                Map.copyOf(metadata),
                Map.of("name", sample.source()), sample.sourceFacts());
    }

    private void complete(MarketFactorResearchBO.Study study, MarketFactorResearchBO.StudyStatus status, int events, int controls,
                          List<String> warnings) {
        lifecyclePolicy.requireTransition(MarketFactorResearchBO.StudyStatus.RUNNING, status);
        repository.complete(new MarketFactorResearchBO.Study(study.id(), study.tenantId(), study.ownerId(), study.config(), status,
                study.dataCutoffAt(), study.featureRegistryVersion(), study.leakageCheckPassed(), events, controls, warnings));
    }

    private List<String> append(List<String> values, String addition) {
        return java.util.stream.Stream.concat((values == null ? List.<String>of() : values).stream(), java.util.stream.Stream.of(addition)).toList();
    }
    private String safeCode(RuntimeException exception) { return exception.getClass().getSimpleName(); }
}
