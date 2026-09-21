package zw.ac.uz.dpdms.auth;

import zw.ac.uz.dpdms.common.HazardType;
import zw.ac.uz.dpdms.common.Role;

public record LoginResponse(
    String token,
    String username,
    Role role,
    HazardType hazard,
    String ward,
    String fullName
) {}
