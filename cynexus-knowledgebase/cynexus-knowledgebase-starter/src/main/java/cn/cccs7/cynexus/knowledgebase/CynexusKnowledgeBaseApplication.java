package cn.cccs7.cynexus.knowledgebase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cn.cccs7.cynexus")
public class CynexusKnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CynexusKnowledgeBaseApplication.class, args);
    }
}
