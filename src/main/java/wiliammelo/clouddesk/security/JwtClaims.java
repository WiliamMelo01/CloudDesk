package wiliammelo.clouddesk.security;

import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record JwtClaims(
        UUID userId,
        String email,
        UserRole role,
        TokenType tokenType,
        Instant expiresAt
) {
}
