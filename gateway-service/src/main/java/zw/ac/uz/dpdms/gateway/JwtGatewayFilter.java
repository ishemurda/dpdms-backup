package zw.ac.uz.dpdms.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Global filter that validates Bearer JWTs and injects X-Actor-Id / X-Role / X-Hazard / X-Ward headers.
 * Requests to /api/auth/** are always allowed through without a token.
 */
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public int getOrder() { return -1; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        // Allow auth endpoints and OPTIONS pass-through
        if (path.startsWith("/api/auth/") || exchange.getRequest().getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = auth.substring(7);
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("bad token");

            // Verify signature
            String signingInput = parts[0] + "." + parts[1];
            String expectedSig = hmacSha256(signingInput, secret);
            if (!expectedSig.equals(parts[2])) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Decode payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String sub = extractClaim(payloadJson, "sub");
            String role = extractClaim(payloadJson, "role");
            String hazard = extractClaim(payloadJson, "hazard");
            String ward = extractClaim(payloadJson, "ward");

            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.headers(h -> {
                        if (sub != null)    h.set("X-Actor-Id", sub);
                        if (role != null)   h.set("X-Role",     role);
                        if (hazard != null) h.set("X-Hazard",   hazard);
                        if (ward != null)   h.set("X-Ward",     ward);
                    }))
                    .build();
            return chain.filter(mutated);

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    /** Minimal JSON string-value extractor — avoids pulling in a JSON library into the reactive module. */
    private String extractClaim(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }
}
