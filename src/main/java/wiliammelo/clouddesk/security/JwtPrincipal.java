package wiliammelo.clouddesk.security;

import wiliammelo.clouddesk.user.UserRole;

import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        String email,
        UserRole role,
        UUID sessionId
) {
}
