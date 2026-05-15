package wiliammelo.clouddesk.agent;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.security.JwtPrincipal;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentControllerTest {

    private final AgentService agentService = mock(AgentService.class);
    private final AgentController agentController = new AgentController(agentService);
    private final JwtPrincipal principal = new JwtPrincipal(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "agent@cloud.test",
            UserRole.AGENT,
            UUID.fromString("22222222-2222-2222-2222-222222222222")
    );

    @Test
    void createsAgent() {
        AgentCreateRequest request = new AgentCreateRequest("Agent", "agent@cloud.test", "password123");
        AgentResponse response = response();
        when(agentService.create(request)).thenReturn(response);

        assertThat(agentController.create(request)).isEqualTo(response);
    }

    @Test
    void getsCurrentAgent() {
        AgentResponse response = response();
        when(agentService.get(principal.userId())).thenReturn(response);

        assertThat(agentController.get(principal)).isEqualTo(response);
    }

    @Test
    void updatesCurrentAgent() {
        AgentUpdateRequest request = new AgentUpdateRequest("Updated", "updated@cloud.test", null);
        AgentResponse response = response();
        when(agentService.update(principal.userId(), request)).thenReturn(response);

        assertThat(agentController.update(principal, request)).isEqualTo(response);
    }

    @Test
    void deletesCurrentAgent() {
        agentController.delete(principal);

        verify(agentService).delete(principal.userId());
    }

    private AgentResponse response() {
        Instant now = Instant.parse("2026-05-14T18:00:00Z");
        return new AgentResponse(UUID.randomUUID(), "Agent", "agent@cloud.test", UserRole.AGENT, true, now, now);
    }
}
