package zw.ac.uz.dpdms.fire.domain;

import zw.ac.uz.dpdms.common.AuditEntry;
import zw.ac.uz.dpdms.common.IncidentMetadata;
import zw.ac.uz.dpdms.common.IncidentStatus;

import java.util.List;
import java.util.UUID;

public record FireIncident(
    UUID id,
    IncidentMetadata metadata,
    double areaBurnedHectares,
    String suspectedCause,
    int casualties,
    int structuresDestroyed,
    String fireState,
    IncidentStatus status,
    List<AuditEntry> auditTrail
) {}
