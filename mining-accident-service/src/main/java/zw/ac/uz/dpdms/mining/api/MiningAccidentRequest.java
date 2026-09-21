package zw.ac.uz.dpdms.mining.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import zw.ac.uz.dpdms.common.IncidentMetadata;

public record MiningAccidentRequest(
    @NotNull @Valid IncidentMetadata metadata,
    @NotBlank String mineNameAndType,
    @NotBlank String accidentType,
    @NotNull @PositiveOrZero Integer trappedOrInjured,
    @NotNull @PositiveOrZero Integer fatalities,
    @NotBlank String rescueStatus
) {}
