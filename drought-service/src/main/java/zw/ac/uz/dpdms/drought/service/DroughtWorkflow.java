package zw.ac.uz.dpdms.drought.service;

import org.springframework.stereotype.Service;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.drought.api.DroughtRequest;
import zw.ac.uz.dpdms.drought.domain.DroughtIncident;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DroughtWorkflow {
    private final Map<UUID, DroughtIncident> incidents = new ConcurrentHashMap<>();

    public DroughtIncident submit(DroughtRequest r, UserClaim actor) {
        RbacEnforcer.assertCanCreate(actor, HazardType.DROUGHT, r.metadata().ward());
        UUID id = UUID.randomUUID();
        var audit = List.of(new AuditEntry(Instant.now(), actor.username(), null, IncidentStatus.PENDING, "Submitted for approval"));
        var incident = new DroughtIncident(
            id,
            r.metadata(),
            r.rainfallDeficitMm(),
            r.consecutiveDryDays(),
            r.cropFailurePercent(),
            r.peopleWaterShortage(),
            r.livestockMortality(),
            IncidentStatus.PENDING,
            audit
        );
        incidents.put(id, incident);
        return incident;
    }

    public DroughtIncident update(UUID id, DroughtRequest r, UserClaim actor) {
        var current = getRaw(id);
        RbacEnforcer.assertCanUpdate(actor, HazardType.DROUGHT, current.metadata().ward(), current.metadata().reporterId(), current.status());

        var audit = new ArrayList<>(current.auditTrail());
        audit.add(new AuditEntry(Instant.now(), actor.username(), current.status(), IncidentStatus.PENDING, "Corrected and resubmitted"));

        var updated = new DroughtIncident(
            current.id(),
            r.metadata(),
            r.rainfallDeficitMm(),
            r.consecutiveDryDays(),
            r.cropFailurePercent(),
            r.peopleWaterShortage(),
            r.livestockMortality(),
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

    public DroughtIncident transition(UUID id, IncidentStatus next, UserClaim actor, String reason) {
        var current = getRaw(id);
        RbacEnforcer.assertCanDecide(actor, HazardType.DROUGHT);

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

        var changed = new DroughtIncident(
            current.id(),
            current.metadata(),
            current.rainfallDeficitMm(),
            current.consecutiveDryDays(),
            current.cropFailurePercent(),
            current.peopleWaterShortage(),
            current.livestockMortality(),
            next,
            List.copyOf(audit)
        );
        incidents.put(id, changed);
        return changed;
    }

    public DroughtIncident get(UUID id, UserClaim actor) {
        var item = getRaw(id);
        if (!RbacEnforcer.canViewIncident(actor, HazardType.DROUGHT, item.status(), item.metadata().reporterId())) {
            throw new ForbiddenException("Not authorized to view incident outside your remit");
        }
        return item;
    }

    public DroughtIncident get(UUID id) {
        return getRaw(id);
    }

    private DroughtIncident getRaw(UUID id) {
        var item = incidents.get(id);
        if (item == null) {
            throw new NoSuchElementException("Drought incident not found");
        }
        return item;
    }

    public List<DroughtIncident> list(UserClaim actor) {
        return incidents.values().stream()
            .filter(i -> RbacEnforcer.canViewIncident(actor, HazardType.DROUGHT, i.status(), i.metadata().reporterId()))
            .toList();
    }

    public List<DroughtIncident> approved() {
        return incidents.values().stream()
            .filter(i -> i.status() == IncidentStatus.APPROVED)
            .toList();
    }
}
