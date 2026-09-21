package zw.ac.uz.dpdms.dashboard;

/** Count of approved incidents per hazard type. */
public record HazardSummary(String hazard, int approvedCount) {}
