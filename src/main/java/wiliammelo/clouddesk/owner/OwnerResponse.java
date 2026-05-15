package wiliammelo.clouddesk.owner;

import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OwnerResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        List<OwnerCompanyResponse> companies,
        Instant createdAt,
        Instant updatedAt
) {
    static OwnerResponse from(User user, List<OwnerCompanyResponse> companies) {
        return new OwnerResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                companies,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
