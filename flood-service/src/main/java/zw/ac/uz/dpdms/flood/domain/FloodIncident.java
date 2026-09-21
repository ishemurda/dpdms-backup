package zw.ac.uz.dpdms.flood.domain;
import zw.ac.uz.dpdms.common.*; import java.time.*; import java.util.*;
public record FloodIncident(UUID id, IncidentMetadata metadata, double peakWaterLevelMetres, String riverBasin, int householdsDisplaced, double floodedAreaHectares, int inundationDays, IncidentStatus status, List<AuditEntry> auditTrail) {
  public record AuditEntry(Instant at, String actorId, IncidentStatus from, IncidentStatus to, String reason) {}
}
