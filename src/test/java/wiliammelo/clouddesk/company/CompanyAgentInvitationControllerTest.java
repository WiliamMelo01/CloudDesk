package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.security.JwtPrincipal;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyAgentInvitationControllerTest {

    private final CompanyAgentInvitationService invitationService = mock(CompanyAgentInvitationService.class);
    private final CompanyAgentInvitationController controller = new CompanyAgentInvitationController(invitationService);
    private final JwtPrincipal ownerPrincipal = new JwtPrincipal(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "owner@cloud.test",
            UserRole.OWNER,
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    );
    private final JwtPrincipal agentPrincipal = new JwtPrincipal(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            "agent@cloud.test",
            UserRole.AGENT,
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    );

    @Test
    void createsInvitation() {
        UUID companyId = UUID.randomUUID();
        CompanyAgentInvitationCreateRequest request = new CompanyAgentInvitationCreateRequest("agent@cloud.test");
        CompanyAgentInvitationResponse response = response();
        when(invitationService.create(ownerPrincipal.userId(), companyId, request)).thenReturn(response);

        assertThat(controller.create(ownerPrincipal, companyId, request)).isEqualTo(response);
    }

    @Test
    void listsPendingInvitations() {
        List<CompanyAgentInvitationResponse> response = List.of(response());
        when(invitationService.listPendingForAgent(agentPrincipal.userId())).thenReturn(response);

        assertThat(controller.listPending(agentPrincipal)).isEqualTo(response);
    }

    @Test
    void acceptsInvitation() {
        UUID invitationId = UUID.randomUUID();
        CompanyAgentInvitationResponse response = response();
        when(invitationService.accept(agentPrincipal.userId(), invitationId)).thenReturn(response);

        assertThat(controller.accept(agentPrincipal, invitationId)).isEqualTo(response);
    }

    @Test
    void rejectsInvitation() {
        UUID invitationId = UUID.randomUUID();
        CompanyAgentInvitationResponse response = response();
        when(invitationService.reject(agentPrincipal.userId(), invitationId)).thenReturn(response);

        assertThat(controller.reject(agentPrincipal, invitationId)).isEqualTo(response);
    }

    private CompanyAgentInvitationResponse response() {
        return new CompanyAgentInvitationResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ByteCare",
                "bytecare",
                UUID.randomUUID(),
                "agent@cloud.test",
                UUID.randomUUID(),
                CompanyAgentInvitationStatus.PENDING,
                Instant.parse("2026-05-14T18:00:00Z"),
                null
        );
    }
}
