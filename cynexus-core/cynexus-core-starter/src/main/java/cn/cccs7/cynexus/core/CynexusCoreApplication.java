package cn.cccs7.cynexus.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = CynexusCoreApplication.CORE_COMPONENT_BASE_PACKAGE)
@Import(CoreSchedulingConfiguration.class)
public class CynexusCoreApplication {

    public static final String CORE_COMPONENT_BASE_PACKAGE = "cn.cccs7.cynexus.core";

    public static void main(String[] args) {
        SpringApplication.run(CynexusCoreApplication.class, args);
    }
}
