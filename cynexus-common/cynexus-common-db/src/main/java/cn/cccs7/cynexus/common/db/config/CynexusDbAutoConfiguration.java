package cn.cccs7.cynexus.common.db.config;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CynexusDbAutoConfiguration {

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator();
    }
}
