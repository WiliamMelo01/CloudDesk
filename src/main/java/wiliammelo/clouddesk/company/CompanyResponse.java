package wiliammelo.clouddesk.company;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String portalSlug,
        String portalPath,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getPortalSlug(),
                "/portal/" + company.getPortalSlug(),
                company.isActive(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
