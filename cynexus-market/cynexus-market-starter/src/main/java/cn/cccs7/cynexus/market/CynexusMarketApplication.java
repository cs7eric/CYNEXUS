package cn.cccs7.cynexus.market;

import cn.cccs7.cynexus.common.db.config.CynexusDbAutoConfiguration;
import cn.cccs7.cynexus.common.cache.config.CynexusCacheAutoConfiguration;
import cn.cccs7.cynexus.common.web.advice.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import({CynexusDbAutoConfiguration.class, CynexusCacheAutoConfiguration.class, GlobalExceptionHandler.class})
@SpringBootApplication(scanBasePackages = "cn.cccs7.cynexus.market")
public class CynexusMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(CynexusMarketApplication.class, args);
    }
}
