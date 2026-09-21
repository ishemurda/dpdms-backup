package zw.ac.uz.dpdms.flood.api;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import zw.ac.uz.dpdms.common.IncidentMetadata;
public record FloodRequest(@Valid @NotNull IncidentMetadata metadata, @PositiveOrZero double peakWaterLevelMetres, @NotBlank String riverBasin, @PositiveOrZero int householdsDisplaced, @PositiveOrZero double floodedAreaHectares, @PositiveOrZero int inundationDays) {}
