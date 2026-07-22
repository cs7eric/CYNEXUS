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
            "20260722_add_ai_agent_capabilities.sql");

    @Test
    void agentMigrationsStayInTheAiSchemaAndExposeOwnerBoundaries() throws IOException {
        for (String migration : MIGRATIONS) assertThat(readMigration(migration)).contains("cynexus_appl_ai.ai_agent_");
        assertThat(readMigration("20260722_create_ai_agent_tables.sql"))
                .contains("owner_key varchar(128) NOT NULL", "(owner_key, updated_at DESC)");
        assertThat(readMigration("20260722_add_ai_agent_event_log.sql"))
                .contains("owner_key varchar(128) NOT NULL", "(owner_key, run_no, sequence_no)");
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
    }

    private String readMigration(String name) throws IOException {
        Path root = Path.of(System.getProperty("maven.multiModuleProjectDirectory", ".")).toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("deploy/local/postgres/migrations"))) root = root.getParent();
        assertThat(root).isNotNull();
        return Files.readString(root.resolve("deploy/local/postgres/migrations").resolve(name));
    }
}
