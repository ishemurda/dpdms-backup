package zw.ac.uz.dpdms.zoonotic.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import zw.ac.uz.dpdms.common.IncidentMetadata;

public record ZoonoticDiseaseRequest(
    @NotNull @Valid IncidentMetadata metadata,
    @NotBlank String pathogen,
    @NotBlank String animalSpecies,
    @NotNull @PositiveOrZero Integer humanCases,
    @NotNull @PositiveOrZero Integer animalCases,
    @NotBlank String classification
) {}
