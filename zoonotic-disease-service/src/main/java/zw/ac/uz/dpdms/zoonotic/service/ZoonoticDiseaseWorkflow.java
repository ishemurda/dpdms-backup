package zw.ac.uz.dpdms.zoonotic.service;

import org.springframework.stereotype.Service;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.zoonotic.api.ZoonoticDiseaseRequest;
import zw.ac.uz.dpdms.zoonotic.domain.ZoonoticDiseaseIncident;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ZoonoticDiseaseWorkflow {
    private final Map<UUID, ZoonoticDiseaseIncident> incidents = new ConcurrentHashMap<>();

    public ZoonoticDiseaseIncident submit(ZoonoticDiseaseRequest r, UserClaim actor) {
        RbacEnforcer.assertCanCreate(actor, HazardType.ZOONOTIC_DISEASE, r.metadata().ward());
        UUID id = UUID.randomUUID();
        var audit = List.of(new AuditEntry(Instant.now(), actor.username(), null, IncidentStatus.PENDING, "Submitted for approval"));
        var incident = new ZoonoticDiseaseIncident(
            id,
            r.metadata(),
            r.pathogen(),
            r.animalSpecies(),
            r.humanCases(),
            r.animalCases(),
            r.classification(),
            IncidentStatus.PENDING,
            audit
        );
        incidents.put(id, incident);
        return incident;
    }

    public ZoonoticDiseaseIncident update(UUID id, ZoonoticDiseaseRequest r, UserClaim actor) {
        var current = getRaw(id);
        RbacEnforcer.assertCanUpdate(actor, HazardType.ZOONOTIC_DISEASE, current.metadata().ward(), current.metadata().reporterId(), current.status());

        var audit = new ArrayList<>(current.auditTrail());
        audit.add(new AuditEntry(Instant.now(), actor.username(), current.status(), IncidentStatus.PENDING, "Corrected and resubmitted"));

        var updated = new ZoonoticDiseaseIncident(
            current.id(),
            r.metadata(),
            r.pathogen(),
            r.animalSpecies(),
            r.humanCases(),
            r.animalCases(),
            r.classification(),
            IncidentStatus.PENDING,
            List.copyOf(audit)
        );
        incidents.put(id, updated);
        return updated;
    }

    public void delete(UUID id, UserClaim actor) {
        var current = getRaw(id);
        RbacEnforcer.assertCanDelete(actor, current.metadata().reporterId(), current.status());
        incidents.remove(id);
    }

    public ZoonoticDiseaseIncident transition(UUID id, IncidentStatus next, UserClaim actor, String reason) {
        var current = getRaw(id);
        RbacEnforcer.assertCanDecide(actor, HazardType.ZOONOTIC_DISEASE);

        if (current.status() != IncidentStatus.PENDING && current.status() != IncidentStatus.CORRECTION_REQUESTED) {
            throw new IllegalStateException("Incident is in terminal status (" + current.status() + ") and cannot be transitioned");
        }
        if (next != IncidentStatus.APPROVED && next != IncidentStatus.REJECTED && next != IncidentStatus.CORRECTION_REQUESTED) {
            throw new IllegalArgumentException("Unsupported transition to " + next);
        }
        if ((next == IncidentStatus.REJECTED || next == IncidentStatus.CORRECTION_REQUESTED) && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A reason must be provided when rejecting or requesting corrections");
        }

        String effectiveReason = (reason != null && !reason.isBlank()) ? reason : "Approved for publication";
        var audit = new ArrayList<>(current.auditTrail());
        audit.add(new AuditEntry(Instant.now(), actor.username(), current.status(), next, effectiveReason));

        var changed = new ZoonoticDiseaseIncident(
            current.id(),
            current.metadata(),
            current.pathogen(),
            current.animalSpecies(),
            current.humanCases(),
            current.animalCases(),
            current.classification(),
            next,
            List.copyOf(audit)
        );
        incidents.put(id, changed);
        return changed;
    }

    public ZoonoticDiseaseIncident get(UUID id, UserClaim actor) {
        var item = getRaw(id);
        if (!RbacEnforcer.canViewIncident(actor, HazardType.ZOONOTIC_DISEASE, item.status(), item.metadata().reporterId())) {
            throw new ForbiddenException("Not authorized to view incident outside your remit");
        }
        return item;
    }

    public ZoonoticDiseaseIncident get(UUID id) {
        return getRaw(id);
    }

    private ZoonoticDiseaseIncident getRaw(UUID id) {
        var item = incidents.get(id);
        if (item == null) {
            throw new NoSuchElementException("Zoonotic disease incident not found");
        }
        return item;
    }

    public List<ZoonoticDiseaseIncident> list(UserClaim actor) {
        return incidents.values().stream()
            .filter(i -> RbacEnforcer.canViewIncident(actor, HazardType.ZOONOTIC_DISEASE, i.status(), i.metadata().reporterId()))
            .toList();
    }

    public List<ZoonoticDiseaseIncident> approved() {
        return incidents.values().stream()
            .filter(i -> i.status() == IncidentStatus.APPROVED)
            .toList();
    }
}
