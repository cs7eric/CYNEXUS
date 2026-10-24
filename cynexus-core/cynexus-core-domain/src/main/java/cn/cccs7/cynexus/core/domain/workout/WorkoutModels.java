package cn.cccs7.cynexus.core.domain.workout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class WorkoutModels {
    private WorkoutModels() {}

    public record Session(String id, LocalDate date, String category, int minutes,
                          String split, String note) {}

    public record Observation(String id, String metric, String side, BigDecimal value,
                              LocalDate measuredAt, String note) {}

    public record Snapshot(LocalDate cycleStart, boolean sample, List<Session> sessions,
                           List<Observation> observations) {}

    public record Plan(LocalDate cycleStart, String timezone, String name) {}

    public record StoredRequest(String id, String fingerprint, boolean deleted) {}
}
