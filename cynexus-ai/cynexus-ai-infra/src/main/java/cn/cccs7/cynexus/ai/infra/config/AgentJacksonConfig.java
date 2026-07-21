package cn.cccs7.cynexus.ai.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentJacksonConfig {

    @Bean
    public ObjectMapper agentObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
