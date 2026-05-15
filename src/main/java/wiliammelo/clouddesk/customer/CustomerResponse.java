package wiliammelo.clouddesk.customer;

import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static CustomerResponse from(User customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getRole(),
                customer.isActive(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
