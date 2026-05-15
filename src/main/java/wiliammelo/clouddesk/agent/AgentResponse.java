package wiliammelo.clouddesk.agent;

import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        List<AgentCompanyResponse> companies,
        Instant createdAt,
        Instant updatedAt
) {
    static AgentResponse from(User user, List<AgentCompanyResponse> companies) {
        return new AgentResponse(
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
