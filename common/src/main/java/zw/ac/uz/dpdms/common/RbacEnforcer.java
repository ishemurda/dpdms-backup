package zw.ac.uz.dpdms.common;

public final class RbacEnforcer {

    private RbacEnforcer() {}

    public static void assertCanCreate(UserClaim user, HazardType serviceHazard, String incidentWard) {
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (user.role() == Role.NATIONAL_VIEWER) {
            throw new ForbiddenException("National viewers have read-only access and cannot capture incidents");
        }
        if (user.role() != Role.WARD_RECORDER) {
            throw new ForbiddenException("Only ward recorders may submit new incidents");
        }
        if (user.hazard() != serviceHazard) {
            throw new ForbiddenException("Recorder for hazard " + user.hazard() + " cannot record " + serviceHazard + " incidents");
        }
        if (user.ward() == null || !user.ward().equalsIgnoreCase(incidentWard)) {
            throw new ForbiddenException("Recorder assigned to " + user.ward() + " cannot submit for " + incidentWard);
        }
    }

    public static void assertCanUpdate(UserClaim user, HazardType serviceHazard, String incidentWard, String originalReporter, IncidentStatus currentStatus) {
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (user.role() == Role.NATIONAL_VIEWER) {
            throw new ForbiddenException("National viewers have read-only access");
        }
        if (currentStatus != IncidentStatus.PENDING && currentStatus != IncidentStatus.CORRECTION_REQUESTED) {
            throw new IllegalStateException("Incident is in terminal state (" + currentStatus + ") and cannot be modified");
        }
        if (user.role() != Role.WARD_RECORDER && user.role() != Role.PROVINCIAL_ADMIN) {
            throw new ForbiddenException("Only the original ward recorder or administrator may edit this incident");
        }
        if (user.role() == Role.WARD_RECORDER) {
            if (user.hazard() != serviceHazard) {
                throw new ForbiddenException("Cross-hazard modification denied");
            }
            if (user.ward() == null || !user.ward().equalsIgnoreCase(incidentWard)) {
                throw new ForbiddenException("Ward mismatch for incident modification");
            }
            if (originalReporter != null && !originalReporter.equalsIgnoreCase(user.username())) {
                throw new ForbiddenException("Only the original recorder may edit this submission");
            }
        }
    }

    public static void assertCanDelete(UserClaim user, String originalReporter, IncidentStatus currentStatus) {
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (user.role() == Role.NATIONAL_VIEWER) {
            throw new ForbiddenException("National viewers have read-only access");
        }
        if (currentStatus != IncidentStatus.PENDING) {
            throw new IllegalStateException("Only pending incidents can be deleted");
        }
        if (user.role() == Role.PROVINCIAL_ADMIN) {
            return;
        }
        if (user.role() == Role.WARD_RECORDER) {
            if (originalReporter != null && originalReporter.equalsIgnoreCase(user.username())) {
                return;
            }
            throw new ForbiddenException("Only the original author can delete this pending incident");
        }
        throw new ForbiddenException("Insufficient permissions to delete incident");
    }

    public static void assertCanDecide(UserClaim user, HazardType serviceHazard) {
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (user.role() == Role.NATIONAL_VIEWER) {
            throw new ForbiddenException("National viewers have read-only access and cannot make decisions");
        }
        if (user.role() == Role.WARD_RECORDER) {
            throw new ForbiddenException("Ward recorders cannot approve, reject, or request corrections");
        }
        if (user.role() == Role.PROVINCIAL_ADMIN) {
            return; // Provincial admin can supervise across hazards
        }
        if (user.role() == Role.PROVINCIAL_SUPERVISOR) {
            if (user.hazard() != serviceHazard) {
                throw new ForbiddenException("Supervisor for " + user.hazard() + " cannot review " + serviceHazard + " incidents");
            }
            return;
        }
        throw new ForbiddenException("Unauthorized to make workflow decisions");
    }

    public static boolean canViewIncident(UserClaim user, HazardType serviceHazard, IncidentStatus status, String reporter) {
        if (status == IncidentStatus.APPROVED) {
            return true; // Approved records are visible to all authorized users
        }
        if (user == null) {
            return false;
        }
        if (user.role() == Role.NATIONAL_VIEWER) {
            return false; // National viewer can only view approved records
        }
        if (user.role() == Role.PROVINCIAL_ADMIN) {
            return true;
        }
        if (user.role() == Role.PROVINCIAL_SUPERVISOR && user.hazard() == serviceHazard) {
            return true;
        }
        if (user.role() == Role.WARD_RECORDER && user.hazard() == serviceHazard && reporter != null && reporter.equalsIgnoreCase(user.username())) {
            return true;
        }
        return false;
    }
}
