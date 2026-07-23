package cn.cccs7.cynexus.ai.domain.applicationanalysis;

/** Extracts the JSON object from a provider response. */
public interface ApplicationAnalysisStructuredOutputPort {

    String extractObject(String rawResponse);
}
