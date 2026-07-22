package cn.cccs7.cynexus.ai.infra.repository.capability;

import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.domain.policy.capability.AgentCapabilityDescriptorPolicy;
import cn.cccs7.cynexus.ai.domain.event.capabilityaudit.AgentCapabilityAuditEvent;
import cn.cccs7.cynexus.ai.domain.repository.capability.AgentCapabilityRepository;
import cn.cccs7.cynexus.ai.infra.basic.entity.capability.AgentCapabilityAuditPO;
import cn.cccs7.cynexus.ai.infra.basic.service.impl.capability.AgentCapabilityAuditBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.capability.AgentCapabilityBasicService;
import cn.cccs7.cynexus.ai.infra.convert.capability.AgentCapabilityPOConverter;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AgentCapabilityRepositoryImpl implements AgentCapabilityRepository {
    private final AgentCapabilityBasicService service;
    private final AgentCapabilityAuditBasicService auditService;

    public AgentCapabilityRepositoryImpl(AgentCapabilityBasicService service, AgentCapabilityAuditBasicService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void synchronize(List<AgentCapabilityDescriptor> descriptors) {
        OffsetDateTime now = OffsetDateTime.now();
        List<AgentCapabilityDescriptor> previous = service.listAll().stream()
                .map(AgentCapabilityPOConverter.INSTANCE::toTarget).toList();
        service.markUnseenUnavailable(now);
        java.util.Set<String> currentVersions = descriptors.stream()
                .filter(java.util.Objects::nonNull)
                .map(item -> item.getCapabilityCode() + "@" + item.getVersion())
                .collect(java.util.stream.Collectors.toSet());
        previous.stream().filter(item -> AgentCapabilityDescriptorPolicy.HANDLER_REGISTRATION.equals(
                        AgentCapabilityDescriptorPolicy.normalizeRegistrationMode(item.getRegistrationMode())))
                .filter(item -> !currentVersions.contains(item.getCapabilityCode() + "@" + item.getVersion()))
                .forEach(item -> record(item.toBuilder().enabled(false).availability("STALE").governanceAvailability(item.getGovernanceAvailability() == null ? "AVAILABLE" : item.getGovernanceAvailability()).build(),
                        "STALE", "Handler descriptor was not present during synchronization"));
        for (AgentCapabilityDescriptor descriptor : descriptors) {
            AgentCapabilityDescriptorPolicy.validate(descriptor);
            AgentCapabilityDescriptor seen = descriptor.toBuilder().registrationMode(AgentCapabilityDescriptorPolicy.HANDLER_REGISTRATION)
                    .lastSeenAt(now).staleAt(now.plusHours(2)).build();
            service.markOlderVersionsUnavailable(seen.getCapabilityCode(), seen.getVersion());
            previous.stream().filter(item -> seen.getCapabilityCode().equals(item.getCapabilityCode())
                    && !seen.getVersion().equals(item.getVersion()))
                    .forEach(item -> record(item.toBuilder().enabled(false).availability("STALE").governanceAvailability(item.getGovernanceAvailability() == null ? "AVAILABLE" : item.getGovernanceAvailability()).build(),
                            "VERSION_RETIRED", "A newer handler descriptor was synchronized"));
            var existing = service.find(seen.getCapabilityCode(), seen.getVersion());
            if (existing == null) {
                service.save(AgentCapabilityPOConverter.INSTANCE.toSource(seen));
                record(seen, "REGISTERED", "Handler descriptor synchronized");
            } else {
                var update = AgentCapabilityPOConverter.INSTANCE.toSource(seen);
                update.setId(existing.getId());
                update.setCreatedAt(existing.getCreatedAt());
                update.setEnabled(existing.getEnabled());
                // Runtime availability belongs to the live handler snapshot. Keep only
                // governance decisions from the persisted row.
                update.setAvailability(seen.getAvailability());
                update.setGovernanceAvailability(existing.getGovernanceAvailability());
                update.setPriority(existing.getPriority());
                service.update(update);
            }
        }
    }

    @Override public List<AgentCapabilityDescriptor> listAll() {
        return service.listAll().stream().map(AgentCapabilityPOConverter.INSTANCE::toTarget).toList();
    }

    @Override public List<AgentCapabilityDescriptor> listRuntimeCapabilities() {
        return service.listRuntime().stream().map(AgentCapabilityPOConverter.INSTANCE::toTarget).toList();
    }

    @Override public boolean updateGovernance(String capabilityCode, boolean enabled, String availability, int priority) {
        AgentCapabilityDescriptorPolicy.validateGovernance(availability, priority);
        boolean updated = service.updateGovernance(capabilityCode, enabled, availability, priority);
        if (updated) {
            record(AgentCapabilityDescriptor.builder().capabilityCode(capabilityCode).version("unknown")
                    .enabled(enabled).governanceAvailability(AgentCapabilityDescriptorPolicy.normalizeAvailability(availability))
                    .availability("AVAILABLE").priority(priority).build(), "GOVERNANCE_UPDATED", "Provider page governance update");
        }
        return updated;
    }

    @Override
    @Transactional
    public AgentCapabilityDescriptor register(AgentCapabilityDescriptor descriptor) {
        AgentCapabilityDescriptorPolicy.validateRegistration(descriptor);
        AgentCapabilityDescriptor source = descriptor.toBuilder()
                .registrationMode(AgentCapabilityDescriptorPolicy.ADMIN_REGISTRATION)
                .lastSeenAt(OffsetDateTime.now()).staleAt(null).build();
        var existing = service.find(source.getCapabilityCode(), source.getVersion());
        if (existing != null && !AgentCapabilityDescriptorPolicy.ADMIN_REGISTRATION.equals(
                AgentCapabilityDescriptorPolicy.normalizeRegistrationMode(existing.getRegistrationMode()))) {
            throw new IllegalArgumentException("Capability code/version is owned by a built-in handler");
        }
        var target = AgentCapabilityPOConverter.INSTANCE.toSource(source);
        String action = "REGISTERED";
        if (existing == null) {
            service.save(target);
        } else {
            target.setId(existing.getId());
            target.setCreatedAt(existing.getCreatedAt());
            service.update(target);
            action = "UPDATED";
        }
        record(source, action, "Administrator hot-add registration");
        return AgentCapabilityPOConverter.INSTANCE.toTarget(target);
    }

    private void record(AgentCapabilityDescriptor descriptor, String action, String reason) {
        AgentCapabilityAuditPO audit = new AgentCapabilityAuditPO();
        audit.setCapabilityCode(descriptor.getCapabilityCode());
        audit.setVersion(descriptor.getVersion());
        audit.setAction(action);
        audit.setEnabled(descriptor.isEnabled());
        audit.setAvailability(descriptor.getAvailability());
        audit.setGovernanceAvailability(descriptor.getGovernanceAvailability());
        audit.setPriority(descriptor.getPriority());
        audit.setReason(reason);
        auditService.save(audit);
    }
}
