package zw.ac.uz.dpdms.dashboard;

import java.time.Instant;
import java.util.List;

/** Top-level response DTO returned by /api/dashboard/summary. */
public record DashboardSummary(
        int totalApproved,
        List<HazardSummary> byHazard,
        Instant generatedAt
) {}
