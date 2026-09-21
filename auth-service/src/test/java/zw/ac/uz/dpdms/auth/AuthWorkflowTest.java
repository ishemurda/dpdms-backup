package zw.ac.uz.dpdms.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.ForbiddenException;
import zw.ac.uz.dpdms.common.HazardType;
import zw.ac.uz.dpdms.common.Role;
import zw.ac.uz.dpdms.common.UserClaim;

import static org.junit.jupiter.api.Assertions.*;

class AuthWorkflowTest {
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    void wardRecorderCanLoginAndReceivesValidToken() {
        LoginResponse response = authService.login(new LoginRequest("recorder_flood_w1", "password123"));

        assertNotNull(response.token());
        assertEquals("recorder_flood_w1", response.username());
        assertEquals(Role.WARD_RECORDER, response.role());
        assertEquals(HazardType.FLOOD, response.hazard());
        assertEquals("Ward 1", response.ward());

        // Token can be validated
        UserClaim claim = authService.validateToken(response.token());
        assertEquals("recorder_flood_w1", claim.username());
        assertEquals(Role.WARD_RECORDER, claim.role());
        assertEquals(HazardType.FLOOD, claim.hazard());
        assertEquals("Ward 1", claim.ward());
    }

    @Test
    void supervisorCanLoginWithHazardClaim() {
        LoginResponse response = authService.login(new LoginRequest("supervisor_fire", "password123"));

        assertEquals(Role.PROVINCIAL_SUPERVISOR, response.role());
        assertEquals(HazardType.FIRE, response.hazard());
        assertNull(response.ward());
    }

    @Test
    void nationalViewerCanLogin() {
        LoginResponse response = authService.login(new LoginRequest("viewer_national", "viewer123"));

        assertEquals(Role.NATIONAL_VIEWER, response.role());
        assertNull(response.hazard());
    }

    @Test
    void invalidPasswordThrowsForbidden() {
        assertThrows(ForbiddenException.class, () -> authService.login(new LoginRequest("admin_provincial", "wrongpass")));
    }
}
