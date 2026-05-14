package wiliammelo.clouddesk.admin;

import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record AdminResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    static AdminResponse from(User user) {
        return new AdminResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
