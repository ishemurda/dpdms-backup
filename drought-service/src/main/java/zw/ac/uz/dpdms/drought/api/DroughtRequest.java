package zw.ac.uz.dpdms.drought.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import zw.ac.uz.dpdms.common.IncidentMetadata;

public record DroughtRequest(
    @NotNull @Valid IncidentMetadata metadata,
    @NotNull @PositiveOrZero Double rainfallDeficitMm,
    @NotNull @PositiveOrZero Integer consecutiveDryDays,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double cropFailurePercent,
    @NotNull @PositiveOrZero Integer peopleWaterShortage,
    @NotNull @PositiveOrZero Integer livestockMortality
) {}
