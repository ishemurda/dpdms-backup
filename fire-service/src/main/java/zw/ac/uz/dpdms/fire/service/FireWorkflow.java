package zw.ac.uz.dpdms.fire.service;

import org.springframework.stereotype.Service;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.fire.api.FireRequest;
import zw.ac.uz.dpdms.fire.domain.FireIncident;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FireWorkflow {
    private final Map<UUID, FireIncident> incidents = new ConcurrentHashMap<>();

    public FireIncident submit(FireRequest r, UserClaim actor) {
        RbacEnforcer.assertCanCreate(actor, HazardType.FIRE, r.metadata().ward());
        UUID id = UUID.randomUUID();
        var audit = List.of(new AuditEntry(Instant.now(), actor.username(), null, IncidentStatus.PENDING, "Submitted for approval"));
        var incident = new FireIncident(
            id,
            r.metadata(),
            r.areaBurnedHectares(),
            r.suspectedCause(),
            r.casualties(),
            r.structuresDestroyed(),
            r.fireState(),
            IncidentStatus.PENDING,
            audit
        );
        incidents.put(id, incident);
        return incident;
    }

    public FireIncident update(UUID id, FireRequest r, UserClaim actor) {
        var current = getRaw(id);
        RbacEnforcer.assertCanUpdate(actor, HazardType.FIRE, current.metadata().ward(), current.metadata().reporterId(), current.status());

        var audit = new ArrayList<>(current.auditTrail());
        audit.add(new AuditEntry(Instant.now(), actor.username(), current.status(), IncidentStatus.PENDING, "Corrected and resubmitted"));

        var updated = new FireIncident(
            current.id(),
            r.metadata(),
            r.areaBurnedHectares(),
            r.suspectedCause(),
            r.casualties(),
            r.structuresDestroyed(),
            r.fireState(),
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

    public FireIncident transition(UUID id, IncidentStatus next, UserClaim actor, String reason) {
        var current = getRaw(id);
        RbacEnforcer.assertCanDecide(actor, HazardType.FIRE);

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

        var changed = new FireIncident(
            current.id(),
            current.metadata(),
            current.areaBurnedHectares(),
            current.suspectedCause(),
            current.casualties(),
            current.structuresDestroyed(),
            current.fireState(),
            next,
            List.copyOf(audit)
        );
        incidents.put(id, changed);
        return changed;
    }

    public FireIncident get(UUID id, UserClaim actor) {
        var item = getRaw(id);
        if (!RbacEnforcer.canViewIncident(actor, HazardType.FIRE, item.status(), item.metadata().reporterId())) {
            throw new ForbiddenException("Not authorized to view incident outside your remit");
        }
        return item;
    }

    public FireIncident get(UUID id) {
        return getRaw(id);
    }

    private FireIncident getRaw(UUID id) {
        var item = incidents.get(id);
        if (item == null) {
            throw new NoSuchElementException("Fire incident not found");
        }
        return item;
    }

    public List<FireIncident> list(UserClaim actor) {
        return incidents.values().stream()
            .filter(i -> RbacEnforcer.canViewIncident(actor, HazardType.FIRE, i.status(), i.metadata().reporterId()))
            .toList();
    }

    public List<FireIncident> approved() {
        return incidents.values().stream()
            .filter(i -> i.status() == IncidentStatus.APPROVED)
            .toList();
    }
}
