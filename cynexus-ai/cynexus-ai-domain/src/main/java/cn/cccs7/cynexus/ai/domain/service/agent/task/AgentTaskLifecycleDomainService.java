package cn.cccs7.cynexus.ai.domain.service.agent.task;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.policy.task.AgentTaskPolicy;
import cn.cccs7.cynexus.ai.domain.port.agent.AgentRunCancellationPort;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentTaskRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentTaskScheduleCommand;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The single domain entry point for the agent-task lifecycle.
 *
 * <p>The repository remains responsible for conditional persistence. This
 * service owns task identifiers, valid lifecycle transitions, retry policy and
 * worker execution parameters so application adapters do not reimplement them.</p>
 */
public final class AgentTaskLifecycleDomainService {
    private static final int DEFAULT_LIST_LIMIT = 30;
    private static final int MAX_LIST_LIMIT = 100;
    private static final int MAX_CLAIM_LIMIT = 20;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final int DEFAULT_LEASE_SECONDS = 900;

    private final AgentTaskRepository repository;
    private final AgentRunCancellationPort runCancellationPort;
    private final Clock clock;

    public AgentTaskLifecycleDomainService(AgentTaskRepository repository,
                                           AgentRunCancellationPort runCancellationPort) {
        this(repository, runCancellationPort, Clock.systemUTC());
    }

    public AgentTaskLifecycleDomainService(AgentTaskRepository repository,
                                           AgentRunCancellationPort runCancellationPort,
                                           Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.runCancellationPort = Objects.requireNonNull(runCancellationPort, "runCancellationPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AgentTaskBO schedule(AgentTaskScheduleCommand command) {
        Objects.requireNonNull(command, "command");
        AgentIdentity identity = command.identity() == null ? AgentIdentity.singleUser() : command.identity();
        AgentTaskPolicy.validate(command.userInput(), command.scheduleAt(), command.maxAttempts(), now());
        String taskNo = "ATK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        return repository.save(AgentTaskBO.builder()
                .taskNo(taskNo)
                .ownerKey(identity.storageOwnerKey())
                .tenantKey(identity.getTenantKey())
                .principalKey(identity.getPrincipalKey())
                .conversationNo(command.conversationNo())
                .userInput(command.userInput().trim())
                .preferredModelCode(command.preferredModelCode())
                .scheduleAt(command.scheduleAt())
                .nextAttemptAt(command.scheduleAt())
                .status("QUEUED")
                .attemptCount(0)
                .maxAttempts(command.maxAttempts())
                .build());
    }

    public List<AgentTaskBO> list(String ownerKey, int limit) {
        return repository.list(requireOwner(ownerKey), Math.max(1, Math.min(limit <= 0 ? DEFAULT_LIST_LIMIT : limit, MAX_LIST_LIMIT)));
    }

    public Optional<AgentTaskBO> find(String ownerKey, String taskNo) {
        return repository.find(requireOwner(ownerKey), requireTaskNo(taskNo));
    }

    public boolean pause(String ownerKey, String taskNo) {
        return repository.pause(requireOwner(ownerKey), requireTaskNo(taskNo));
    }

    public boolean resume(String ownerKey, String taskNo) {
        return repository.resume(requireOwner(ownerKey), requireTaskNo(taskNo));
    }

    public boolean cancel(AgentIdentity identity, String ownerKey, String taskNo) {
        AgentTaskBO task = find(ownerKey, taskNo).orElse(null);
        if (task == null) {
            return false;
        }
        if ("RUNNING".equals(task.getStatus()) && task.getRunNo() != null && !task.getRunNo().isBlank()) {
            runCancellationPort.cancel(identity == null ? AgentIdentity.singleUser() : identity, task.getRunNo());
        }
        return repository.cancel(requireOwner(ownerKey), requireTaskNo(taskNo));
    }

