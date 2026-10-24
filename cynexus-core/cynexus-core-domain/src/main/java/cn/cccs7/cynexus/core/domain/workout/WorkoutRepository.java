package cn.cccs7.cynexus.core.domain.workout;

import cn.cccs7.cynexus.core.domain.workout.WorkoutModels.*;
import java.time.LocalDate;

public interface WorkoutRepository {
    void lockOwner(String owner);
    Plan plan(String owner);
    Snapshot snapshot(String owner);
    String savePlan(String owner, Plan plan, String idempotencyKey, String fingerprint);
    Session sessionByDate(String owner, LocalDate date, boolean includeDeleted);
    void saveSession(String owner, Session session, String idempotencyKey, String fingerprint);
    void deleteSession(String owner, String id, String idempotencyKey, String fingerprint);
    Observation observation(String owner, String id, boolean includeDeleted);
    void saveObservation(String owner, Observation observation, String idempotencyKey, String fingerprint);
    void deleteObservation(String owner, String id, String idempotencyKey, String fingerprint);
    StoredRequest request(String owner, String resource, String key);
    void recordRequest(String owner, String resource, String key, String id, String fingerprint);
    void markRequestsDeleted(String owner, String resource, String id);
}
