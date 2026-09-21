package zw.ac.uz.dpdms.dashboard;

/** Lightweight DTO used to represent an approved incident from any hazard service. */
public record IncidentSummary(
        String id,
        String hazard,
        String ward,
        String district,
        String severity,
        double latitude,
        double longitude,
        String occurredAt
) {}
