package cn.cccs7.cynexus.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cn.cccs7.cynexus.gateway")
public class CynexusGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(CynexusGatewayApplication.class, args);
    }
}
