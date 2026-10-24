package cn.cccs7.cynexus.core.domain.workout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.core.domain.workout.WorkoutModels.Plan;
import cn.cccs7.cynexus.core.domain.workout.WorkoutModels.Session;
import cn.cccs7.cynexus.core.domain.workout.WorkoutModels.StoredRequest;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class WorkoutDomainServiceTest {
    @Test
    void olderIdempotencyKeyCannotRestoreValuesAfterLaterEdit() {
        AtomicReference<Plan> plan = new AtomicReference<>();
        AtomicReference<Session> session = new AtomicReference<>();
        Map<String, StoredRequest> requests = new HashMap<>();
        AtomicInteger writes = new AtomicInteger();
        WorkoutRepository repository = (WorkoutRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {WorkoutRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner", "deleteSession", "deleteObservation", "markRequestsDeleted" -> null;
                    case "plan" -> plan.get();
                    case "snapshot" -> null;
                    case "sessionByDate" -> session.get();
                    case "request" -> requests.get(args[1] + ":" + args[2]);
                    case "savePlan" -> {
                        Plan value = (Plan) args[1];
                        plan.set(value);
                        requests.put("plan:" + args[2], new StoredRequest("1", (String) args[3], false));
                        yield "1";
                    }
                    case "saveSession" -> {
                        Session value = (Session) args[1];
                        session.set(value);
                        requests.put("session:" + args[2], new StoredRequest("2", (String) args[3], false));
                        writes.incrementAndGet();
                        yield null;
                    }
                    default -> throw new AssertionError(method.getName());
                });

        WorkoutDomainService service = new WorkoutDomainService(repository);
        LocalDate today = LocalDate.now();
        LocalDate cycleStart = today.minusDays(12);
        service.saveSession("owner", null, today, "Strength", 45, null, null, "first", cycleStart);
        service.saveSession("owner", null, today, "Strength", 60, null, null, "second", cycleStart);
        service.saveSession("owner", null, today, "Strength", 45, null, null, "first", cycleStart);

        assertThat(plan.get()).isNotNull();
        assertThat(plan.get().cycleStart()).isEqualTo(cycleStart);
        assertThat(session.get().minutes()).isEqualTo(60);
        assertThat(writes).hasValue(2);
    }

    @Test
    void retryWithoutExplicitSplitIsStableAfterThePlanAndSessionChange() {
        AtomicReference<Plan> plan = new AtomicReference<>();
        AtomicReference<Session> session = new AtomicReference<>();
        Map<String, StoredRequest> requests = new HashMap<>();
        AtomicInteger sessionWrites = new AtomicInteger();
        WorkoutRepository repository = (WorkoutRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {WorkoutRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner", "deleteSession", "deleteObservation", "markRequestsDeleted" -> null;
                    case "plan" -> plan.get();
                    case "snapshot" -> null;
                    case "sessionByDate" -> session.get();
                    case "request" -> requests.get(args[1] + ":" + args[2]);
                    case "savePlan" -> {
                        Plan value = (Plan) args[1];
                        plan.set(value);
                        requests.put("plan:" + args[2], new StoredRequest("1", (String) args[3], false));
                        yield "1";
                    }
                    case "saveSession" -> {
                        Session value = (Session) args[1];
                        session.set(value);
                        requests.put("session:" + args[2], new StoredRequest("2", (String) args[3], false));
                        sessionWrites.incrementAndGet();
                        yield null;
                    }
                    default -> throw new AssertionError(method.getName());
                });

        WorkoutDomainService service = new WorkoutDomainService(repository);
        LocalDate today = LocalDate.now();
        LocalDate cycleStart = today.minusDays(12);
        service.saveSession("owner", null, today, "Strength", 45, null, null, "strength-request", cycleStart);
        service.saveSession("owner", null, today, "Cardio", 25, null, null, "cardio-request", cycleStart);
        service.savePlan("owner", today.plusDays(1), "Asia/Shanghai", "My workout", "new-cycle");
        service.saveSession("owner", null, today, "Strength", 45, null, null, "strength-request", cycleStart);

        assertThat(session.get().category()).isEqualTo("cardio");
        assertThat(sessionWrites).hasValue(2);
    }

    @Test
    void deleteSessionUsesDurableIdempotencyAndRejectsKeyReuseForAnotherSession() {
        Map<String, StoredRequest> requests = new HashMap<>();
        AtomicInteger deletes = new AtomicInteger();
        WorkoutRepository repository = (WorkoutRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {WorkoutRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner", "markRequestsDeleted" -> null;
                    case "request" -> requests.get(args[1] + ":" + args[2]);
                    case "deleteSession" -> {
                        assertThat(args[0]).isEqualTo("owner");
                        assertThat(args[1]).isEqualTo("42");
                        assertThat(args[2]).isEqualTo("delete-session-42");
                        deletes.incrementAndGet();
                        requests.put("session:" + args[2], new StoredRequest((String) args[1], (String) args[3], false));
                        yield null;
                    }
                    case "recordRequest" -> {
                        requests.put(args[1] + ":" + args[2], new StoredRequest((String) args[3], (String) args[4], false));
                        yield null;
                    }
                    default -> throw new AssertionError(method.getName());
                });

        WorkoutDomainService service = new WorkoutDomainService(repository);
        service.deleteSession("owner", "42", "delete-session-42");
        service.deleteSession("owner", "42", "delete-session-42");

        assertThat(deletes).hasValue(1);
        assertThatThrownBy(() -> service.deleteSession("owner", "43", "delete-session-42"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different request");
        assertThatThrownBy(() -> service.deleteSession("owner", "42", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey");
        assertThat(deletes).hasValue(1);
    }

    @Test
    void deleteMeasurementReplayRemainsSuccessfulAfterTheEntityIsMarkedDeleted() {
        AtomicReference<StoredRequest> request = new AtomicReference<>();
        AtomicInteger deletes = new AtomicInteger();
        WorkoutRepository repository = (WorkoutRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {WorkoutRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner", "markRequestsDeleted" -> null;
                    case "request" -> request.get();
                    case "deleteObservation" -> {
                        deletes.incrementAndGet();
                        request.set(new StoredRequest((String) args[1], (String) args[3], false));
                        yield null;
                    }
                    case "recordRequest" -> {
                        request.set(new StoredRequest((String) args[3], (String) args[4], false));
                        yield null;
                    }
                    default -> throw new AssertionError(method.getName());
                });

        WorkoutDomainService service = new WorkoutDomainService(repository);
        service.deleteObservation("owner", "91", "delete-measurement-91");
        request.set(new StoredRequest("91", request.get().fingerprint(), true));
        service.deleteObservation("owner", "91", "delete-measurement-91");

        assertThat(deletes).hasValue(1);
    }
}
