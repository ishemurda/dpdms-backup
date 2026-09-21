package zw.ac.uz.dpdms.alert;

import java.time.Instant;

/** Immutable record representing a single triggered alert. */
public record AlertLogEntry(
        String incidentId,
        String hazard,
        String ward,
        String severity,
        String reason,
        Instant triggeredAt
) {}
