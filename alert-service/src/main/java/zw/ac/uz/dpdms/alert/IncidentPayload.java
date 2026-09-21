package zw.ac.uz.dpdms.alert;

import java.util.Map;

/**
 * Generic incident payload posted by hazard services or the gateway when an incident is approved.
 * Uses a flexible fields map to avoid coupling to any specific hazard domain object.
 */
public record IncidentPayload(
        String id,
        String hazard,
        String ward,
        String severity,
        Map<String, Object> fields
) {}
