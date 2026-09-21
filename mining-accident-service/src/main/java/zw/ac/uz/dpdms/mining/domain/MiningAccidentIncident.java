package zw.ac.uz.dpdms.mining.domain;

import zw.ac.uz.dpdms.common.AuditEntry;
import zw.ac.uz.dpdms.common.IncidentMetadata;
import zw.ac.uz.dpdms.common.IncidentStatus;

import java.util.List;
import java.util.UUID;

public record MiningAccidentIncident(
    UUID id,
    IncidentMetadata metadata,
    String mineNameAndType,
    String accidentType,
    int trappedOrInjured,
    int fatalities,
    String rescueStatus,
    IncidentStatus status,
    List<AuditEntry> auditTrail
) {}
