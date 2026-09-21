package zw.ac.uz.dpdms.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

public final class JwtUtils {
    public static final String DEFAULT_SECRET = "dpdms-rushinga-super-secure-jwt-signing-secret-key-2026";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtUtils() {}

    public static String generateToken(UserClaim claim, String secret, long validitySeconds) {
        try {
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("sub", claim.username());
            payload.put("role", claim.role().name());
            if (claim.hazard() != null) {
                payload.put("hazard", claim.hazard().name());
            }
            if (claim.ward() != null) {
                payload.put("ward", claim.ward());
            }
            long now = Instant.now().getEpochSecond();
            payload.put("iat", now);
            payload.put("exp", now + validitySeconds);

            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(MAPPER.writeValueAsBytes(payload));
            String data = encodedHeader + "." + encodedPayload;
            String signature = sign(data, secret);

            return data + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT", e);
        }
    }

    public static UserClaim parseToken(String token, String secret) {
        if (token == null || token.isBlank()) {
            throw new ForbiddenException("Missing authentication token");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ForbiddenException("Malformed token format");
        }

        String data = parts[0] + "." + parts[1];
        String expectedSignature = sign(data, secret);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("Invalid token signature");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = MAPPER.readTree(payloadBytes);

            long exp = node.path("exp").asLong(0);
            if (exp > 0 && Instant.now().getEpochSecond() > exp) {
                throw new ForbiddenException("Token has expired");
            }

            String sub = node.path("sub").asText();
            String roleStr = node.path("role").asText();
            Role role = Role.valueOf(roleStr);

            HazardType hazard = null;
            if (node.hasNonNull("hazard")) {
                hazard = HazardType.valueOf(node.path("hazard").asText());
            }

            String ward = null;
            if (node.hasNonNull("ward")) {
                ward = node.path("ward").asText();
            }

            return new UserClaim(sub, role, hazard, ward);
        } catch (ForbiddenException fe) {
            throw fe;
        } catch (Exception e) {
            throw new ForbiddenException("Failed to decode token payload: " + e.getMessage());
        }
    }

    private static String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }
}
