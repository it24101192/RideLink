package com.ridelink.ridemanagement.security;

import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import com.ridelink.ridemanagement.error.DomainException;

public record AuthenticatedUser(String userId, String role) {
    public static AuthenticatedUser from(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new DomainException(org.springframework.http.HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid JWT is required");
        }
        String id = String.valueOf(jwt.getClaims().getOrDefault("userId", jwt.getSubject()));
        String role = jwt.getClaimAsString("role");
        if (role == null) {
            var roles = jwt.getClaimAsStringList("roles");
            role = roles == null || roles.isEmpty() ? "" : roles.getFirst();
        }
        if (id.isBlank()) throw new DomainException(org.springframework.http.HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "JWT must contain a userId claim");
        String normalized = role == null ? "" : role.replaceFirst("^ROLE_", "").toUpperCase(Locale.ROOT);
        if ("RIDER".equals(normalized)) normalized = "PASSENGER";
        return new AuthenticatedUser(id, normalized);
    }
    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isPassenger() { return "PASSENGER".equals(role); }
    public boolean isDriver() { return "DRIVER".equals(role); }
}
