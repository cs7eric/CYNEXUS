package cn.cccs7.cynexus.core.domain.workout;

import cn.cccs7.cynexus.core.domain.workout.WorkoutModels.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkoutDomainService {
    private final WorkoutRepository repository;
    public WorkoutDomainService(WorkoutRepository repository) { this.repository = repository; }

    public Snapshot snapshot(String owner) {
        WorkoutPolicy.text(owner, 512, "owner");
        Snapshot result = repository.snapshot(owner);
        Plan plan = repository.plan(owner);
        return result == null ? new Snapshot(plan == null ? LocalDate.now() : plan.cycleStart(), false, List.of(), List.of()) : result;
    }

    public void savePlan(String owner, LocalDate cycleStart, String timezone, String name, String key) {
        WorkoutPolicy.text(owner, 512, "owner");
        cycleStart = WorkoutPolicy.date(cycleStart, "cycleStart");
        timezone = WorkoutPolicy.optionalText(timezone, 64, "timezone");
        if (timezone == null) timezone = "Asia/Shanghai";
        name = WorkoutPolicy.optionalText(name, 120, "name");
        if (name == null) name = "My workout";
        repository.lockOwner(owner);
        String fingerprint = WorkoutPolicy.fingerprint(cycleStart, timezone, name);
        key = WorkoutPolicy.key(key, "workout-plan:" + cycleStart + ":" + fingerprint.substring(0, 16));
        StoredRequest previous = repository.request(owner, "plan", key);
        if (previous != null) {
            requireReplay(previous, fingerprint);
            return;
        }
        repository.savePlan(owner, new Plan(cycleStart, timezone, name), key, fingerprint);
    }

    public void saveSession(String owner, String id, LocalDate date, String categoryInput, Integer minutes,
                            String splitInput, String noteInput, String key, LocalDate cycleStartInput) {
        WorkoutPolicy.text(owner, 512, "owner");
        date = WorkoutPolicy.pastOrToday(date, "Workout date");
        String category = WorkoutPolicy.category(categoryInput);
        int duration = WorkoutPolicy.duration(minutes);
        String requestedSplit = WorkoutPolicy.split(splitInput, category);
        String note = WorkoutPolicy.optionalText(noteInput, 240, "note");
        repository.lockOwner(owner);
        Plan plan = repository.plan(owner);
        if (plan == null && cycleStartInput != null) {
            savePlan(owner, WorkoutPolicy.date(cycleStartInput, "cycleStart"), "Asia/Shanghai", "My workout", "workout-plan:auto:" + cycleStartInput);
            plan = repository.plan(owner);
        }
        Session prior = repository.sessionByDate(owner, date, true);
        String split = requestedSplit;
        if (split == null && "strength".equals(category)) split = prior != null && "strength".equals(prior.category()) ? prior.split() : plannedSplit(plan, date);
        String fingerprint = WorkoutPolicy.fingerprint(date, category, duration, requestedSplit, note);
        key = WorkoutPolicy.key(key, "workout-session:" + date + ":" + fingerprint.substring(0, 16));
        StoredRequest previous = repository.request(owner, "session", key);
        if (previous != null) { requireReplay(previous, fingerprint); return; }
        repository.saveSession(owner, new Session(id, date, category, duration, split, note), key, fingerprint);
    }

    public void deleteSession(String owner, String id, String key) {
        WorkoutPolicy.text(owner, 512, "owner");
        WorkoutPolicy.text(id, 64, "session id");
        repository.lockOwner(owner);
        String fingerprint = WorkoutPolicy.fingerprint(id);
        key = WorkoutPolicy.text(key, 128, "idempotencyKey");
        StoredRequest previous = repository.request(owner, "session", key);
        if (previous != null) { requireDeleteReplay(previous, fingerprint); return; }
        repository.deleteSession(owner, id, key, fingerprint);
    }

    public void saveObservation(String owner, String id, String metricInput, String sideInput, Double valueInput,
                                LocalDate measuredAt, String noteInput, String key, LocalDate cycleStartInput) {
        WorkoutPolicy.text(owner, 512, "owner");
        String metric = WorkoutPolicy.metric(metricInput);
        String side = WorkoutPolicy.side(sideInput, metric);
        BigDecimal value = WorkoutPolicy.value(valueInput, metric);
        measuredAt = WorkoutPolicy.pastOrToday(measuredAt, "Measured date");
        String note = WorkoutPolicy.optionalText(noteInput, 240, "note");
        repository.lockOwner(owner);
        if (repository.plan(owner) == null && cycleStartInput != null) {
            savePlan(owner, WorkoutPolicy.date(cycleStartInput, "cycleStart"), "Asia/Shanghai", "My workout", "workout-plan:auto:" + cycleStartInput);
        }
        String fingerprint = WorkoutPolicy.fingerprint(metric, side, value, measuredAt, note);
        key = WorkoutPolicy.key(key, "workout-measurement:" + metric + ":" + (side == null ? "none" : side) + ":" + measuredAt + ":" + fingerprint.substring(0, 16));
        StoredRequest previous = repository.request(owner, "measurement", key);
        if (previous != null) { requireReplay(previous, fingerprint); return; }
        repository.saveObservation(owner, new Observation(id, metric, side, value, measuredAt, note), key, fingerprint);
    }

    public void deleteObservation(String owner, String id, String key) {
        WorkoutPolicy.text(owner, 512, "owner");
        WorkoutPolicy.text(id, 64, "measurement id");
        repository.lockOwner(owner);
        String fingerprint = WorkoutPolicy.fingerprint(id);
        key = WorkoutPolicy.text(key, 128, "idempotencyKey");
        StoredRequest previous = repository.request(owner, "measurement", key);
        if (previous != null) { requireDeleteReplay(previous, fingerprint); return; }
        repository.deleteObservation(owner, id, key, fingerprint);
    }

    private static String plannedSplit(Plan plan, LocalDate date) {
        if (plan == null || date.isBefore(plan.cycleStart())) return null;
        int offset = (int) (date.toEpochDay() - plan.cycleStart().toEpochDay());
        return switch (Math.floorMod(offset, 4)) {
            case 0 -> "chest_triceps"; case 1 -> "back_biceps"; case 2 -> "shoulders"; default -> null;
        };
    }
    private static void requireReplay(StoredRequest request, String fingerprint) {
        WorkoutPolicy.require(fingerprint.equals(request.fingerprint()), "Idempotency key was already used for a different request");
        WorkoutPolicy.require(!request.deleted(), "The original request was deleted; use a new idempotency key");
    }
    private static void requireDeleteReplay(StoredRequest request, String fingerprint) {
        WorkoutPolicy.require(fingerprint.equals(request.fingerprint()), "Idempotency key was already used for a different request");
    }
}
