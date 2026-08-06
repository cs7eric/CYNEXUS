package cn.cccs7.cynexus.wechat;

import cn.cccs7.cynexus.ai.api.feign.AiExecutionFeignClient;
import cn.cccs7.cynexus.common.db.config.CynexusDbAutoConfiguration;
import cn.cccs7.cynexus.common.cache.config.CynexusCacheAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients(basePackageClasses = AiExecutionFeignClient.class)
@Import({CynexusDbAutoConfiguration.class, CynexusCacheAutoConfiguration.class})
@SpringBootApplication(scanBasePackages = "cn.cccs7.cynexus.wechat")
public class CynexusWechatApplication {
    public static void main(String[] args) {
        SpringApplication.run(CynexusWechatApplication.class, args);
    }
}
