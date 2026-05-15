package wiliammelo.clouddesk.company;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyAgentInvitationCreateRequest(
        @NotBlank
        @Email
        @Size(max = 180)
        String agentEmail
) {
}
