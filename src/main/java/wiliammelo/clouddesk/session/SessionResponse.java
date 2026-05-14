package wiliammelo.clouddesk.session;

import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID sessionId,
        UserRole role,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt,
        String ipAddress,
        String userAgent,
        boolean current
) {
    static SessionResponse from(SessionRecord session, UUID currentSessionId) {
        return new SessionResponse(
                session.sessionId(),
                session.role(),
                session.createdAt(),
                session.expiresAt(),
                session.lastUsedAt(),
                session.ipAddress(),
                session.userAgent(),
                session.sessionId().equals(currentSessionId)
        );
    }
}
