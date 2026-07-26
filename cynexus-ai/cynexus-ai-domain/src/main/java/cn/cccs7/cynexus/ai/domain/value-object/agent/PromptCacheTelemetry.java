package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import java.util.List;
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
    List<String> inputItemHashes;
    List<String> inputPrefixHashes;
    List<String> inputItemKinds;
    List<Integer> inputItemBytes;
    String toolsHash;
    String requestOptionsHash;
    Integer payloadBytes;
    String continuationMode;
    String previousResponseIdHash;
    String providerResponseIdHash;
    boolean responseStorageEnabled;
    boolean stateFallback;
}
