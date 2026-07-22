package cn.cccs7.cynexus.ai.domain.policy.capability;

import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.domain.policy.sensitivedata.AgentSourceUrlPolicy;
import java.util.Set;

/** Domain validation for capability metadata before it crosses the persistence/runtime boundary. */
public final class AgentCapabilityDescriptorPolicy {
    public static final Set<String> AVAILABILITIES = Set.of("AVAILABLE", "DEGRADED", "UNAVAILABLE", "STALE");
    public static final String ADMIN_REGISTRATION = "ADMIN";
    public static final String HANDLER_REGISTRATION = "HANDLER";
    private static final int MAX_PRIORITY = 10_000;

    private AgentCapabilityDescriptorPolicy() {
    }

    public static void validate(AgentCapabilityDescriptor descriptor) {
        if (descriptor == null) throw new IllegalArgumentException("Capability descriptor is required");
        require(descriptor.getCapabilityCode(), "capabilityCode");
        require(descriptor.getVersion(), "version");
        require(descriptor.getName(), "name");
        require(descriptor.getModule(), "module");
        require(descriptor.getDescription(), "description");
        require(descriptor.getInputSchemaJson(), "inputSchemaJson");
        require(descriptor.getOutputSchemaJson(), "outputSchemaJson");
        require(descriptor.getTenantScope(), "tenantScope");
        require(descriptor.getPromptKey(), "promptKey");
        require(descriptor.getRuntimeEntry(), "runtimeEntry");
        require(descriptor.getRouteGroup(), "routeGroup");
        if (!AVAILABILITIES.contains(normalizeAvailability(descriptor.getAvailability()))) {
            throw new IllegalArgumentException("Unsupported capability availability: " + descriptor.getAvailability());
        }
        if (!AVAILABILITIES.contains(normalizeAvailability(descriptor.getGovernanceAvailability()))) {
            throw new IllegalArgumentException("Unsupported capability governance availability: " + descriptor.getGovernanceAvailability());
        }
        if (descriptor.getPriority() < 0 || descriptor.getPriority() > MAX_PRIORITY) {
            throw new IllegalArgumentException("Capability priority must be between 0 and " + MAX_PRIORITY);
        }
        if (descriptor.getSourceUrl() != null
                && AgentSourceUrlPolicy.sanitize(descriptor.getSourceUrl(), Set.of()) == null) {
            throw new IllegalArgumentException("Capability sourceUrl must be an app-relative URL");
        }
    }

    public static String normalizeAvailability(String availability) {
        return availability == null || availability.isBlank() ? "AVAILABLE" : availability.trim().toUpperCase();
    }

    public static void validateGovernance(String availability, int priority) {
        if (!AVAILABILITIES.contains(normalizeAvailability(availability))) {
            throw new IllegalArgumentException("Unsupported capability availability: " + availability);
        }
        if (priority < 0 || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException("Capability priority must be between 0 and " + MAX_PRIORITY);
        }
    }

    /**
     * Dynamic registration is metadata-only: execution must still bind to a known
     * Spring handler. The runtime catalog performs that binding check separately.
     */
    public static void validateRegistration(AgentCapabilityDescriptor descriptor) {
        validate(descriptor);
        if (!descriptor.isReadOnly() || descriptor.isRequiresConfirmation()) {
            throw new IllegalArgumentException("Only autonomous read-only capabilities may be registered");
        }
        if (!ADMIN_REGISTRATION.equals(normalizeRegistrationMode(descriptor.getRegistrationMode()))) {
            throw new IllegalArgumentException("Capability registration mode must be ADMIN");
        }
        if (!descriptor.isEnabled()
                || !"AVAILABLE".equals(normalizeAvailability(descriptor.getAvailability()))
                || !"AVAILABLE".equals(normalizeAvailability(descriptor.getGovernanceAvailability()))) {
            throw new IllegalArgumentException("Only active capabilities may be hot-added; use governance to disable one");
        }
        if (!descriptor.getRuntimeEntry().matches("[A-Za-z0-9_.$-]+")) {
            throw new IllegalArgumentException("Capability runtimeEntry is not a safe handler identifier");
        }
    }

    public static String normalizeRegistrationMode(String mode) {
        return mode == null || mode.isBlank() ? HANDLER_REGISTRATION : mode.trim().toUpperCase();
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Capability " + field + " is required");
    }
}
