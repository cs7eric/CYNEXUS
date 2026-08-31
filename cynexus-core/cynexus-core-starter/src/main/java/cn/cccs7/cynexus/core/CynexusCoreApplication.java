package cn.cccs7.cynexus.core;

import cn.cccs7.cynexus.common.db.config.CynexusDbAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.cloud.openfeign.EnableFeignClients;
import cn.cccs7.cynexus.knowledgebase.api.feign.KnowledgeBaseFeignClient;

@SpringBootApplication(scanBasePackages = CynexusCoreApplication.CORE_COMPONENT_BASE_PACKAGE)
@EnableFeignClients(basePackageClasses = KnowledgeBaseFeignClient.class)
@Import({CoreSchedulingConfiguration.class, CynexusDbAutoConfiguration.class})
public class CynexusCoreApplication {

    public static final String CORE_COMPONENT_BASE_PACKAGE = "cn.cccs7.cynexus.core";

    public static void main(String[] args) {
        SpringApplication.run(CynexusCoreApplication.class, args);
    }
}
