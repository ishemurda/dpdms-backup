package zw.ac.uz.dpdms.common;

public record UserClaim(
    String username,
    Role role,
    HazardType hazard,
    String ward
) {}
