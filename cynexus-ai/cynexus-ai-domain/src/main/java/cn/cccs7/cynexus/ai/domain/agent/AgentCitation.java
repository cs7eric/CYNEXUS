package cn.cccs7.cynexus.ai.domain.agent;

import lombok.Builder;
import lombok.Value;

/** A persisted, tool-backed source reference. Model-only claims never create this value. */
@Value
@Builder
public class AgentCitation {
    String citationId;
    String title;
    String url;
    String publisher;
    String publicationDate;
    String retrievedAt;
    String dataDate;
    String excerpt;
    String freshness;
    String sourceType;
    String availability;
    boolean conflict;
}
