package zw.ac.uz.dpdms.drought.domain;

import zw.ac.uz.dpdms.common.AuditEntry;
import zw.ac.uz.dpdms.common.IncidentMetadata;
import zw.ac.uz.dpdms.common.IncidentStatus;

import java.util.List;
import java.util.UUID;

public record DroughtIncident(
    UUID id,
    IncidentMetadata metadata,
    double rainfallDeficitMm,
    int consecutiveDryDays,
    double cropFailurePercent,
    int peopleWaterShortage,
    int livestockMortality,
    IncidentStatus status,
    List<AuditEntry> auditTrail
) {}
