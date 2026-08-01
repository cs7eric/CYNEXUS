package cn.cccs7.cynexus.market.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the JSON mapper shared by market persistence and messaging adapters.
 * The market starter does not use Spring Boot's web MVC auto-configuration,
 * so an explicit mapper is required for the Rabbit consumers and portfolio
 * adapters.
 */
@Configuration
public class MarketJacksonConfiguration {

    @Bean
    public ObjectMapper marketObjectMapper() {
        return new ObjectMapper();
    }
}
