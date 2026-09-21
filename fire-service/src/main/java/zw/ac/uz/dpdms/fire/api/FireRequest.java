package zw.ac.uz.dpdms.fire.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import zw.ac.uz.dpdms.common.IncidentMetadata;

public record FireRequest(
    @NotNull @Valid IncidentMetadata metadata,
    @NotNull @Positive Double areaBurnedHectares,
    @NotBlank String suspectedCause,
    @NotNull @PositiveOrZero Integer casualties,
    @NotNull @PositiveOrZero Integer structuresDestroyed,
    @NotBlank String fireState
) {}
