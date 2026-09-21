package zw.ac.uz.dpdms.auth;

import zw.ac.uz.dpdms.common.HazardType;
import zw.ac.uz.dpdms.common.Role;

public record UserAccount(
    String username,
    String password,
    Role role,
    HazardType hazard,
    String ward,
    String fullName
) {}
