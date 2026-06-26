package cn.cccs7.cynexus.ai.domain.valueobject;

public record ModelCapability(boolean toolCallSupported, boolean structuredOutputSupported, boolean visionSupported) {
}
