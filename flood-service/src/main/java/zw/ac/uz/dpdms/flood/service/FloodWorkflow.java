package zw.ac.uz.dpdms.flood.service;

import org.springframework.stereotype.Service;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.flood.api.FloodRequest;
import zw.ac.uz.dpdms.flood.domain.FloodIncident;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FloodWorkflow {
    private final Map<UUID, FloodIncident> incidents = new ConcurrentHashMap<>();

    public FloodIncident submit(FloodRequest r, String actor, String actorWard) {
        UserClaim claim = new UserClaim(actor, Role.WARD_RECORDER, HazardType.FLOOD, actorWard);
        return submit(r, claim);
    }

    public FloodIncident submit(FloodRequest r, UserClaim actor) {
        RbacEnforcer.assertCanCreate(actor, HazardType.FLOOD, r.metadata().ward());
        UUID id = UUID.randomUUID();
        var audit = List.of(new FloodIncident.AuditEntry(Instant.now(), actor.username(), null, IncidentStatus.PENDING, "Submitted for approval"));
        var incident = new FloodIncident(
            id,
            r.metadata(),
            r.peakWaterLevelMetres(),
            r.riverBasin(),
            r.householdsDisplaced(),
            r.floodedAreaHectares(),
            r.inundationDays(),
            IncidentStatus.PENDING,
            audit
        );
        incidents.put(id, incident);
        return incident;
    }

    public FloodIncident update(UUID id, FloodRequest r, UserClaim actor) {
        var current = getRaw(id);
        RbacEnforcer.assertCanUpdate(actor, HazardType.FLOOD, current.metadata().ward(), current.metadata().reporterId(), current.status());

        var audit = new ArrayList<>(current.auditTrail());
        audit.add(new FloodIncident.AuditEntry(Instant.now(), actor.username(), current.status(), IncidentStatus.PENDING, "Corrected and resubmitted"));

        var updated = new FloodIncident(
            current.id(),
            r.metadata(),
            r.peakWaterLevelMetres(),
            r.riverBasin(),
            r.householdsDisplaced(),
            r.floodedAreaHectares(),
            r.inundationDays(),
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

    public FloodIncident transition(UUID id, IncidentStatus next, String actor, String reason) {
        UserClaim claim = new UserClaim(actor, Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null);
        return transition(id, next, claim, reason);
    }

    public FloodIncident transition(UUID id, IncidentStatus next, UserClaim actor, String reason) {
        var current = getRaw(id);
        RbacEnforcer.assertCanDecide(actor, HazardType.FLOOD);

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
        audit.add(new FloodIncident.AuditEntry(Instant.now(), actor.username(), current.status(), next, effectiveReason));

        var changed = new FloodIncident(
            current.id(),
            current.metadata(),
            current.peakWaterLevelMetres(),
            current.riverBasin(),
            current.householdsDisplaced(),
            current.floodedAreaHectares(),
            current.inundationDays(),
            next,
            List.copyOf(audit)
        );
        incidents.put(id, changed);
        return changed;
    }

    public FloodIncident get(UUID id, UserClaim actor) {
        var item = getRaw(id);
        if (!RbacEnforcer.canViewIncident(actor, HazardType.FLOOD, item.status(), item.metadata().reporterId())) {
            throw new ForbiddenException("Not authorized to view pending or rejected incident outside your remit");
        }
        return item;
    }

    public FloodIncident get(UUID id) {
        return getRaw(id);
    }

    private FloodIncident getRaw(UUID id) {
        var item = incidents.get(id);
        if (item == null) {
            throw new NoSuchElementException("Flood incident not found");
        }
        return item;
    }

    public List<FloodIncident> list(UserClaim actor) {
        return incidents.values().stream()
            .filter(i -> RbacEnforcer.canViewIncident(actor, HazardType.FLOOD, i.status(), i.metadata().reporterId()))
            .toList();
    }

    public List<FloodIncident> approved() {
        return incidents.values().stream()
            .filter(i -> i.status() == IncidentStatus.APPROVED)
            .toList();
    }

    public static class ForbiddenException extends zw.ac.uz.dpdms.common.ForbiddenException {
        public ForbiddenException(String m) {
            super(m);
        }
    }
}
