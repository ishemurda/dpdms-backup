package zw.ac.uz.dpdms.common;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static UserClaim resolveUser(String authHeader, String actorId, String roleStr, String hazardStr, String wardStr) {
        if (authHeader != null && !authHeader.isBlank()) {
            return JwtUtils.parseToken(authHeader, JwtUtils.DEFAULT_SECRET);
        }
        if (actorId != null && !actorId.isBlank()) {
            Role role = roleStr != null && !roleStr.isBlank() ? Role.valueOf(roleStr) : Role.WARD_RECORDER;
            HazardType hazard = hazardStr != null && !hazardStr.isBlank() ? HazardType.valueOf(hazardStr) : null;
            return new UserClaim(actorId, role, hazard, wardStr);
        }
        throw new ForbiddenException("Missing authentication credentials");
    }
}
