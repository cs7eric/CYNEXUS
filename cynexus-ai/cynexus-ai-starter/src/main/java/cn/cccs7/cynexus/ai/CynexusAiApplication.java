package cn.cccs7.cynexus.ai;

import cn.cccs7.cynexus.core.api.feign.FinanceDataFeignClient;
import cn.cccs7.cynexus.knowledgebase.api.feign.KnowledgeBaseFeignClient;
import cn.cccs7.cynexus.market.api.feign.MarketFeignClient;
import cn.cccs7.cynexus.common.db.config.CynexusDbAutoConfiguration;
import cn.cccs7.cynexus.common.cache.config.CynexusCacheAutoConfiguration;
import cn.cccs7.cynexus.common.web.advice.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import cn.cccs7.cynexus.ai.infra.ai.rag.SpringAiPgVectorConfiguration;

@EnableScheduling
@Import({CynexusDbAutoConfiguration.class, CynexusCacheAutoConfiguration.class, GlobalExceptionHandler.class,
        SpringAiPgVectorConfiguration.class})
@EnableFeignClients(basePackageClasses = {
        FinanceDataFeignClient.class,
        KnowledgeBaseFeignClient.class,
        MarketFeignClient.class
})
@SpringBootApplication(scanBasePackages = "cn.cccs7.cynexus.ai")
public class CynexusAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CynexusAiApplication.class, args);
    }
}
