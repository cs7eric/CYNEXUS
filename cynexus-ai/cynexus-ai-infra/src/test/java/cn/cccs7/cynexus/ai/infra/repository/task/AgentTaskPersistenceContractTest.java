package cn.cccs7.cynexus.ai.infra.repository.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AgentTaskPersistenceContractTest {
    @Test
    void migrationExpandsLastErrorAndReleaseAppliesTheRepair() throws IOException {
        String taskMigration = readMigration("20260722_add_ai_agent_tasks.sql");
        assertThat(taskMigration).contains("last_error text", "ALTER COLUMN last_error TYPE text");
        assertThat(readResource("cynexus-ai/cynexus-ai-infra/src/main/java/cn/cccs7/cynexus/ai/infra/basic/entity/agent/AgentTaskPO.java"))
                .contains("JdbcType.LONGVARCHAR");
        assertThat(readResource("cynexus-ai/cynexus-ai-infra/src/main/resources/mapper/AgentTaskMapper.xml"))
                .contains("jdbcType=LONGVARCHAR");
        assertThat(readMigration("20260722_repair_ai_agent_capability_runtime_state.sql"))
                .contains("ck_ai_agent_task_attempts_valid", "attempt_count <= max_attempts");
        assertThat(readResource("deploy/release/apply-agent-migrations.ps1"))
                .contains("20260722_add_ai_agent_tasks.sql", "20260722_harden_ai_agent_owner_constraints.sql");
    }

    @Test
    void claimDueDeadLettersExpiredExhaustedLeasesAndRecoversOnlyEligibleLeases() throws IOException {
        String mapper = readResource("cynexus-ai/cynexus-ai-infra/src/main/resources/mapper/AgentTaskMapper.xml");
        assertThat(mapper).contains("WITH exhausted AS", "status = 'DEAD_LETTER'",
                "attempt_count &gt;= max_attempts", "attempt_count &lt; max_attempts",
                "status = 'RUNNING' AND lease_until &lt; #{now}");
    }

    @Test
    void stateTransitionsRequireOwnerAndLeaseOwnership() throws IOException {
        String mapper = readResource("cynexus-ai/cynexus-ai-infra/src/main/resources/mapper/AgentTaskMapper.xml");
        for (String operation : new String[] {"complete", "retry", "deadLetter"}) {
            String block = mapper.substring(mapper.indexOf("<update id=\"" + operation + "\""),
                    mapper.indexOf("</update>", mapper.indexOf("<update id=\"" + operation + "\"")));
            assertThat(block).contains("owner_key = #{ownerKey}", "task_no = #{taskNo}",
                    "lease_owner = #{leaseOwner}", "deleted_at IS NULL");
        }
    }

    private String readMigration(String name) throws IOException {
        return readResource("deploy/local/postgres/migrations/" + name);
    }

    private String readResource(String relativePath) throws IOException {
        Path root = Path.of(System.getProperty("maven.multiModuleProjectDirectory", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve(relativePath))) root = root.getParent();
        assertThat(root).isNotNull();
        return Files.readString(root.resolve(relativePath));
    }
}
