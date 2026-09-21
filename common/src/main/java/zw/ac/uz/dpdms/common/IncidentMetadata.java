package zw.ac.uz.dpdms.common;
import jakarta.validation.constraints.*;
import java.time.Instant;
public record IncidentMetadata(@NotBlank String ward, @NotBlank String district, @NotBlank String province, @NotNull Instant occurredAt, @NotBlank String reporterId, @NotNull Severity severity, @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude, @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {}
