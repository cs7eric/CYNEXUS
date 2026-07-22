package cn.cccs7.cynexus.ai.infra.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AgentCapabilityDynamicRegistrationContractTest {
    @Test
    void migrationAddsAnAuditableAdminRegistrationMode() throws IOException {
        String migration = read("deploy/local/postgres/migrations/20260723_add_ai_agent_dynamic_capabilities.sql");
        assertThat(migration).contains("registration_mode varchar(32)", "registration_mode IN ('HANDLER', 'ADMIN')",
                "idx_ai_agent_capability_registration_mode");
        assertThat(read("deploy/release/apply-agent-migrations.ps1"))
                .contains("20260723_add_ai_agent_dynamic_capabilities.sql");
    }

    private String read(String relativePath) throws IOException {
        Path root = Path.of(System.getProperty("maven.multiModuleProjectDirectory", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve(relativePath))) root = root.getParent();
        assertThat(root).isNotNull();
        return Files.readString(root.resolve(relativePath));
    }
}
