package wiliammelo.clouddesk.session;

import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record SessionRecord(
        UUID sessionId,
        UUID userId,
        String email,
        UserRole role,
        String refreshTokenHash,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt,
        String ipAddress,
        String userAgent
) {
    SessionRecord markUsed(Instant usedAt) {
        return new SessionRecord(
                sessionId,
                userId,
                email,
                role,
                refreshTokenHash,
                createdAt,
                expiresAt,
                usedAt,
                ipAddress,
                userAgent
        );
    }
}
