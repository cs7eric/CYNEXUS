package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.api.dto.workout.WorkoutDTO;
import cn.cccs7.cynexus.core.domain.workout.WorkoutDomainService;
import cn.cccs7.cynexus.core.domain.workout.WorkoutModels;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutAppService {
    private final WorkoutDomainService domain;
    public WorkoutAppService(WorkoutDomainService domain) { this.domain = domain; }

    public WorkoutDTO.Snapshot snapshot(String owner) { return map(domain.snapshot(owner)); }

    @Transactional
    public WorkoutDTO.Snapshot savePlan(String owner, WorkoutDTO.PlanRequest request) {
        require(request != null, "Plan request is required");
        domain.savePlan(owner, request.cycleStart(), request.timezone(), request.name(), request.idempotencyKey());
        return snapshot(owner);
    }

    @Transactional
    public WorkoutDTO.Snapshot saveSession(String owner, WorkoutDTO.SessionRequest request) {
        require(request != null, "Session request is required");
        domain.saveSession(owner, request.id(), request.date(), request.category(), request.minutes(), request.split(), request.note(), request.idempotencyKey(), request.cycleStart());
        return snapshot(owner);
    }

    @Transactional
    public WorkoutDTO.Snapshot deleteSession(String owner, String id, String idempotencyKey) {
        domain.deleteSession(owner, id, idempotencyKey);
        return snapshot(owner);
    }

    @Transactional
    public WorkoutDTO.Snapshot saveMeasurement(String owner, WorkoutDTO.MeasurementRequest request) {
        require(request != null, "Measurement request is required");
        domain.saveObservation(owner, request.id(), request.metric(), request.side(), request.value(), request.measuredAt(), request.note(), request.idempotencyKey(), request.cycleStart());
        return snapshot(owner);
    }

    @Transactional
    public WorkoutDTO.Snapshot deleteMeasurement(String owner, String id, String idempotencyKey) {
        domain.deleteObservation(owner, id, idempotencyKey);
        return snapshot(owner);
    }

    private WorkoutDTO.Snapshot map(WorkoutModels.Snapshot snapshot) {
        return new WorkoutDTO.Snapshot(snapshot.cycleStart(), snapshot.sample(),
                snapshot.sessions().stream().map(session -> new WorkoutDTO.Session(session.id(), session.date(), category(session.category()), session.minutes(), split(session.split()), session.note())).toList(),
                snapshot.observations().stream().map(observation -> new WorkoutDTO.Observation(observation.id(), metric(observation.metric()), observation.side(), observation.value().doubleValue(), observation.measuredAt(), observation.note())).toList());
    }
    private static String category(String value) { return "cardio".equals(value) ? "Cardio" : "Strength"; }
    private static String split(String value) {
        if (value == null) return null;
        return switch (value) { case "chest_triceps" -> "Chest + Triceps"; case "back_biceps" -> "Back + Biceps"; case "shoulders" -> "Shoulders"; default -> null; };
    }
    private static String metric(String value) {
        return switch (value) {
            case "shoulder_width" -> "shoulderWidth"; case "under_chest" -> "underChest"; case "upper_arm" -> "upperArm";
            default -> value;
        };
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
