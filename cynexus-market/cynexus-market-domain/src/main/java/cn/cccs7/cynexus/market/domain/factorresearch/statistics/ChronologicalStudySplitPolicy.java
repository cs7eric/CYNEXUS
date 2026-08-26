package cn.cccs7.cynexus.market.domain.factorresearch.statistics;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Produces a date-disjoint chronological train/validation split; random shuffling is forbidden. */
public final class ChronologicalStudySplitPolicy {
    public Split split(List<Observation> values) {
        if (values == null || values.size() < 2) throw new IllegalArgumentException("at least two observations are required");
        List<Observation> ordered = values.stream().sorted(Comparator.comparing(Observation::eventTradeDate).thenComparing(Observation::sampleId)).toList();
        int boundary = Math.max(1, (int) Math.floor(ordered.size() * .70d));
        LocalDate lastTrainDate = ordered.get(boundary - 1).eventTradeDate();
        while (boundary < ordered.size() && ordered.get(boundary).eventTradeDate().equals(lastTrainDate)) boundary++;
        if (boundary == ordered.size()) throw new IllegalArgumentException("chronological validation requires a later trade date");
        return new Split(ordered.subList(0, boundary), ordered.subList(boundary, ordered.size()));
    }

    public record Observation(String sampleId, LocalDate eventTradeDate) {
        public Observation { if (sampleId == null || sampleId.isBlank() || eventTradeDate == null) throw new IllegalArgumentException("observation is incomplete"); }
    }
    public record Split(List<Observation> train, List<Observation> validation) {
        public Split { train = List.copyOf(train); validation = List.copyOf(validation); }
    }
}
