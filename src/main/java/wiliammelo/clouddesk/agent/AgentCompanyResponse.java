package wiliammelo.clouddesk.agent;

import wiliammelo.clouddesk.company.Company;

import java.util.UUID;

public record AgentCompanyResponse(
        UUID id,
        String name,
        String portalSlug,
        String portalPath,
        String logoUrl
) {
    static AgentCompanyResponse from(Company company) {
        return new AgentCompanyResponse(
                company.getId(),
                company.getName(),
                company.getPortalSlug(),
                "/portal/" + company.getPortalSlug(),
                company.getLogoUrl()
        );
    }
}
