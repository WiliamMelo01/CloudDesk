package wiliammelo.clouddesk.auth;

import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UUID userId,
        String email,
        UserRole role
) {
}