    public List<AgentTaskBO> claimDue(String workerId, OffsetDateTime at, int limit, int leaseSeconds) {
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId must not be blank");
        }
        OffsetDateTime effectiveAt = at == null ? now() : at;
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 5 : limit, MAX_CLAIM_LIMIT));
        int boundedLease = Math.max(1, leaseSeconds <= 0 ? DEFAULT_LEASE_SECONDS : leaseSeconds);
        return repository.claimDue(workerId, effectiveAt, boundedLimit, boundedLease);
    }

    public Optional<String> linkRun(AgentTaskBO task) {
        requireTask(task);
        String runNo = "TASK-RUN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        return repository.linkRun(task, runNo) ? Optional.of(runNo) : Optional.empty();
    }

    public void complete(AgentTaskBO task) {
        requireTask(task);
        requireStatus(task, "RUNNING");
        repository.complete(task);
    }

    public void retry(AgentTaskBO task, OffsetDateTime nextAttemptAt, int nextAttempt, String error) {
        requireTask(task);
        requireStatus(task, "RUNNING");
        int attempt = attemptCount(task);
        if (nextAttempt != attempt + 1 || nextAttempt > maxAttempts(task)) {
            throw new IllegalStateException("Invalid agent task retry transition");
        }
        if (nextAttemptAt == null || nextAttemptAt.isBefore(now())) {
            throw new IllegalArgumentException("nextAttemptAt must not be in the past");
        }
        repository.retry(task, nextAttemptAt, normalizeError(error));
    }

    public void deadLetter(AgentTaskBO task, String error) {
        requireTask(task);
        requireStatus(task, "RUNNING");
        repository.deadLetter(task, normalizeError(error));
    }

    public FailureDecision failureDecision(AgentTaskBO task, String error, OffsetDateTime at) {
        requireTask(task);
        requireStatus(task, "RUNNING");
        String safeError = normalizeError(error);
        int attempt = attemptCount(task);
        if (attempt < maxAttempts(task)) {
            int nextAttempt = attempt + 1;
            return FailureDecision.retry(nextAttempt, now(at).plusSeconds(AgentTaskPolicy.retryBackoffSeconds(attempt)), safeError);
        }
        return FailureDecision.deadLetter(safeError);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private OffsetDateTime now(OffsetDateTime requested) {
        return requested == null ? now() : requested;
    }

    private static int attemptCount(AgentTaskBO task) {
        return task.getAttemptCount() == null ? 1 : task.getAttemptCount();
    }

    private static int maxAttempts(AgentTaskBO task) {
        return task.getMaxAttempts() == null ? DEFAULT_MAX_ATTEMPTS : task.getMaxAttempts();
    }

    private static String normalizeError(String error) {
        return error == null || error.isBlank() ? "Agent task execution failed" : error;
    }

    private static String requireOwner(String ownerKey) {
        if (ownerKey == null || ownerKey.isBlank()) {
            throw new IllegalArgumentException("ownerKey must not be blank");
        }
        return ownerKey;
    }

    private static String requireTaskNo(String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            throw new IllegalArgumentException("taskNo must not be blank");
        }
        return taskNo;
    }

    private static void requireTask(AgentTaskBO task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
    }

    private static void requireStatus(AgentTaskBO task, String expected) {
        if (!expected.equals(task.getStatus())) {
            throw new IllegalStateException("Agent task must be " + expected + " but was " + task.getStatus());
        }
    }

    public record FailureDecision(Type type, int nextAttempt, OffsetDateTime nextAttemptAt, String error) {
        public enum Type { RETRY, DEAD_LETTER }

        public static FailureDecision retry(int nextAttempt, OffsetDateTime nextAttemptAt, String error) {
            return new FailureDecision(Type.RETRY, nextAttempt, nextAttemptAt, error);
        }

        public static FailureDecision deadLetter(String error) {
            return new FailureDecision(Type.DEAD_LETTER, 0, null, error);
        }
    }
}
