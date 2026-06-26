package cn.cccs7.cynexus.ai.domain.valueobject;

import java.math.BigDecimal;

public record ModelRuntimeOptions(Integer maxContextTokens, BigDecimal temperature, Integer timeoutMs) {
}
