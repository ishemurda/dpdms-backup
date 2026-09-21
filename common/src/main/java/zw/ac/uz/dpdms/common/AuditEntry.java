package zw.ac.uz.dpdms.common;

import java.time.Instant;

public record AuditEntry(
    Instant timestamp,
    String actor,
    IncidentStatus fromStatus,
    IncidentStatus toStatus,
    String reason
) {}
