package cn.cccs7.cynexus.ai.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentPersistenceSchemaContractTest {
    private static final List<String> MIGRATIONS = List.of(
            "20260722_create_ai_agent_tables.sql", "20260722_create_ai_agent_messages.sql",
            "20260722_add_ai_agent_tasks.sql", "20260722_add_ai_agent_usage_quota.sql",
            "20260722_add_ai_agent_event_log.sql", "20260722_add_ai_agent_citations.sql",
            "20260722_add_ai_agent_run_evidence_fields.sql", "20260722_add_ai_agent_idempotency_and_retry_fields.sql",
            "20260722_add_ai_agent_feedback.sql", "20260722_add_ai_agent_write_commands.sql",
            "20260722_add_ai_agent_source_urls.sql", "20260722_add_ai_agent_cancellation_intent.sql",
            "20260722_add_ai_agent_capabilities.sql", "20260722_repair_ai_agent_capability_runtime_state.sql",
            "20260722_add_ai_agent_task_notifications.sql");

    @Test
    void agentMigrationsStayInTheAiSchemaAndExposeOwnerBoundaries() throws IOException {
        for (String migration : MIGRATIONS) assertThat(readMigration(migration)).contains("cynexus_appl_ai.ai_agent_");
        assertThat(readMigration("20260722_create_ai_agent_tables.sql"))
                .contains("owner_key varchar(128) NOT NULL", "(owner_key, updated_at DESC)");
        for (String migration : List.of("20260722_add_ai_agent_tasks.sql", "20260722_add_ai_agent_usage_quota.sql",
                "20260722_add_ai_agent_feedback.sql", "20260722_add_ai_agent_write_commands.sql")) {
            assertThat(readMigration(migration)).contains("owner_key varchar(128) NOT NULL");
        }
        assertThat(readMigration("20260722_harden_ai_agent_owner_constraints.sql"))
                .contains("cynexus_appl_ai.ai_agent_");
        assertThat(readMigration("20260722_add_ai_agent_event_log.sql"))
                .contains("owner_key varchar(128) NOT NULL", "(owner_key, run_no, sequence_no)");
        assertThat(readMigration("20260722_create_ai_agent_messages.sql"))
                .contains("owner_key varchar(128) NOT NULL", "uk_ai_agent_message_owner_sequence");
        assertThat(readMigration("20260722_create_ai_agent_tables.sql"))
                .contains("owner_key varchar(128) NOT NULL", "uk_ai_agent_run_step_owner_order");
    }

    @Test
    void ownerConstraintRepairMigrationIsRegisteredForRelease() throws IOException {
        assertThat(readMigration("20260722_harden_ai_agent_owner_constraints.sql"))
                .contains("fk_ai_agent_run_step_owner_run", "fk_ai_agent_message_owner_conversation",
                        "uk_ai_agent_run_owner_idempotency", "ck_ai_agent_task_lease_shape");
        assertThat(readResource("deploy/release/apply-agent-migrations.ps1"))
                .contains("20260722_harden_ai_agent_owner_constraints.sql");
    }

    @Test
    void taskNotificationOutboxIsIdempotentAndRegisteredForRelease() throws IOException {
        String migration = readMigration("20260722_add_ai_agent_task_notifications.sql");
        assertThat(migration).contains("ai_agent_task_notification", "event_key varchar(256) NOT NULL UNIQUE",
                "payload_json jsonb NOT NULL", "status varchar(32) NOT NULL DEFAULT 'PENDING'",
                "last_error text", "idx_ai_agent_task_notification_pending",
                "ck_ai_agent_task_notification_event_type");
        assertThat(readResource("deploy/release/apply-agent-migrations.ps1"))
                .contains("20260722_add_ai_agent_task_notifications.sql");
        String mapper = readResource(
                "cynexus-ai/cynexus-ai-infra/src/main/java/cn/cccs7/cynexus/ai/infra/basic/mapper/agent/AgentTaskNotificationMapper.java");
        assertThat(mapper).contains("JsonbTypeHandler", "ON CONFLICT (event_key) DO NOTHING");
    }

    @Test
    void identityConstraintsProtectTranscriptRetryAndUsageRecords() throws IOException {
        assertThat(readMigration("20260722_create_ai_agent_tables.sql"))
                .contains("uk_ai_agent_conversation_no UNIQUE (conversation_no)", "uk_ai_agent_run_no UNIQUE (run_no)");
        assertThat(readMigration("20260722_create_ai_agent_messages.sql"))
                .contains("uk_ai_agent_message_sequence UNIQUE (conversation_no, sequence_no)");
        assertThat(readMigration("20260722_add_ai_agent_event_log.sql"))
                .contains("uk_ai_agent_run_event_sequence UNIQUE (run_no, sequence_no)");
        assertThat(readMigration("20260722_add_ai_agent_usage_quota.sql"))
                .contains("PRIMARY KEY (owner_key, usage_date)");
        assertThat(readMigration("20260722_repair_ai_agent_capability_runtime_state.sql"))
                .contains("FOREIGN KEY (owner_key, conversation_no)",
                        "FOREIGN KEY (owner_key, run_no)", "ck_ai_agent_task_lease_shape",
                        "attempt_count <= max_attempts");
    }

    @Test
    void taskRecoveryAndRetentionRemainOwnerScoped() throws IOException {
        assertThat(readResource("cynexus-ai/cynexus-ai-infra/src/main/resources/mapper/AgentTaskMapper.xml"))
                .contains("attempt_count &lt; max_attempts", "LEASE_EXPIRED_MAX_ATTEMPTS",
                        "WHERE owner_key = #{ownerKey}");
        assertThat(readResource("cynexus-ai/cynexus-ai-infra/src/main/java/cn/cccs7/cynexus/ai/infra/basic/mapper/write/AgentWriteCommandMapper.java"))
                .contains("archiveBefore", "ARCHIVED_BY_RETENTION_POLICY");
        assertThat(readResource("cynexus-ai/cynexus-ai-infra/src/main/java/cn/cccs7/cynexus/ai/infra/basic/mapper/write/AgentWriteAuditMapper.java"))
                .contains("redactBefore", "occurred_at < #{cutoff}");
    }

    private String readMigration(String name) throws IOException {
        Path root = Path.of(System.getProperty("maven.multiModuleProjectDirectory", ".")).toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("deploy/local/postgres/migrations"))) root = root.getParent();
        assertThat(root).isNotNull();
        return Files.readString(root.resolve("deploy/local/postgres/migrations").resolve(name));
    }

    private String readResource(String relativePath) throws IOException {
        Path root = Path.of(System.getProperty("maven.multiModuleProjectDirectory", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve(relativePath))) root = root.getParent();
        assertThat(root).isNotNull();
        return Files.readString(root.resolve(relativePath));
    }
}
