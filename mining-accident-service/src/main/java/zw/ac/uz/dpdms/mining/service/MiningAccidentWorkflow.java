package zw.ac.uz.dpdms.mining.service;

import org.springframework.stereotype.Service;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.mining.api.MiningAccidentRequest;
import zw.ac.uz.dpdms.mining.domain.MiningAccidentIncident;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MiningAccidentWorkflow {
    private final Map<UUID, MiningAccidentIncident> incidents = new ConcurrentHashMap<>();

    public MiningAccidentIncident submit(MiningAccidentRequest r, UserClaim actor) {
        RbacEnforcer.assertCanCreate(actor, HazardType.MINING_ACCIDENT, r.metadata().ward());
        UUID id = UUID.randomUUID();
        var audit = List.of(new AuditEntry(Instant.now(), actor.username(), null, IncidentStatus.PENDING, "Submitted for approval"));
        var incident = new MiningAccidentIncident(
            id,
            r.metadata(),
            r.mineNameAndType(),
            r.accidentType(),
            r.trappedOrInjured(),
            r.fatalities(),
            r.rescueStatus(),
            IncidentStatus.PENDING,
            audit
        );
        incidents.put(id, incident);
        return incident;
    }

    public MiningAccidentIncident update(UUID id, MiningAccidentRequest r, UserClaim actor) {
        var current = getRaw(id);
        RbacEnforcer.assertCanUpdate(actor, HazardType.MINING_ACCIDENT, current.metadata().ward(), current.metadata().reporterId(), current.status());

        var audit = new ArrayList<>(current.auditTrail());
        audit.add(new AuditEntry(Instant.now(), actor.username(), current.status(), IncidentStatus.PENDING, "Corrected and resubmitted"));

        var updated = new MiningAccidentIncident(
            current.id(),
            r.metadata(),
            r.mineNameAndType(),
            r.accidentType(),
            r.trappedOrInjured(),
            r.fatalities(),
            r.rescueStatus(),
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

    public MiningAccidentIncident transition(UUID id, IncidentStatus next, UserClaim actor, String reason) {
        var current = getRaw(id);
        RbacEnforcer.assertCanDecide(actor, HazardType.MINING_ACCIDENT);

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

        var changed = new MiningAccidentIncident(
            current.id(),
            current.metadata(),
            current.mineNameAndType(),
            current.accidentType(),
            current.trappedOrInjured(),
            current.fatalities(),
            current.rescueStatus(),
            next,
            List.copyOf(audit)
        );
        incidents.put(id, changed);
        return changed;
    }

    public MiningAccidentIncident get(UUID id, UserClaim actor) {
        var item = getRaw(id);
        if (!RbacEnforcer.canViewIncident(actor, HazardType.MINING_ACCIDENT, item.status(), item.metadata().reporterId())) {
            throw new ForbiddenException("Not authorized to view incident outside your remit");
        }
        return item;
    }

    public MiningAccidentIncident get(UUID id) {
        return getRaw(id);
    }

    private MiningAccidentIncident getRaw(UUID id) {
        var item = incidents.get(id);
        if (item == null) {
            throw new NoSuchElementException("Mining accident incident not found");
        }
        return item;
    }

    public List<MiningAccidentIncident> list(UserClaim actor) {
        return incidents.values().stream()
            .filter(i -> RbacEnforcer.canViewIncident(actor, HazardType.MINING_ACCIDENT, i.status(), i.metadata().reporterId()))
            .toList();
    }

    public List<MiningAccidentIncident> approved() {
        return incidents.values().stream()
            .filter(i -> i.status() == IncidentStatus.APPROVED)
            .toList();
    }
}
