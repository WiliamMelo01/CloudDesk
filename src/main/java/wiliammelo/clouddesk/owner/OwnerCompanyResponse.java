package wiliammelo.clouddesk.owner;

import wiliammelo.clouddesk.company.Company;

import java.util.UUID;

public record OwnerCompanyResponse(
        UUID id,
        String name,
        String portalSlug,
        String portalPath,
        String logoUrl
) {
    static OwnerCompanyResponse from(Company company) {
        return new OwnerCompanyResponse(
                company.getId(),
                company.getName(),
                company.getPortalSlug(),
                "/portal/" + company.getPortalSlug(),
                company.getLogoUrl()
        );
    }
}
