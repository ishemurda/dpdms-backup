package zw.ac.uz.dpdms.zoonotic.domain;

import zw.ac.uz.dpdms.common.AuditEntry;
import zw.ac.uz.dpdms.common.IncidentMetadata;
import zw.ac.uz.dpdms.common.IncidentStatus;

import java.util.List;
import java.util.UUID;

public record ZoonoticDiseaseIncident(
    UUID id,
    IncidentMetadata metadata,
    String pathogen,
    String animalSpecies,
    int humanCases,
    int animalCases,
    String classification,
    IncidentStatus status,
    List<AuditEntry> auditTrail
) {}
