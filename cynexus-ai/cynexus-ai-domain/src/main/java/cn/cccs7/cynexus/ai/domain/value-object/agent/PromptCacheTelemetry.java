package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import lombok.Builder;
import lombok.Value;

/** Non-sensitive request metadata used to diagnose provider prompt-cache behavior. */
@Value
@Builder(toBuilder = true)
public class PromptCacheTelemetry {

    String cacheKeyHash;
    String stablePrefixHash;
    String requestInputHash;
    Integer inputItemCount;
    String continuationMode;
    String previousResponseIdHash;
    String providerResponseIdHash;
    boolean responseStorageEnabled;
    boolean stateFallback;
}
