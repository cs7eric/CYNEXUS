package cn.cccs7.cynexus.common.cloud.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "cynexus.cloud")
public class CynexusCloudProperties {

    private String environment;
    private String namespace;
}
