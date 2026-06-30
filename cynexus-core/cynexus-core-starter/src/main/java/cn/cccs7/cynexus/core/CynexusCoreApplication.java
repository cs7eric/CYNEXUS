package cn.cccs7.cynexus.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "cn.cccs7.cynexus")
public class CynexusCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CynexusCoreApplication.class, args);
    }
}
