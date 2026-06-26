package cn.cccs7.cynexus.ai.domain.valueobject;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRuntimeOptions {

    private Integer maxContextTokens;
    private BigDecimal temperature;
    private Integer timeoutMs;
}
