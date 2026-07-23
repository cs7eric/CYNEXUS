package cn.cccs7.cynexus.knowledgebase;

import cn.cccs7.cynexus.common.db.config.CynexusDbAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(CynexusDbAutoConfiguration.class)
@SpringBootApplication(scanBasePackages = "cn.cccs7.cynexus.knowledgebase")
public class CynexusKnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CynexusKnowledgeBaseApplication.class, args);
    }
}
