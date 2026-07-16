package cn.cccs7.cynexus.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients(basePackages = "cn.cccs7.cynexus")
@SpringBootApplication(scanBasePackages = "cn.cccs7.cynexus")
public class CynexusAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CynexusAiApplication.class, args);
    }
}
