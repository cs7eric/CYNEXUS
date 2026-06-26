package cn.cccs7.cynexus.common.web.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "cynexus.web")
public class CynexusWebProperties {

    private boolean wrapResponse;
    private boolean requestLogEnabled;
}
