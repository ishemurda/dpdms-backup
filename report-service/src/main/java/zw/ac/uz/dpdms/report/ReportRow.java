package zw.ac.uz.dpdms.report;

import java.util.Map;

/** Flattened representation of an approved incident used in reports. */
public record ReportRow(
        String id,
        String hazard,
        String ward,
        String district,
        String severity,
        String occurredAt,
        double latitude,
        double longitude,
        Map<String, Object> hazardFields
) {}
