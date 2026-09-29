package com.ridelink.farepayment.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Locale;
import com.ridelink.farepayment.error.UnauthorizedError;

public record AuthenticatedUser(String userId, String role) {
    public static AuthenticatedUser from(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthorizedError("A valid JWT is required");
        }
        Object rawId = jwt.getClaims().getOrDefault("userId", jwt.getSubject());
        try {
            String id = String.valueOf(rawId);
            if (id.isBlank()) throw new IllegalArgumentException("userId missing");
            String role = jwt.getClaimAsString("role");
            if (role == null) {
                var roles = jwt.getClaimAsStringList("roles");
                role = roles == null || roles.isEmpty() ? "" : roles.getFirst();
            }
            return new AuthenticatedUser(id, normalizeRole(role));
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedError("JWT must contain a userId claim");
        }
    }

    private static String normalizeRole(String role) {
        if (role == null) return "";
        String normalized = role.startsWith("ROLE_") ? role.substring(5) : role;
        normalized = normalized.toUpperCase(Locale.ROOT);
        return "RIDER".equals(normalized) ? "PASSENGER" : normalized;
    }

    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isPassenger() { return "PASSENGER".equals(role); }
    public boolean isDriver() { return "DRIVER".equals(role); }
}
