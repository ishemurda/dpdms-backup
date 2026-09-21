package zw.ac.uz.dpdms.auth;

import org.springframework.stereotype.Service;
import zw.ac.uz.dpdms.common.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();

    public AuthService() {
        seedUsers();
    }

    private void seedUsers() {
        // Ward Recorders
        register(new UserAccount("recorder_flood_w1", "password123", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1", "Tariro Moyo"));
        register(new UserAccount("recorder_drought_w2", "password123", Role.WARD_RECORDER, HazardType.DROUGHT, "Ward 2", "Tendai Shumba"));
        register(new UserAccount("recorder_fire_w3", "password123", Role.WARD_RECORDER, HazardType.FIRE, "Ward 3", "Farai Ndlovu"));
        register(new UserAccount("recorder_zoonotic_w4", "password123", Role.WARD_RECORDER, HazardType.ZOONOTIC_DISEASE, "Ward 4", "Chipo Mutasa"));
        register(new UserAccount("recorder_mining_w5", "password123", Role.WARD_RECORDER, HazardType.MINING_ACCIDENT, "Ward 5", "Tatenda Chidziwa"));

        // Provincial Supervisors (Hazard Scoped)
        register(new UserAccount("supervisor_flood", "password123", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null, "Sarah Sithole"));
        register(new UserAccount("supervisor_drought", "password123", Role.PROVINCIAL_SUPERVISOR, HazardType.DROUGHT, null, "Blessing Gumbo"));
        register(new UserAccount("supervisor_fire", "password123", Role.PROVINCIAL_SUPERVISOR, HazardType.FIRE, null, "Kudakwashe Chiwenga"));
        register(new UserAccount("supervisor_zoonotic", "password123", Role.PROVINCIAL_SUPERVISOR, HazardType.ZOONOTIC_DISEASE, null, "Dr. Rutendo Hove"));
        register(new UserAccount("supervisor_mining", "password123", Role.PROVINCIAL_SUPERVISOR, HazardType.MINING_ACCIDENT, null, "Eng. Munyaradzi Zhou"));

        // Provincial Administrator
        register(new UserAccount("admin_provincial", "admin123", Role.PROVINCIAL_ADMIN, null, null, "Nyasha Mupfumi"));

        // National Viewer
        register(new UserAccount("viewer_national", "viewer123", Role.NATIONAL_VIEWER, null, null, "Simba Makoni"));
    }

    public void register(UserAccount account) {
        users.put(account.username().toLowerCase(), account);
    }

    public LoginResponse login(LoginRequest request) {
        if (request.username() == null || request.password() == null) {
            throw new IllegalArgumentException("Username and password are required");
        }
        UserAccount account = users.get(request.username().trim().toLowerCase());
        if (account == null || !account.password().equals(request.password())) {
            throw new ForbiddenException("Invalid username or password");
        }

        UserClaim claim = new UserClaim(account.username(), account.role(), account.hazard(), account.ward());
        long validitySeconds = 86400; // 24 hours
        String token = JwtUtils.generateToken(claim, JwtUtils.DEFAULT_SECRET, validitySeconds);

        return new LoginResponse(
            token,
            account.username(),
            account.role(),
            account.hazard(),
            account.ward(),
            account.fullName()
        );
    }

    public UserClaim validateToken(String token) {
        return JwtUtils.parseToken(token, JwtUtils.DEFAULT_SECRET);
    }

    public List<UserAccount> listUsers() {
        return List.copyOf(users.values());
    }
}
