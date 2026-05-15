package wiliammelo.clouddesk.company;

import java.time.Instant;
import java.util.UUID;

public record CompanyAgentInvitationResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String companyPortalSlug,
        UUID agentId,
        String agentEmail,
        UUID invitedByOwnerId,
        CompanyAgentInvitationStatus status,
        Instant createdAt,
        Instant respondedAt
) {
    static CompanyAgentInvitationResponse from(CompanyAgentInvitation invitation) {
        return new CompanyAgentInvitationResponse(
                invitation.getId(),
                invitation.getCompany().getId(),
                invitation.getCompany().getName(),
                invitation.getCompany().getPortalSlug(),
                invitation.getAgent().getId(),
                invitation.getAgent().getEmail(),
                invitation.getInvitedByOwner().getId(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getRespondedAt()
        );
    }
}
