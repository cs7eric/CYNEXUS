package cn.cccs7.cynexus.core.api.dto.workout;

import java.time.LocalDate;
import java.util.List;

/** Wire contracts for the owner-scoped Workout log. */
public final class WorkoutDTO {
    private WorkoutDTO() {}

    public record Session(String id, LocalDate date, String category, int minutes, String split, String note) {}

    public record Observation(String id, String metric, String side, double value,
                              LocalDate measuredAt, String note) {}

    public record Snapshot(LocalDate cycleStart, boolean isSample, List<Session> sessions,
                           List<Observation> observations) {}

    public record PlanRequest(LocalDate cycleStart, String timezone, String name,
                              String idempotencyKey) {}

    public record DeleteRequest(String idempotencyKey) {}

    public record SessionRequest(String id, LocalDate date, String category, Integer minutes,
                                 String split, String note, String idempotencyKey, LocalDate cycleStart) {}

    public record MeasurementRequest(String id, String metric, String side, Double value,
                                     LocalDate measuredAt, String note, String idempotencyKey, LocalDate cycleStart) {}
}
